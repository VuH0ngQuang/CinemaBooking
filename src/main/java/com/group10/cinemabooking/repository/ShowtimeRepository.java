package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Showtimes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtimes, Long> {
}