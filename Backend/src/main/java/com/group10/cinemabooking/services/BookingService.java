package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.BookingDto;
import com.group10.cinemabooking.dtos.BookingFullRequestDto;
import com.group10.cinemabooking.dtos.BookingRequestDto;

import java.util.List;

public interface BookingService {
    BookingDto createBooking(BookingRequestDto requestDto);

    BookingDto createBookingWithSeats(BookingFullRequestDto requestDto);

    List<BookingDto> getAllBookings();

    BookingDto getBookingById(Long bookingId);

    BookingDto updateBooking(Long bookingId, BookingRequestDto requestDto);

    void updateTotalPrice(Long bookingId, Long totalPrice);

    void deleteBooking(Long bookingId);
    
    List<BookingDto> getVisibleBookingsByUserId(Long userId);
}