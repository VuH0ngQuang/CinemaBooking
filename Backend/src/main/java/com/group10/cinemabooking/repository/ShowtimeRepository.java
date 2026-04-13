package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Showtimes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtimes, Long> {

    @Query("""
            SELECT DISTINCT s
            FROM Showtimes s
            JOIN FETCH s.movie
            JOIN FETCH s.screeningRoom r
            LEFT JOIN FETCH r.cinema
            """)
    List<Showtimes> findAllJoinFetch();
}