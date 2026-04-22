package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.models.Payments;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payments, Long> {

    @Query("SELECT p FROM Payments p WHERE p.booking.booking_id = :bookingId")
    List<Payments> findPaymentsByBookingId(@Param("bookingId") Long bookingId);

    Optional<Payments> findByRef(String ref);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payments p
            WHERE p.payment_id = :paymentId
            """)
    Optional<Payments> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payments p
            WHERE p.ref = :ref
            """)
    Optional<Payments> findByRefForUpdate(@Param("ref") String ref);

    @Query("""
            SELECT p
            FROM Payments p
            WHERE p.status = :status
              AND p.created_at < :cutoff
            """)
    List<Payments> findPendingBefore(@Param("status") PaymentStatusEnum status,
                                     @Param("cutoff") Date cutoff);

    @Query("SELECT COUNT(p) > 0 FROM Payments p WHERE p.booking.booking_id = :bookingId AND p.status = :status")
    boolean existsByBookingIdAndStatus(@Param("bookingId") Long bookingId,
                                       @Param("status") PaymentStatusEnum status);

    @Query("""
            SELECT DISTINCT p
            FROM Payments p
            JOIN FETCH p.booking b
            JOIN FETCH b.user
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.screeningRoom r
            LEFT JOIN FETCH r.cinema
            """)
    List<Payments> findAllJoinFetch();
}