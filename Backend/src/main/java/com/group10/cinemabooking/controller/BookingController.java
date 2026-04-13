package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.BookingDto;
import com.group10.cinemabooking.dtos.BookingFullRequestDto;
import com.group10.cinemabooking.dtos.BookingRequestDto;
import com.group10.cinemabooking.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody BookingRequestDto requestDto) {
        BookingDto bookingDto = bookingService.createBooking(requestDto);
        return new ResponseEntity<>(bookingDto, HttpStatus.CREATED);
    }

    @PostMapping("/full")
    public ResponseEntity<BookingDto> createBookingWithSeats(@Valid @RequestBody BookingFullRequestDto requestDto) {
        BookingDto createdBooking = bookingService.createBookingWithSeats(requestDto);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        List<BookingDto> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto> getBookingById(@PathVariable Long bookingId) {
        BookingDto booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<BookingDto> updateBooking(@PathVariable Long bookingId,
                                                    @Valid @RequestBody BookingRequestDto requestDto) {
        BookingDto updatedBooking = bookingService.updateBooking(bookingId, requestDto);
        return ResponseEntity.ok(updatedBooking);
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long bookingId) {
        bookingService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
}