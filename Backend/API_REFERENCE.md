# CinemaBooking API Reference

Base URL (local): `http://localhost:1325`

This file documents current controller endpoints with request payloads and response examples.

## Authentication

### POST `/api/auth/register`

Request:
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "full_name": "Nguyen Van A"
}
```

Response `200`:
```json
"User registered successfully"
```

### POST `/api/auth/login`

Request:
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Response `200`:
```json
"<jwt-token>"
```

### POST `/api/auth/logout`

Headers:
- `Authorization: Bearer <jwt-token>`

Response `200`:
```json
"Logged out successfully"
```

---

## Cinemas

### POST `/api/cinemas`

Request:
```json
{
  "name": "Cinema ABC",
  "address": "123 Street, District 1"
}
```

Response `201`:
```json
{
  "cinemas_id": 212345678,
  "name": "Cinema ABC",
  "address": "123 Street, District 1",
  "created_at": "2026-04-08T03:00:00.000+07:00"
}
```

### GET `/api/cinemas`
Response `200`: array of `CinemaDto`.

### GET `/api/cinemas/{id}`
Response `200`: `CinemaDto`.

### PUT `/api/cinemas/{id}`

Request:
```json
{
  "name": "Cinema ABC Updated",
  "address": "456 Street, District 1"
}
```

Response `200`: updated `CinemaDto`.

### DELETE `/api/cinemas/{id}`
Response `204` (no content).

---

## Screening Rooms

### POST `/api/screeningrooms`

Request:
```json
{
  "room_name": "Room A",
  "amount_rows": 10,
  "amount_cols": 15,
  "cinema_id": 212345678
}
```

Response `201`:
```json
{
  "room_id": 712345678,
  "room_name": "Room A",
  "amount_rows": 10,
  "amount_cols": 15,
  "cinema_id": 212345678
}
```

### GET `/api/screeningrooms`
Response `200`: array of `ScreeningRoomDto`.

### GET `/api/screeningrooms/{id}`
Response `200`: `ScreeningRoomDto`.

### PUT `/api/screeningrooms/{id}`
Request body same as create (partial fields accepted by service logic).

### DELETE `/api/screeningrooms/{id}`
Response `200`:
```json
"Screening room deleted successfully with id: 712345678"
```

---

## Movies

### POST `/api/movies`

Request:
```json
{
  "age_rating": "PG13",
  "release_date": "2026-04-01T00:00:00.000+07:00",
  "title": "Avengers",
  "status": "NOW_SHOWING",
  "description": "Action movie",
  "genre": "ACTION",
  "duration_minutes": 120
}
```

Response `201`: `MovieDto`.

### GET `/api/movies`
Response `200`: array of `MovieDto`.

### GET `/api/movies/{id}`
Response `200`: `MovieDto`.

### PUT `/api/movies/{id}`
Request body follows `MovieDto` fields.

### DELETE `/api/movies/{id}`
Response `200`:
```json
"Movie deleted successfully with id: 512345678"
```

---

## Showtimes

### POST `/api/showtimes`

Request:
```json
{
  "status": "SCHEDULED",
  "start_time": "2026-04-09T18:30:00.000+07:00",
  "end_time": "2026-04-09T20:30:00.000+07:00",
  "buffer_time": 15,
  "movie_id": 512345678,
  "screening_room_id": 712345678,
  "seat_price": 100000
}
```

Response `201`: `ShowtimeDto`.

### GET `/api/showtimes`
Response `200`: array of `ShowtimeDto`.

### GET `/api/showtimes/{id}`
Response `200`: `ShowtimeDto`.

### PUT `/api/showtimes/{id}`
Request body follows `ShowtimeDto`.

### DELETE `/api/showtimes/{id}`
Response `200`:
```json
"Showtime deleted successfully with id: 912345678"
```

---

## Seats

### GET `/api/seats`
Response `200`: array of `SeatDto`.

### GET `/api/seats/{id}`
Response `200`: `SeatDto`.

### GET `/api/seats/room/{roomId}`
Response `200`: array of `SeatDto`.

### PUT `/api/seats/{id}`

Request:
```json
{
  "seat_row": 1,
  "seat_col": 1,
  "seat_label": "A",
  "is_active": true,
  "seat_price": 100000,
  "screeningRoomId": 712345678
}
```

Response `200`: updated `SeatDto`.

### DELETE `/api/seats/{id}`
Response `204`.

---

## Bookings

### POST `/api/bookings`

Request:
```json
{
  "userId": 112345678,
  "showtimeId": 912345678
}
```

Response `201`:
```json
{
  "bookingId": 312345678,
  "bookingStatus": "PENDING",
  "totalPrice": 0,
  "userId": 112345678,
  "showtimeId": 912345678
}
```

### POST `/api/bookings/full`

Request:
```json
{
  "userId": 112345678,
  "showtimeId": 912345678,
  "totalPrice": 200000,
  "seats": [
    { "seatId": 812345678, "price": 100000 },
    { "seatId": 812345679, "price": 100000 }
  ]
}
```

Response `201`: `BookingDto`.

### GET `/api/bookings`
Response `200`: array of `BookingDto`.

### GET `/api/bookings/{bookingId}`
Response `200`: `BookingDto`.

### PUT `/api/bookings/{bookingId}`
Request body:
```json
{
  "userId": 112345678,
  "showtimeId": 912345678
}
```

### DELETE `/api/bookings/{bookingId}`
Response `204`.

---

## Booking Seats

### POST `/api/booking-seats`

Request:
```json
{
  "bookingId": 312345678,
  "seatId": 812345678,
  "price": 100000
}
```

Response `201`:
```json
{
  "bookingSeatId": 412345678,
  "bookingId": 312345678,
  "seatId": 812345678,
  "price": 100000,
  "status": "LOCKED"
}
```

### GET `/api/booking-seats`
Response `200`: array of `BookingSeatDto`.

### GET `/api/booking-seats/{bookingSeatId}`
Response `200`: `BookingSeatDto`.

### PUT `/api/booking-seats/{bookingSeatId}`
Request same as create.

### DELETE `/api/booking-seats/{bookingSeatId}`
Response `204`.

---

## Payments

### POST `/api/payments`

Request:
```json
{
  "bookingId": 312345678
}
```

Response `201`:
```json
"https://pay.payos.vn/web/...."
```

### GET `/api/payments`
Response `200`: array of `PaymentDto`.

### GET `/api/payments/{paymentId}`
Response `200`: `PaymentDto`.

### PUT `/api/payments/{paymentId}`

Request:
```json
{
  "amount": 200000,
  "status": "PENDING"
}
```

Response `200`: updated `PaymentDto`.

### POST `/api/payments/{paymentId}/mark-success`
Response `200`: `PaymentDto` with `status = SUCCESS`.

### POST `/api/payments/ref/{ref}/mark-success`
Response `200`: `PaymentDto` with `status = SUCCESS`.

### DELETE `/api/payments/{paymentId}`
Response `200`:
```json
"Payment deleted successfully"
```

---

## Tickets

### GET `/api/tickets`
Response `200`: array of `TicketDto`.

### GET `/api/tickets/{ticketId}`
Response `200`: `TicketDto`.

### GET `/api/tickets/code/{ticketCode}`
Response `200`: `TicketDto`.

### POST `/api/tickets/generate/payment/{paymentId}`
Response `200`: array of generated `TicketDto`.

### POST `/api/tickets/validate`

Request:
```json
{
  "ticketCode": "TICKET-1775586921560-26452"
}
```

Response `200`:
```json
{
  "success": true,
  "message": "Ticket is valid and has been marked as used",
  "ticket": {
    "ticketId": 1175631442,
    "ticketCode": "TICKET-1775586921560-26452",
    "status": "USED"
  }
}
```

### POST `/api/tickets/validate-booking`

Request:
```json
{
  "bookingCode": "BOOKING-385695425"
}
```

Response `200`:
```json
{
  "success": true,
  "message": "Booking is valid and all tickets have been marked as used",
  "bookingCode": "BOOKING-385695425",
  "tickets": [
    {
      "ticketId": 1165558674,
      "ticketCode": "TICKET-1775586921577-67159",
      "status": "USED",
      "seatNumber": "A2"
    },
    {
      "ticketId": 1175631442,
      "ticketCode": "TICKET-1775586921560-26452",
      "status": "USED",
      "seatNumber": "A1"
    }
  ]
}
```

---

## Webhook

### POST `/api/webhook/payment`

Request body: PayOS webhook JSON payload.

Behavior:
- verifies webhook through `PayOSService`
- marks payment success
- triggers ticket generation

Response:
- `200` empty body if accepted
- `400 "Invalid webhook"` if verification fails
