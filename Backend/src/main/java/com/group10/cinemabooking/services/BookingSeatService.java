package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.BookingSeatDto;
import com.group10.cinemabooking.dtos.BookingSeatRequestDto;

import java.util.List;

public interface BookingSeatService {
    BookingSeatDto createBookingSeat(BookingSeatRequestDto requestDto);

    List<BookingSeatDto> getAllBookingSeats();

    BookingSeatDto getBookingSeatById(Long bookingSeatId);

    BookingSeatDto updateBookingSeat(Long bookingSeatId, BookingSeatRequestDto requestDto);

    void deleteBookingSeat(Long bookingSeatId);
}