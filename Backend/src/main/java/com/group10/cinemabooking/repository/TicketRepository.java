package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Tickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Tickets, Long> {

    @Query("SELECT t FROM Tickets t WHERE t.ticket_code = :ticketCode")
    Optional<Tickets> findByTicketCode(@Param("ticketCode") String ticketCode);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tickets t WHERE t.ticket_code = :ticketCode")
    boolean existsByTicketCode(@Param("ticketCode") String ticketCode);

    @Query("SELECT t FROM Tickets t WHERE t.booking.booking_id = :bookingId")
    List<Tickets> findByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM Tickets t " +
           "WHERE t.booking.booking_id = :bookingId AND t.seat.seat_id = :seatId")
    boolean existsByBookingIdAndSeatId(@Param("bookingId") Long bookingId,
                                       @Param("seatId") Long seatId);

    @Query("""
            SELECT DISTINCT t
            FROM Tickets t
            JOIN FETCH t.booking
            JOIN FETCH t.seat
            """)
    List<Tickets> findAllJoinFetch();

    @Query("""
            SELECT t.seat.seat_id
            FROM Tickets t
            WHERE t.booking.booking_id = :bookingId
              AND t.seat.seat_id IN :seatIds
            """)
    List<Long> findExistingSeatIdsByBookingIdAndSeatIdIn(@Param("bookingId") Long bookingId,
                                                          @Param("seatIds") List<Long> seatIds);
}