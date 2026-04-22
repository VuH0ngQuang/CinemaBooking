package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.BookingDto;
import com.group10.cinemabooking.dtos.BookingFullRequestDto;
import com.group10.cinemabooking.dtos.BookingRequestDto;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping("/user/{userId}/visible")
    public ResponseEntity<List<BookingDto>> getVisibleBookingsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getVisibleBookingsByUserId(userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingDto>> getMyVisibleBookings() {
        Users principal = currentPrincipal();
        return ResponseEntity.ok(bookingService.getVisibleBookingsByUserId(principal.getUser_id()));
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

    private Users currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Users principal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }
}