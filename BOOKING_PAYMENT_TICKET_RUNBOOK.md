## Booking-Payment-Ticket Runbook

### State model

- `BookingStatusEnum`
  - `PENDING` -> user selected seats, payment not confirmed yet.
  - `PAID` -> payment success was applied.
  - `CONFIRMED` -> async ticket generation flow completed.
  - `EXPIRED` / `CANCELLED` -> inactive booking.

- `PaymentStatusEnum`
  - `PENDING`, `SUCCESS`, `FAILED`.

- `BookingSeatStatusEnum`
  - `LOCKED` -> seat held for pending booking.
  - `CONFIRMED` -> paid and finalized.
  - `RELEASED` -> hold released by expiry cleanup.

### Main flow

1. `POST /api/bookings/full` creates booking and seat holds in one transaction.
2. In the same request, backend creates `Payments` as `PENDING`, calls PayOS to generate a `checkoutUrl`, and returns it to the client.
3. Payment success is applied when either:
   - PayOS sends `POST /api/webhook/payment` → `PayOSService.verifyPayment(webhook)` updates the payment to `SUCCESS`, marks booking `PAID`, converts `ShowTimeSeats` to `BOOKED`, and emits `PaymentSucceededEvent`.
   - (Optional/manual/test) `POST /payments/{paymentId}/mark-success` or `POST /payments/ref/{ref}/mark-success`.
4. Async listener consumes `PaymentSucceededEvent`:
   - Generates tickets idempotently.
   - Sets `BookingSeats` to `CONFIRMED`.
   - Moves booking `PAID -> CONFIRMED`.

### Recovery and reconciliation

- `BookingExpiryJob` runs every 30s:
  - Expires stale `PENDING` bookings.
  - Marks booking seats `RELEASED`.
  - Releases `ShowTimeSeats` back to `AVAILABLE`.

- `PaymentReconciliationJob` runs every 60s:
  - Scans old `PENDING` payments.
  - If booking already `PAID/CONFIRMED`, replays success flow idempotently.

### Concurrency and idempotency

- In-memory locks:
  - `booking:create:{userId}:{showtimeId}` / `booking:full:create:{userId}:{showtimeId}`
  - `seat:showtime:lock:{showtimeId}:{seatId}`
  - `payment:create:{bookingId}`
  - `payment:success:{paymentId}`
  - `ticket:generate:payment:{paymentId}`
- DB row locking for critical payment transitions:
  - `PaymentRepository.findByIdForUpdate`
  - `PaymentRepository.findByRefForUpdate`
  - `BookingRepository.findByIdForUpdate`
- DB uniqueness:
  - `Payments.ref` is unique.
  - `ShowTimeSeats(showtime_id, seat_id)` is unique.

### Operational checks

- Verify booking progression:
  - `PENDING -> PAID -> CONFIRMED`
- Verify seat lifecycle:
  - `LOCKED -> CONFIRMED` on payment success.
  - `LOCKED -> RELEASED` on expiry.
- Verify ticket idempotency:
  - repeated ticket generation calls do not duplicate tickets.

