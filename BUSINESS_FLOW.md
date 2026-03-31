## Cinema Booking – End‑to‑End Business Flow

This document explains the core business flow from seat selection to ticket usage, based on the current backend implementation.

---

## 1. Seat Selection & Booking Creation

- **Entry points**
  - `POST /api/bookings` → `BookingServiceImpl.createBooking`
  - `POST /api/bookings/full` → `BookingServiceImpl.createBookingWithSeats` (**recommended – atomic flow**)

- **Core behavior (`createBookingWithSeats`)**
  - Validates:
    - `userId`, `showtimeId`, `totalPrice` are present and > 0.
    - `seats[]` is non‑empty, each seat has `seatId` and positive `price`.
    - No duplicate `seatId` in the same request.
  - Loads `Users`, `Showtimes`, and per‑seat `Seats`, ensuring each seat belongs to the showtime’s screening room.
  - **Concurrency control**
    - Global lock: `booking:full:create:<userId>:<showtimeId>`.
    - Per‑seat locks (sorted by `seatId`): `seat:showtime:lock:<showtimeId>:<seatId>`.
  - **Global seat conflict check**
    - For each requested seat:
      - If `ShowTimeSeats.status == BOOKED` → reject.
      - If `ShowTimeSeats.status == HELD` **and** `hold_expires_at > now` → reject.
  - **On success (single DB transaction)**
    - Creates `Bookings` with:
      - `booking_status = PENDING`
      - `expired_at = now + 10 minutes`
    - Creates `BookingSeats` for each selected seat.
    - For each seat:
      - Creates / updates `ShowTimeSeats`:
        - `status = HELD`
        - `hold_expires_at = booking.expired_at`
        - `hold_token = <booking_id>`

**Result:** seats are **held** for 10 minutes (or until payment completes), and the booking is `PENDING`.

### 1.1 Incremental Seat Locking (Per‑Click Flow)

As an alternative to the atomic `/api/bookings/full` flow, the system also supports **locking seats one‑by‑one** as the user clicks them in the UI.

- **Entry point**
  - `POST /api/booking-seats` → `BookingSeatServiceImpl.createBookingSeat`

- **Intended usage**
  - The user already has a `Bookings` record (e.g. created via `POST /api/bookings`).
  - Frontend logic:
    - On seat click (select 1A, 1B, ...):
      - Call `POST /api/booking-seats` with `bookingId`, `seatId`, `price`.
    - On seat unselect:
      - Optionally call `DELETE /api/booking-seats/{bookingSeatId}` to remove and later free the seat (you can extend the delete logic to also revert `ShowTimeSeats` if desired).

- **Core behavior (`createBookingSeat`)**
  - Loads `Bookings` and verifies:
    - Requested seat belongs to the same screening room as the booking’s showtime.
  - **Concurrency control**
    - Lock key: `bookingSeat:create:<showtimeId>:<seatId>` ensures only one writer per seat+showtime at a time.
  - **Global seat conflict check**
    - Uses `BookingSeatRepository.existsActiveSeatByShowtimeAndSeatId(showtimeId, seatId)` to see if any *active* booking has already taken this seat.
    - Also checks `ShowTimeSeats`:
      - `status == BOOKED` → reject.
      - `status == HELD` with `hold_expires_at > now` → reject.
  - **On success**
    - Creates a `BookingSeats` row for `(booking, seat)`.
    - Creates/updates `ShowTimeSeats` with:
      - `status = HELD`
      - `hold_expires_at = booking.expired_at`
      - `hold_token = <booking_id>`.

**Result:** if your frontend calls `POST /api/booking-seats` on each click, seats like 1A and 1B are **locked immediately at click time**, not only at the final “Book” action.

---

## 2. Payment Creation

- **Entry point**
  - `POST /api/payments` → `PaymentServiceImpl.createPayment`

- **Core behavior**
  - Validates:
    - `bookingId` is present.
    - `amount > 0`.
    - `ref` (gateway reference / order code) is non‑blank and unique.
  - Loads `Bookings` by `bookingId`.
  - Rejects if booking status is any of:
    - `CANCELLED`, `EXPIRED`, `CONFIRMED`, `PAID`.
  - Enforces **amount integrity**:
    - `amount == booking.total_price`.
  - Enforces **single successful payment per booking**:
    - If `existsByBookingIdAndStatus(bookingId, SUCCESS)` → reject.
  - On success:
    - Creates `Payments`:
      - `status = PENDING`
      - `amount`, `ref`, `booking` set.

**Result:** a `PENDING` payment exists for a `PENDING` booking; seats remain `HELD`.

---

## 3. Payment Completion (Gateway / PayOS)

> You will handle all PayOS‑specific logic yourself. Below is how it integrates with internal services.

- **External event**
  - Payment gateway (PayOS) sends a webhook to `POST /api/webhook/payment`.
  - `WebhookController.handlePaymentWebhook` delegates to `PayOSService.verifyPayment(webhook)`.

