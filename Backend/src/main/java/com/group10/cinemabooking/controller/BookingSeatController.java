package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.BookingSeatDto;
import com.group10.cinemabooking.dtos.BookingSeatRequestDto;
import com.group10.cinemabooking.services.BookingSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking-seats")
@RequiredArgsConstructor
public class BookingSeatController {

    private final BookingSeatService bookingSeatService;

    @PostMapping
    public ResponseEntity<BookingSeatDto> createBookingSeat(@Valid @RequestBody BookingSeatRequestDto requestDto) {
        BookingSeatDto createdBookingSeat = bookingSeatService.createBookingSeat(requestDto);
        return new ResponseEntity<>(createdBookingSeat, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BookingSeatDto>> getAllBookingSeats() {
        List<BookingSeatDto> bookingSeats = bookingSeatService.getAllBookingSeats();
        return ResponseEntity.ok(bookingSeats);
    }

    @GetMapping("/{bookingSeatId}")
    public ResponseEntity<BookingSeatDto> getBookingSeatById(@PathVariable Long bookingSeatId) {
        BookingSeatDto bookingSeat = bookingSeatService.getBookingSeatById(bookingSeatId);
        return ResponseEntity.ok(bookingSeat);
    }

    @PutMapping("/{bookingSeatId}")
    public ResponseEntity<BookingSeatDto> updateBookingSeat(@PathVariable Long bookingSeatId,
                                                            @Valid @RequestBody BookingSeatRequestDto requestDto) {
        BookingSeatDto updatedBookingSeat = bookingSeatService.updateBookingSeat(bookingSeatId, requestDto);
        return ResponseEntity.ok(updatedBookingSeat);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteBookingSeat(@Valid @RequestBody BookingSeatRequestDto requestDto) {
        bookingSeatService.deleteBookingSeat(requestDto);
        return ResponseEntity.noContent().build();
    }
}