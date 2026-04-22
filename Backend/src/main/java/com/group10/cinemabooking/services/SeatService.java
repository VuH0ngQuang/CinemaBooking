package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.SeatDto;
import com.group10.cinemabooking.dtos.SeatRequestDto;

import java.util.List;

public interface SeatService {
    SeatDto createSeat(SeatRequestDto requestDto);

    List<SeatDto> getAllSeats();

    SeatDto getSeatById(Long seatId);

    SeatDto updateSeat(Long seatId, SeatRequestDto requestDto);

    void deleteSeat(Long seatId);

    List<SeatDto> getSeatsByRoomId(Long roomId);
}