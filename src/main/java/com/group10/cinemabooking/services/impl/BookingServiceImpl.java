package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.BookingDto;
import com.group10.cinemabooking.dtos.BookingRequestDto;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Showtimes;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.ShowtimeRepository;
import com.group10.cinemabooking.repository.UserRepository;
import com.group10.cinemabooking.services.BookingService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Bookings> bookingCache;

    @Override
    @Transactional
    public BookingDto createBooking(BookingRequestDto requestDto) {
        validateRequest(requestDto);

        String lockKey = "booking:create:" + requestDto.getUserId() + ":" + requestDto.getShowtimeId();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Users user = userRepository.findById(requestDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + requestDto.getUserId()
                    ));

            Showtimes showtime = showtimeRepository.findById(requestDto.getShowtimeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Showtime not found with id: " + requestDto.getShowtimeId()
                    ));

            Bookings booking = Bookings.builder()
                    .user(user)
                    .showtime(showtime)
                    .total_price(requestDto.getTotalPrice())
                    .booking_status(BookingStatusEnum.PENDING)
                    .expired_at(buildExpiredAt())
                    .build();

            Bookings savedBooking = bookingRepository.save(booking);
            bookingCache.put(savedBooking.getBooking_id(), savedBooking);

            return toDto(savedBooking);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public BookingDto getBookingById(Long bookingId) {
        Bookings booking = bookingCache.getOrLoad(bookingId, key ->
                bookingRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + key))
        );

        return toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto updateBooking(Long bookingId, BookingRequestDto requestDto) {
        validateRequest(requestDto);

        String lockKey = "booking:update:" + bookingId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Bookings existingBooking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

            if (existingBooking.getBooking_status() == BookingStatusEnum.CONFIRMED
                    || existingBooking.getBooking_status() == BookingStatusEnum.CANCELLED
                    || existingBooking.getBooking_status() == BookingStatusEnum.EXPIRED) {
                throw new InvalidRequestException(
                        "Cannot update booking with status: " + existingBooking.getBooking_status()
                );
            }

            Users user = userRepository.findById(requestDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + requestDto.getUserId()
                    ));

            Showtimes showtime = showtimeRepository.findById(requestDto.getShowtimeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Showtime not found with id: " + requestDto.getShowtimeId()
                    ));

            updateFromDto(existingBooking, requestDto, user, showtime);

            Bookings updatedBooking = bookingRepository.save(existingBooking);
            bookingCache.put(updatedBooking.getBooking_id(), updatedBooking);

            return toDto(updatedBooking);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteBooking(Long bookingId) {
        String lockKey = "booking:delete:" + bookingId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Bookings booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

            bookingRepository.delete(booking);
            bookingCache.remove(bookingId);
        } finally {
            lock.unlock();
        }
    }

    private BookingDto toDto(Bookings booking) {
        return BookingDto.builder()
                .bookingId(booking.getBooking_id())
                .bookingStatus(booking.getBooking_status())
                .totalPrice(booking.getTotal_price())
                .confirmedAt(booking.getConfirmed_at())
                .expiredAt(booking.getExpired_at())
                .updatedAt(booking.getUpdated_at())
                .createdAt(booking.getCreated_at())
                .canceledAt(booking.getCanceled_at())
                .userId(booking.getUser() != null ? booking.getUser().getUser_id() : null)
                .showtimeId(booking.getShowtime() != null ? booking.getShowtime().getShowtime_id() : null)
                .build();
    }

    private void updateFromDto(Bookings booking,
                               BookingRequestDto requestDto,
                               Users user,
                               Showtimes showtime) {
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setTotal_price(requestDto.getTotalPrice());
        booking.setUpdated_at(new Date());
    }

    private void validateRequest(BookingRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new InvalidRequestException("User id must not be null");
        }

        if (requestDto.getShowtimeId() == null) {
            throw new InvalidRequestException("Showtime id must not be null");
        }

        if (requestDto.getTotalPrice() == null || requestDto.getTotalPrice() <= 0) {
            throw new InvalidRequestException("Total price must be greater than 0");
        }
    }

    private Date buildExpiredAt() {
        return new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }
}