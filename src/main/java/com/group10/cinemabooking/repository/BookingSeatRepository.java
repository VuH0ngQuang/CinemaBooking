package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.BookingSeats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeats, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(bs) > 0 THEN true ELSE false END
            FROM BookingSeats bs
            WHERE bs.booking.booking_id = :bookingId
              AND bs.seat.seat_id = :seatId
            """)
    boolean existsByBookingIdAndSeatId(@Param("bookingId") long bookingId,
                                       @Param("seatId") long seatId);

    @Query("""
            SELECT bs
            FROM BookingSeats bs
            WHERE bs.booking.booking_id = :bookingId
            """)
    List<BookingSeats> findAllByBookingId(@Param("bookingId") long bookingId);
}