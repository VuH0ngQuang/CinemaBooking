package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.ShowTimeSeats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    @Query("""
            SELECT sts
            FROM ShowTimeSeats sts
            JOIN FETCH sts.seat
            WHERE sts.showtime.showtime_id = :showtimeId
              AND sts.seat.seat_id IN :seatIds
            """)
    List<ShowTimeSeats> findAllByShowtimeIdAndSeatIdIn(@Param("showtimeId") long showtimeId,
                                                       @Param("seatIds") Collection<Long> seatIds);
}

