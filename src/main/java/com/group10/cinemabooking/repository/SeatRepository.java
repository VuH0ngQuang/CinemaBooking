package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Seats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seats, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Seats s
            WHERE s.screeningRoom.room_id = :roomId
              AND s.seat_row = :seatRow
              AND s.seat_col = :seatCol
            """)
    boolean existsByRoomAndPosition(@Param("roomId") long roomId,
                                    @Param("seatRow") int seatRow,
                                    @Param("seatCol") int seatCol);

    @Query("""
            SELECT s
            FROM Seats s
            WHERE s.screeningRoom.room_id = :roomId
            """)
    List<Seats> findByRoomId(@Param("roomId") long roomId);
}