- **Expected internal behavior (high level)**
  1. Verify the webhook (signature, amount, currency, merchant, status).
  2. Map webhook data (e.g. `orderCode` / reference) to your `Payments.ref` (or `Payments.payment_id`).
  3. In a transaction:
     - Mark the `Payments` row as `SUCCESS` **once**.
       - Use `PaymentService.updatePayment(paymentId, PaymentRequestDto{ status = SUCCESS })`.
       - This method:
         - Prevents updates once `status == SUCCESS`.
         - When status becomes `SUCCESS`, it:
           - Sets `booking.booking_status = PAID`
           - Sets `booking.confirmed_at` and `booking.updated_at` to `now`.
     - Trigger ticket generation:
       - Call `TicketService.generateTicketsAfterSuccessfulPayment(paymentId)`.

**Result:** booking is `PAID`, payment is `SUCCESS`, and tickets can be generated exactly once for each seat.

---

## 4. Ticket Generation

- **Entry point**
  - Service‑side call (typically from webhook / payment success handler):
    - `TicketServiceImpl.generateTicketsAfterSuccessfulPayment(Long paymentId)`

- **Core behavior**
  - Validates:
    - `paymentId` is not null.
  - Lock: `ticket:generate:payment:<paymentId>` (ensures idempotency per payment).
  - Loads `Payments` and checks:
    - `status == PaymentStatusEnum.SUCCESS`, otherwise rejects.
  - Loads `Bookings` from the payment and all `BookingSeats` for the booking.
    - If no `BookingSeats` → rejects.
  - For each `BookingSeat`:
    - If `TicketRepository.existsByBookingIdAndSeatId(bookingId, seatId)` → **skip** (already has ticket).
    - Else create `Tickets`:
      - `booking`, `seat`
      - `ticket_code` = unique code (time + random suffix)
      - `issued_at = now`
      - `valid_until = showtime.end_time + 30 minutes`
      - `status = VALID`
  - If no new tickets created (all seats already had tickets) → rejects with a clear message.

**Result:** each paid and seated booking has exactly one **valid** ticket per seat; duplicate calls are safe.

---

## 5. Ticket Validation (At Cinema)

- **Entry point**
  - `POST /api/tickets/validate` → `TicketServiceImpl.validateTicket`

- **Core behavior**
  - Validates request and ticket code.
  - Lock: `ticket:validate:<ticketCode>`.
  - Loads `Tickets` by `ticket_code`.
  - Evaluates status:
    - If `status == USED` → return `success = false`, message `"Ticket already used"`.
    - If `status == EXPIRED` **or** `valid_until < now`:
      - If not already `EXPIRED`, update status to `EXPIRED`.
      - Return `success = false`, message `"Ticket expired"`.
    - Otherwise:
      - Update:
        - `status = USED`
        - `used_at = now`
      - Return `success = true`, message `"Ticket is valid and has been marked as used"`.

**Result:** tickets can be safely scanned exactly once; reused or expired tickets are rejected with clear reasons.

---

## 6. Booking Expiry & Seat Release

- **Entry point**
  - Scheduled job: `BookingExpiryJob.expirePendingBookings()`
  - Runs every **30 seconds**.

- **Core behavior**
  - Finds all `Bookings` with:
    - `status = PENDING`
    - `expired_at < now`.
  - For each expired booking:
    - Set:
      - `booking_status = EXPIRED`
      - `updated_at = now`
    - Save booking.
    - Load all `BookingSeats` for that booking.
    - For each seat:
      - Acquire lock: `seat:showtime:lock:<showtimeId>:<seatId>`.
      - Load `ShowTimeSeats` for `(showtimeId, seatId)`.
      - If:
        - `status == HELD`
        - `hold_token == <booking_id>` (string compare)
        - Then:
          - Set `status = AVAILABLE`
          - Clear `hold_token` and `hold_expires_at`.
          - Save `ShowTimeSeats`.

**Result:** if a user never completes payment, their booking becomes `EXPIRED` and held seats are safely returned to `AVAILABLE` for others.

---

## 7. Key Invariants & Idempotency Rules

- **Booking**
  - Initially `PENDING` after booking (with or without seats).
  - Moves to `EXPIRED` automatically if not paid before `expired_at`.
  - Moves to `PAID` when a related payment becomes `SUCCESS`.
  - `CONFIRMED` is available for future use if you want an extra state (e.g., after ticket generation or manual staff confirmation).

- **Payment**
  - Only one `SUCCESS` payment per booking is allowed.
  - Once `SUCCESS`, it cannot be updated or deleted.

- **Seats (`ShowTimeSeats`)**
  - `AVAILABLE` → can be selected.
  - `HELD` → reserved for a specific booking until `hold_expires_at`, keyed by `hold_token = booking_id`.
  - `BOOKED` → recommended for you to use after successful payment (you can extend logic to set this when payment succeeds).

- **Tickets**
  - Generated only when payment is `SUCCESS`.
  - Exactly one ticket per `(bookingId, seatId)`.
  - Status lifecycle: `VALID` → `USED` or `EXPIRED`.

---

## 8. Where to Extend Next

- **PayOS integration**
  - Implement detailed verification and mapping from webhooks to `Payments` and `Bookings`.
  - Call `PaymentService.updatePayment(...SUCCESS...)` and then `TicketService.generateTicketsAfterSuccessfulPayment(...)`.

- **Reconciliation**
  - Periodic job to query PayOS for definitive status of `PENDING` payments and reconcile with local DB.

- **Seat “BOOKED” update**
  - After payment success, update `ShowTimeSeats` from `HELD` → `BOOKED` for that booking’s seats.

