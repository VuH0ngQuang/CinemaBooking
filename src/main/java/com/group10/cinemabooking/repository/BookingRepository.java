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
}