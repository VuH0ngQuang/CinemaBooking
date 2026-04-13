package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.*;
import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.*;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.repository.ShowTimeSeatRepository;
import com.group10.cinemabooking.repository.ShowtimeRepository;
import com.group10.cinemabooking.repository.UserRepository;
import com.group10.cinemabooking.services.BookingService;
import com.group10.cinemabooking.services.PayOSService;
import com.group10.cinemabooking.services.PaymentService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowTimeSeatRepository showTimeSeatRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Bookings> bookingCache;
    private final PaymentService paymentService;
    private final PayOSService payOSService;

    @Override
    @Transactional
    public BookingDto createBooking(BookingRequestDto requestDto) {
        validateRequest(requestDto);

        String lockKey = "booking:create:" + requestDto.getUserId() + ":" + requestDto.getShowtimeId();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Users user = userRepository.findActiveById(requestDto.getUserId())
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
    @Transactional
    public BookingDto createBookingWithSeats(BookingFullRequestDto requestDto) {
        validateFullRequest(requestDto);

        String bookingLockKey = "booking:full:create:" + requestDto.getUserId() + ":" + requestDto.getShowtimeId();
        ReentrantLock bookingLock = lockManager.getLock(bookingLockKey);
        bookingLock.lock();

        List<Long> sortedSeatIds = requestDto.getSeats().stream()
                .map(SeatSelectionDto::getSeatId)
                .sorted()
                .toList();
        List<ReentrantLock> seatLocks = sortedSeatIds.stream()
                .map(seatId -> lockManager.getLock("seat:showtime:lock:" + requestDto.getShowtimeId() + ":" + seatId))
                .toList();

        seatLocks.forEach(ReentrantLock::lock);
        try {
            Users user = userRepository.findActiveById(requestDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + requestDto.getUserId()
                    ));

            Showtimes showtime = showtimeRepository.findById(requestDto.getShowtimeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Showtime not found with id: " + requestDto.getShowtimeId()
                    ));

            // Batch-fetch existing show_time_seats and seats once instead of per-iteration lookups.
            Map<Long, ShowTimeSeats> existingStsBySeatId = showTimeSeatRepository
                    .findAllByShowtimeIdAndSeatIdIn(showtime.getShowtime_id(), sortedSeatIds)
                    .stream()
                    .collect(Collectors.toMap(s -> s.getSeat().getSeat_id(), s -> s));

            Map<Long, Seats> seatById = seatRepository.findAllById(sortedSeatIds)
                    .stream()
                    .collect(Collectors.toMap(Seats::getSeat_id, s -> s));

            Date now = new Date();
            for (SeatSelectionDto selection : requestDto.getSeats()) {
                ShowTimeSeats sts = existingStsBySeatId.get(selection.getSeatId());
                if (sts != null) {
                    if (sts.getStatus() == com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.BOOKED) {
                        throw new InvalidRequestException("Seat has already been selected. seatId=" + selection.getSeatId());
                    }
                    if (sts.getStatus() == com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.HELD
                            && sts.getHold_expires_at() != null
                            && sts.getHold_expires_at().after(now)) {
                        throw new InvalidRequestException("Seat has already been selected. seatId=" + selection.getSeatId());
                    }
                }
            }

            Bookings booking = Bookings.builder()
                    .user(user)
                    .showtime(showtime)
                    .total_price(requestDto.getTotalPrice())
                    .booking_status(BookingStatusEnum.PENDING)
                    .expired_at(buildExpiredAt())
                    .build();
            Bookings savedBooking = bookingRepository.save(booking);

            for (SeatSelectionDto selection : requestDto.getSeats()) {
                Seats seat = seatById.get(selection.getSeatId());
                if (seat == null) {
                    throw new ResourceNotFoundException(
                            "Seat not found with id: " + selection.getSeatId()
                    );
                }
                if (seat.getScreeningRoom() == null
                        || seat.getScreeningRoom().getRoom_id() != showtime.getScreeningRoom().getRoom_id()) {
                    throw new InvalidRequestException(
                            "Seat does not belong to the showtime screening room. seatId=" + selection.getSeatId()
                    );
                }
                BookingSeats bookingSeat = BookingSeats.builder()
                        .booking(savedBooking)
                        .seat(seat)
                        .price(selection.getPrice())
                        .status(BookingSeatStatusEnum.LOCKED)
                        .build();
                bookingSeatRepository.save(bookingSeat);

                ShowTimeSeats existing = existingStsBySeatId.get(selection.getSeatId());
                ShowTimeSeats toSave = existing != null ? existing : ShowTimeSeats.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .build();
                toSave.setStatus(com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.HELD);
                toSave.setHold_expires_at(savedBooking.getExpired_at());
                toSave.setHold_token(String.valueOf(savedBooking.getBooking_id()));
                showTimeSeatRepository.save(toSave);
            }

            bookingCache.put(savedBooking.getBooking_id(), savedBooking);

            return toDto(savedBooking);
        } finally {
            for (int i = seatLocks.size() - 1; i >= 0; i--) {
                seatLocks.get(i).unlock();
            }
            bookingLock.unlock();
        }
    }

    @Override
    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAllJoinFetch()
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

            Users user = userRepository.findActiveById(requestDto.getUserId())
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

    @Transactional
    @Override
    public void updateTotalPrice(Long bookingId, Long totalPrice) {
        if (totalPrice == null || totalPrice <= 0) {
            throw new InvalidRequestException("Total price must be greater than 0");
        }

        String lockKey = "booking:updatePrice:" + bookingId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Bookings booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

            if (booking.getBooking_status() == BookingStatusEnum.CONFIRMED
                    || booking.getBooking_status() == BookingStatusEnum.CANCELLED
                    || booking.getBooking_status() == BookingStatusEnum.EXPIRED) {
                throw new InvalidRequestException(
                        "Cannot update price for booking with status: " + booking.getBooking_status()
                );
            }

            booking.setTotal_price(totalPrice);
            booking.setUpdated_at(new Date());
            Bookings updatedBooking = bookingRepository.save(booking);
            bookingCache.put(updatedBooking.getBooking_id(), updatedBooking);
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
        booking.setUpdated_at(new Date());
    }

    private void validateRequest(BookingRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new InvalidRequestException("User id must not be null");
        }

        if (requestDto.getShowtimeId() == null) {
            throw new InvalidRequestException("Showtime id must not be null");
        }
    }

    private void validateFullRequest(BookingFullRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidRequestException("Request must not be null");
        }
        if (requestDto.getUserId() == null) {
            throw new InvalidRequestException("User id must not be null");
        }
        if (requestDto.getShowtimeId() == null) {
            throw new InvalidRequestException("Showtime id must not be null");
        }
        if (requestDto.getTotalPrice() == null || requestDto.getTotalPrice() <= 0) {
            throw new InvalidRequestException("Total price must be greater than 0");
        }
        if (requestDto.getSeats() == null || requestDto.getSeats().isEmpty()) {
            throw new InvalidRequestException("Seat selections must not be empty");
        }

        Set<Long> seatIds = new HashSet<>();
        for (SeatSelectionDto seat : requestDto.getSeats()) {
            if (seat.getSeatId() == null) {
                throw new InvalidRequestException("Seat id must not be null");
            }
            if (seat.getPrice() == null || seat.getPrice() <= 0) {
                throw new InvalidRequestException("Seat price must be greater than 0");
            }
            if (!seatIds.add(seat.getSeatId())) {
                throw new InvalidRequestException("Duplicate seat selection detected. seatId=" + seat.getSeatId());
            }
        }
    }

    private Date buildExpiredAt() {
        return new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }
}