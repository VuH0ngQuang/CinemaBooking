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

    @Query("""
            SELECT CASE WHEN COUNT(bs) > 0 THEN true ELSE false END
            FROM BookingSeats bs
            WHERE bs.booking.showtime.showtime_id = :showtimeId
              AND bs.seat.seat_id = :seatId
              AND bs.booking.booking_status IN (
                com.group10.cinemabooking.enums.BookingStatusEnum.PENDING,
                com.group10.cinemabooking.enums.BookingStatusEnum.PAID,
                com.group10.cinemabooking.enums.BookingStatusEnum.CONFIRMED
              )
            """)
    boolean existsActiveSeatByShowtimeAndSeatId(@Param("showtimeId") long showtimeId,
                                                @Param("seatId") long seatId);

    @Query("""
            SELECT CASE WHEN COUNT(bs) > 0 THEN true ELSE false END
            FROM BookingSeats bs
            WHERE bs.booking.showtime.showtime_id = :showtimeId
              AND bs.seat.seat_id = :seatId
              AND bs.booking_seat_id <> :bookingSeatId
              AND bs.booking.booking_status IN (
                com.group10.cinemabooking.enums.BookingStatusEnum.PENDING,
                com.group10.cinemabooking.enums.BookingStatusEnum.PAID,
                com.group10.cinemabooking.enums.BookingStatusEnum.CONFIRMED
              )
            """)
    boolean existsActiveSeatByShowtimeAndSeatIdExcludingBookingSeatId(@Param("showtimeId") long showtimeId,
                                                                      @Param("seatId") long seatId,
                                                                      @Param("bookingSeatId") long bookingSeatId);

    @Query("""
            SELECT bs
            FROM BookingSeats bs
            WHERE bs.booking.booking_id = :bookingId
              AND bs.seat.seat_id = :seatId
            """)
    java.util.Optional<BookingSeats> findByBookingIdAndSeatId(@Param("bookingId") long bookingId,
                                                              @Param("seatId") long seatId);

    @Query("""
            SELECT DISTINCT bs
            FROM BookingSeats bs
            JOIN FETCH bs.booking
            JOIN FETCH bs.seat
            """)
    List<BookingSeats> findAllJoinFetch();

    @Query("""
            SELECT bs
            FROM BookingSeats bs
            JOIN FETCH bs.seat
            WHERE bs.booking.booking_id = :bookingId
            """)
    List<BookingSeats> findAllByBookingIdJoinFetchSeat(@Param("bookingId") long bookingId);
}