package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Movies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movies, Long> {

}