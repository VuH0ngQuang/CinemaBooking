# API Test URLs and Payloads

Quick copy/paste guide for testing APIs described in:
- `BUSINESS_FLOW.md`
- `MOVIE_ROOM_SHOWTIME_FLOW.md`

Base URL (local):
- `http://localhost:1325`

---

## 1) Movie -> Room -> Showtime

### 1.1 Create Movie
- **URL:** `POST http://localhost:1325/api/movies`
- **Payload:**
```json
{
  "title": "Avengers: Endgame",
  "description": "Superhero movie",
  "genre": "Action",
  "duration_minutes": 181,
  "age_rating": "PG-13",
  "release_date": "2019-04-26",
  "status": "NOW_SHOWING"
}
```

### 1.2 Create Screening Room
- **URL:** `POST http://localhost:1325/api/screeningrooms`
- **Payload:**
```json
{
  "room_name": "Room A1",
  "amount_rows": 10,
  "amount_cols": 12,
  "cinema_id": 1
}
```

### 1.3 Create Showtime
- **URL:** `POST http://localhost:1325/api/showtimes`
- **Payload:**
```json
{
  "movie_id": 1,
  "screening_room_id": 1,
  "start_time": "2026-04-03T19:00:00",
  "end_time": "2026-04-03T22:15:00",
  "buffer_time": 15,
  "status": "SCHEDULED"
}
```

---

## 2) Booking -> Payment -> Ticket

### 2.1 Create Booking With Seats (returns PayOS checkout URL)
- **URL:** `POST http://localhost:1325/api/bookings`
- **Payload:**
```json
{
  "userId": 1,
  "showtimeId": 1,
  "totalPrice": 200000,
  "seats": [
    {
      "seatId": 101,
      "price": 100000
    },
    {
      "seatId": 102,
      "price": 100000
    }
  ]
}
```
- **Expected response:** a string checkout URL (PayOS link).

### 2.2 (Optional) Create Payment directly
- **URL:** `POST http://localhost:1325/payments`
- **Payload:**
```json
{
  "bookingId": 1,
  "amount": 200000
}
```

### 2.3 Mark Payment Success by paymentId (manual/test)
- **URL:** `POST http://localhost:1325/payments/{paymentId}/mark-success`
- **Payload:** none

### 2.4 Mark Payment Success by ref (manual/test)
- **URL:** `POST http://localhost:1325/payments/ref/{ref}/mark-success`
- **Payload:** none

### 2.5 PayOS Webhook endpoint
- **URL:** `POST http://localhost:1325/api/webhook/payment`
- **Payload:** webhook JSON from PayOS (structure depends on PayOS webhook format/signature).

---

## 3) Ticket Validation

### Validate ticket at check-in
- **URL:** `POST http://localhost:1325/api/tickets/validate`
- **Payload:**
```json
{
  "ticketCode": "TICKET-1712050000000-12345"
}
```

---

## 4) Useful Read APIs for verification

### Booking
- `GET http://localhost:1325/api/bookings`
- `GET http://localhost:1325/api/bookings/{bookingId}`

### Payment
- `GET http://localhost:1325/payments`
- `GET http://localhost:1325/payments/{paymentId}`

### Ticket
- `GET http://localhost:1325/api/tickets`
- `GET http://localhost:1325/api/tickets/{ticketId}`
- `GET http://localhost:1325/api/tickets/code/{ticketCode}`

---

## 5) Curl Samples

### Create movie
```bash
curl -X POST "http://localhost:1325/api/movies" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Avengers: Endgame",
    "description":"Superhero movie",
    "genre":"Action",
    "duration_minutes":181,
    "age_rating":"PG-13",
    "release_date":"2019-04-26",
    "status":"NOW_SHOWING"
  }'
```

### Create booking with seats (get checkout URL)
```bash
curl -X POST "http://localhost:1325/api/bookings" \
  -H "Content-Type: application/json" \
  -d '{
    "userId":1,
    "showtimeId":1,
    "totalPrice":200000,
    "seats":[
      {"seatId":101,"price":100000},
      {"seatId":102,"price":100000}
    ]
  }'
```

### Validate ticket
```bash
curl -X POST "http://localhost:1325/api/tickets/validate" \
  -H "Content-Type: application/json" \
  -d '{"ticketCode":"TICKET-1712050000000-12345"}'
```

---

## Notes
- Replace IDs (`movie_id`, `screening_room_id`, `showtimeId`, `bookingId`, `seatId`) with real values from your DB.
- Amount values should match booking total price exactly.
- If authentication is enabled for your environment, include `Authorization: Bearer <token>`.
