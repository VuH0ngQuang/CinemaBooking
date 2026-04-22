package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.ScreeningRoomDto;

import java.util.List;

public interface ScreeningRoomService {

    ScreeningRoomDto createScreeningRoom(ScreeningRoomDto screeningRoomDto);

    List<ScreeningRoomDto> getAllScreeningRooms();

    ScreeningRoomDto getScreeningRoomById(Long roomId);

    ScreeningRoomDto updateScreeningRoom(Long roomId, ScreeningRoomDto screeningRoomDto);

    void deleteScreeningRoom(Long roomId);
}