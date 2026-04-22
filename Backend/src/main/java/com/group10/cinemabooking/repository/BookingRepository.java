package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.models.Bookings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Bookings, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.booking_id = :bookingId
            """)
    java.util.Optional<Bookings> findByIdForUpdate(@Param("bookingId") Long bookingId);

    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.booking_status = :status
              AND b.expired_at < :now
            """)
    List<Bookings> findExpired(@Param("status") BookingStatusEnum status,
                               @Param("now") Date now);

    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.booking_code = :bookingCode
            """)
    Optional<Bookings> findByBookingCode(@Param("bookingCode") String bookingCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.booking_code = :bookingCode
            """)
    Optional<Bookings> findByBookingCodeForUpdate(@Param("bookingCode") String bookingCode);

    @Query("""
            SELECT DISTINCT b
            FROM Bookings b
            JOIN FETCH b.user
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.screeningRoom r
            LEFT JOIN FETCH r.cinema
            """)
    List<Bookings> findAllJoinFetch();

    @Query("""
            SELECT b
            FROM Bookings b
            JOIN FETCH b.user
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.screeningRoom r
            LEFT JOIN FETCH r.cinema
            WHERE b.booking_id = :bookingId
            """)
    Optional<Bookings> findByIdWithDetails(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT b
        FROM Bookings b
        JOIN FETCH b.showtime s
        JOIN FETCH s.movie
        WHERE b.user.user_id = :userId
           AND b.booking_status IN :statuses
        ORDER BY b.created_at DESC
        """)
    List<Bookings> findVisibleBookingsByUserId(
            @Param("userId") Long userId,
            @Param("statuses") List<BookingStatusEnum> statuses
    );

    @Query("SELECT b FROM Bookings b" +
           " WHERE b.booking_status = :status" +
           " AND b.expired_at IS NOT NULL" +
           " AND b.expired_at < :now")
    List<Bookings> findAllExpiredPendingBookings(
                @Param("status") BookingStatusEnum status,
                @Param("now") Date now
        );

    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.user.user_id = :userId
              AND b.booking_status = com.group10.cinemabooking.enums.BookingStatusEnum.PENDING
              AND b.currentDraft = true
            ORDER BY b.updated_at DESC, b.created_at DESC
            """)
    List<Bookings> findCurrentDraftBookingsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.currentDraft = true
              AND b.booking_status = com.group10.cinemabooking.enums.BookingStatusEnum.EXPIRED
              AND b.expired_at IS NOT NULL
              AND b.expired_at < :now
            """)
    List<Bookings> findExpiredCurrentDraftBookings(@Param("now") Date now);
}