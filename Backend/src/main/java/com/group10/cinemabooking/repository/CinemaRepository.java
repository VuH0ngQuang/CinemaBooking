package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Cinemas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CinemaRepository extends JpaRepository<Cinemas, Long> {
    
}