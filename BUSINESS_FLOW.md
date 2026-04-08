## Cinema Booking – End-to-End Business Flow

This document reflects the **current implementation** after booking-code validation was introduced.

Run one-time local business-flow smoke test:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.test.run-business-flow-once=true"
```

---

## 1) Booking and Seat Hold

### Option A: Create booking first, then lock seats incrementally

1. `POST /api/bookings` creates a booking (`PENDING`, with `expired_at` = now + ~10 minutes).
2. `POST /api/booking-seats` adds seats one-by-one:
   - validates seat belongs to booking showtime room
   - rejects seat if already held/booked by another active booking
   - creates `BookingSeats` row (`LOCKED`)
   - updates/creates `ShowTimeSeats` to `HELD` with `hold_token = booking_id`
   - updates booking total price

### Option B: Create booking with seats atomically

`POST /api/bookings/full` performs booking + seat hold in one transaction:

- validates seat list (non-empty, unique seat IDs, positive price)
- acquires deterministic seat locks for concurrency safety
- creates booking + all `BookingSeats`
- sets related `ShowTimeSeats` to `HELD`

---

## 2) Payment

Use `POST /api/payments` with `bookingId`.

`PaymentService.createPayment(...)`:

- rejects inactive booking statuses (`CANCELLED`, `EXPIRED`, `CONFIRMED`, `PAID`)
- rejects if booking already has a successful payment
- creates a `PENDING` payment
- returns PayOS checkout URL (from `PayOSService`)

On payment success:

- `PaymentService.markPaymentSuccess(...)` marks payment `SUCCESS`
- booking status moves to `PAID`
- seat holds are finalized to `BOOKED`
- `PaymentSucceededEvent` is published

---

## 3) Ticket Generation

Triggered by payment success (event/webhook/manual endpoint):

- `TicketService.generateTicketsAfterSuccessfulPayment(paymentId)`
- one ticket per `(booking, seat)` if not already exists
- ticket defaults:
  - `status = VALID`
  - `issued_at = now`
  - `valid_until = showtime.end_time + 30 minutes`

Also sends booking confirmation email (after commit) using:

- template: `mail/booking-confirmation`
- QR payload: **booking_code** (not per-seat ticket code)

---

## 4) Validation at Cinema

### Legacy single-seat scan

`POST /api/tickets/validate`

- validates one `ticketCode`
- marks only that ticket `USED`

### New business flow: one scan for all seats in booking

`POST /api/tickets/validate-booking`

- input: `bookingCode`
- finds booking by `booking_code`
- loads all tickets in booking
- in one transaction/lock:
  - marks all valid tickets to `USED` with same timestamp
  - returns all tickets in response

This supports: **1 QR scan -> redeem all seats in same booking**.

---

## 5) Booking Code

`Bookings.booking_code` exists and is unique.

- auto-generated on persist if empty:
  - format: `BOOKING-<booking_id>`

This is now the recommended code for entry scanning and email QR.

---

## 6) Expiry Job

`BookingExpiryJob` runs periodically and expires stale `PENDING` bookings:

- booking status -> `EXPIRED`
- corresponding `ShowTimeSeats` held by that booking -> `AVAILABLE`

---

## 7) Key Invariants

- one active ticket row per seat per booking (`booking_id`, `seat_id`)
- booking-level scan is done through `booking_code`
- payment success is idempotent/locked
- ticket generation is idempotent per payment

