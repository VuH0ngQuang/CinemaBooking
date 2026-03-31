package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.BookingSeatDto;
import com.group10.cinemabooking.dtos.BookingSeatRequestDto;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.services.BookingSeatService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingSeatServiceImpl implements BookingSeatService {

    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, BookingSeats> bookingSeatCache;

    @Override
    @Transactional
    public BookingSeatDto createBookingSeat(BookingSeatRequestDto requestDto) {
        validateRequest(requestDto);

        String lockKey = "bookingSeat:create:" + requestDto.getBookingId() + ":" + requestDto.getSeatId();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Bookings booking = bookingRepository.findById(requestDto.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with id: " + requestDto.getBookingId()
                    ));

            Seats seat = seatRepository.findById(requestDto.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Seat not found with id: " + requestDto.getSeatId()
                    ));

            boolean exists = bookingSeatRepository.existsByBookingIdAndSeatId(
                    booking.getBooking_id(),
                    seat.getSeat_id()
            );

            if (exists) {
                throw new InvalidRequestException(
                        "Seat already exists in this booking. bookingId=" + booking.getBooking_id()
                                + ", seatId=" + seat.getSeat_id()
                );
            }

            BookingSeats bookingSeat = BookingSeats.builder()
                    .booking(booking)
                    .seat(seat)
                    .price(requestDto.getPrice())
                    .build();

            BookingSeats savedBookingSeat = bookingSeatRepository.save(bookingSeat);
            bookingSeatCache.put(savedBookingSeat.getBooking_seat_id(), savedBookingSeat);

            return toDto(savedBookingSeat);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<BookingSeatDto> getAllBookingSeats() {
        return bookingSeatRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public BookingSeatDto getBookingSeatById(Long bookingSeatId) {
        BookingSeats bookingSeat = bookingSeatCache.getOrLoad(bookingSeatId, key ->
                bookingSeatRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "BookingSeat not found with id: " + key
                        ))
        );

        return toDto(bookingSeat);
    }

    @Override
    @Transactional
    public BookingSeatDto updateBookingSeat(Long bookingSeatId, BookingSeatRequestDto requestDto) {
        validateRequest(requestDto);

        String lockKey = "bookingSeat:update:" + bookingSeatId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            BookingSeats existingBookingSeat = bookingSeatRepository.findById(bookingSeatId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "BookingSeat not found with id: " + bookingSeatId
                    ));

            Bookings booking = bookingRepository.findById(requestDto.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with id: " + requestDto.getBookingId()
                    ));

            Seats seat = seatRepository.findById(requestDto.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Seat not found with id: " + requestDto.getSeatId()
                    ));

            boolean changedBookingOrSeat =
                    existingBookingSeat.getBooking().getBooking_id() != booking.getBooking_id()
                            || existingBookingSeat.getSeat().getSeat_id() != seat.getSeat_id();

            if (changedBookingOrSeat) {
                boolean exists = bookingSeatRepository.existsByBookingIdAndSeatId(
                        booking.getBooking_id(),
                        seat.getSeat_id()
                );

                if (exists) {
                    throw new InvalidRequestException(
                            "Seat already exists in this booking. bookingId=" + booking.getBooking_id()
                                    + ", seatId=" + seat.getSeat_id()
                    );
                }
            }

            updateFromDto(existingBookingSeat, requestDto, booking, seat);

            BookingSeats updatedBookingSeat = bookingSeatRepository.save(existingBookingSeat);
            bookingSeatCache.put(updatedBookingSeat.getBooking_seat_id(), updatedBookingSeat);

            return toDto(updatedBookingSeat);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteBookingSeat(Long bookingSeatId) {
        String lockKey = "bookingSeat:delete:" + bookingSeatId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            BookingSeats bookingSeat = bookingSeatRepository.findById(bookingSeatId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "BookingSeat not found with id: " + bookingSeatId
                    ));

            bookingSeatRepository.delete(bookingSeat);
            bookingSeatCache.remove(bookingSeatId);
        } finally {
            lock.unlock();
        }
    }

    private BookingSeatDto toDto(BookingSeats bookingSeat) {
        return BookingSeatDto.builder()
                .bookingSeatId(bookingSeat.getBooking_seat_id())
                .bookingId(bookingSeat.getBooking() != null ? bookingSeat.getBooking().getBooking_id() : null)
                .seatId(bookingSeat.getSeat() != null ? bookingSeat.getSeat().getSeat_id() : null)
                .price(bookingSeat.getPrice())
                .build();
    }

    private void updateFromDto(BookingSeats bookingSeat,
                               BookingSeatRequestDto requestDto,
                               Bookings booking,
                               Seats seat) {
        bookingSeat.setBooking(booking);
        bookingSeat.setSeat(seat);
        bookingSeat.setPrice(requestDto.getPrice());
    }

    private void validateRequest(BookingSeatRequestDto requestDto) {
        if (requestDto.getBookingId() == null) {
            throw new InvalidRequestException("Booking id must not be null");
        }

        if (requestDto.getSeatId() == null) {
            throw new InvalidRequestException("Seat id must not be null");
        }

        if (requestDto.getPrice() == null || requestDto.getPrice() <= 0) {
            throw new InvalidRequestException("Price must be greater than 0");
        }
    }
}