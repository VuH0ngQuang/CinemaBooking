package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.models.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payments, Long> {

    @Query("SELECT p FROM Payments p WHERE p.booking.booking_id = :bookingId")
    List<Payments> findPaymentsByBookingId(@Param("bookingId") Long bookingId);

    Optional<Payments> findByRef(String ref);

    @Query("SELECT COUNT(p) > 0 FROM Payments p WHERE p.booking.booking_id = :bookingId AND p.status = :status")
    boolean existsByBookingIdAndStatus(@Param("bookingId") Long bookingId,
                                       @Param("status") PaymentStatusEnum status);
}