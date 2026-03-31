package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.ShowTimeSeats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShowTimeSeatRepository extends JpaRepository<ShowTimeSeats, Long> {

    @Query("""
            SELECT sts
            FROM ShowTimeSeats sts
            WHERE sts.showtime.showtime_id = :showtimeId
              AND sts.seat.seat_id = :seatId
            """)
    Optional<ShowTimeSeats> findByShowtimeIdAndSeatId(@Param("showtimeId") long showtimeId,
                                                      @Param("seatId") long seatId);
}

