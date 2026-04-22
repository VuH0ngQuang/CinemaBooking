package com.group10.cinemabooking.models.cache;

import java.util.Date;

public class SeatHoldCacheEntry {
    private Long bookingId;
    private Long showtimeId;
    private Long seatId;
    private Date expiresAt;

    public SeatHoldCacheEntry() {
    }

    public SeatHoldCacheEntry(Long bookingId, Long showtimeId, Long seatId, Date expiresAt) {
        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.expiresAt = expiresAt;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Date());
    }
}