package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.ScreeningRooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScreeningRoomRepository extends JpaRepository<ScreeningRooms, Long> {

    @Query("""
            SELECT DISTINCT r
            FROM ScreeningRooms r
            LEFT JOIN FETCH r.cinema
            """)
    List<ScreeningRooms> findAllJoinFetch();
}