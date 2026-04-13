package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.ScreeningRoomDto;
import com.group10.cinemabooking.services.ScreeningRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screeningrooms")
public class ScreeningRoomsController {

    private final ScreeningRoomService screeningRoomService;

    public ScreeningRoomsController(ScreeningRoomService screeningRoomService) {
        this.screeningRoomService = screeningRoomService;
    }

    @PostMapping
    public ResponseEntity<ScreeningRoomDto> createScreeningRoom(@Valid @RequestBody ScreeningRoomDto screeningRoomDto) {
        ScreeningRoomDto createdScreeningRoom = screeningRoomService.createScreeningRoom(screeningRoomDto);
        return new ResponseEntity<>(createdScreeningRoom, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ScreeningRoomDto>> getAllScreeningRooms() {
        List<ScreeningRoomDto> screeningRooms = screeningRoomService.getAllScreeningRooms();
        return ResponseEntity.ok(screeningRooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningRoomDto> getScreeningRoomById(@PathVariable("id") Long roomId) {
        ScreeningRoomDto screeningRoom = screeningRoomService.getScreeningRoomById(roomId);
        return ResponseEntity.ok(screeningRoom);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScreeningRoomDto> updateScreeningRoom(@PathVariable("id") Long roomId,
                                                                @Valid @RequestBody ScreeningRoomDto screeningRoomDto) {
        ScreeningRoomDto updatedScreeningRoom = screeningRoomService.updateScreeningRoom(roomId, screeningRoomDto);
        return ResponseEntity.ok(updatedScreeningRoom);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteScreeningRoom(@PathVariable("id") Long roomId) {
        screeningRoomService.deleteScreeningRoom(roomId);
        return ResponseEntity.ok("Screening room deleted successfully with id: " + roomId);
    }
}