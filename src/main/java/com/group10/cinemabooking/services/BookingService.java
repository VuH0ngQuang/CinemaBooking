package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.BookingDto;
import com.group10.cinemabooking.dtos.BookingRequestDto;

import java.util.List;

public interface BookingService {
    BookingDto createBooking(BookingRequestDto requestDto);

    List<BookingDto> getAllBookings();

    BookingDto getBookingById(Long bookingId);

    BookingDto updateBooking(Long bookingId, BookingRequestDto requestDto);

    void deleteBooking(Long bookingId);
}