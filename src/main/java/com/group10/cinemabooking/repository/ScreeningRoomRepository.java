package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.ScreeningRooms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRoomRepository extends JpaRepository<ScreeningRooms, Long> {
    
}