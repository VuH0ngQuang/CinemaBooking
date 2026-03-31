package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.SeatDto;
import com.group10.cinemabooking.dtos.SeatRequestDto;
import com.group10.cinemabooking.services.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    public ResponseEntity<SeatDto> createSeat(@Valid @RequestBody SeatRequestDto requestDto) {
        SeatDto createdSeat = seatService.createSeat(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSeat);
    }

    @GetMapping
    public ResponseEntity<List<SeatDto>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatDto> getSeatById(@PathVariable("id") Long seatId) {
        return ResponseEntity.ok(seatService.getSeatById(seatId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatDto> updateSeat(@PathVariable("id") Long seatId,
                                              @Valid @RequestBody SeatRequestDto requestDto) {
        return ResponseEntity.ok(seatService.updateSeat(seatId, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable("id") Long seatId) {
        seatService.deleteSeat(seatId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<SeatDto>> getSeatsByRoomId(@PathVariable Long roomId) {
        return ResponseEntity.ok(seatService.getSeatsByRoomId(roomId));
    }
}