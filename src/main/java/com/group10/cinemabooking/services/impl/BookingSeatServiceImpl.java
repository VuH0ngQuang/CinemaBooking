package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.BookingSeatDto;
import com.group10.cinemabooking.dtos.BookingSeatRequestDto;
import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.models.ShowTimeSeats;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.repository.ShowTimeSeatRepository;
import com.group10.cinemabooking.services.BookingSeatService;
import com.group10.cinemabooking.services.BookingService;
import com.group10.cinemabooking.services.ShowtimeService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingSeatServiceImpl implements BookingSeatService {

    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowTimeSeatRepository showTimeSeatRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, BookingSeats> bookingSeatCache;
    private final BookingService bookingService;

    @Override
    @Transactional
    public BookingSeatDto createBookingSeat(BookingSeatRequestDto requestDto) {
        validateRequest(requestDto);

        Bookings booking = bookingRepository.findById(requestDto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + requestDto.getBookingId()
                ));

        String lockKey = "bookingSeat:create:" + booking.getShowtime().getShowtime_id() + ":" + requestDto.getSeatId();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            if (booking.getBooking_status() != BookingStatusEnum.PENDING) {
                throw new InvalidRequestException("Booking must be PENDING to add seat");
            }

            Seats seat = seatRepository.findById(requestDto.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Seat not found with id: " + requestDto.getSeatId()
                    ));
            if (seat.getScreeningRoom() == null
                    || seat.getScreeningRoom().getRoom_id() != booking.getShowtime().getScreeningRoom().getRoom_id()) {
                throw new InvalidRequestException(
                        "Seat does not belong to the booking showtime screening room. seatId=" + seat.getSeat_id()
                );
            }

            boolean alreadySelectedInShowtime = bookingSeatRepository.existsActiveSeatByShowtimeAndSeatId(
                    booking.getShowtime().getShowtime_id(),
                    seat.getSeat_id()
            );
            if (alreadySelectedInShowtime) {
                throw new InvalidRequestException(
                        "Seat has already been selected. showtimeId=" + booking.getShowtime().getShowtime_id()
                                + ", seatId=" + seat.getSeat_id()
                );
            }

            ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(
                    booking.getShowtime().getShowtime_id(),
                    seat.getSeat_id()
            ).orElse(null);
            if (sts != null) {
                if (sts.getStatus() == com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.BOOKED) {
                    throw new InvalidRequestException(
                            "Seat has already been selected. showtimeId=" + booking.getShowtime().getShowtime_id()
                                    + ", seatId=" + seat.getSeat_id()
                    );
                }
                if (sts.getStatus() == com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.HELD
                        && sts.getHold_expires_at() != null
                        && sts.getHold_expires_at().after(new Date())) {
                    throw new InvalidRequestException(
                            "Seat has already been selected. showtimeId=" + booking.getShowtime().getShowtime_id()
                                    + ", seatId=" + seat.getSeat_id()
                    );
                }
            }

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
                    .price(booking.getShowtime().getSeat_price())
                    .status(BookingSeatStatusEnum.LOCKED)
                    .build();

            BookingSeats savedBookingSeat = bookingSeatRepository.save(bookingSeat);
            bookingSeatCache.put(savedBookingSeat.getBooking_seat_id(), savedBookingSeat);

            ShowTimeSeats existing = showTimeSeatRepository.findByShowtimeIdAndSeatId(
                    booking.getShowtime().getShowtime_id(),
                    seat.getSeat_id()
            ).orElse(null);
            ShowTimeSeats toSave = existing != null ? existing : ShowTimeSeats.builder()
                    .showtime(booking.getShowtime())
                    .seat(seat)
                    .build();
            toSave.setStatus(com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.HELD);
            toSave.setHold_expires_at(booking.getExpired_at());
            toSave.setHold_token(String.valueOf(booking.getBooking_id()));
            bookingService.updateTotalPrice(booking.getBooking_id(), booking.getTotal_price() + bookingSeat.getPrice());
            showTimeSeatRepository.save(toSave);

            return toDto(savedBookingSeat);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<BookingSeatDto> getAllBookingSeats() {
        return bookingSeatRepository.findAllJoinFetch()
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
            if (booking.getBooking_status() != BookingStatusEnum.PENDING) {
                throw new InvalidRequestException("Booking must be PENDING to update seat");
            }

            Seats seat = seatRepository.findById(requestDto.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Seat not found with id: " + requestDto.getSeatId()
                    ));
            if (seat.getScreeningRoom() == null
                    || seat.getScreeningRoom().getRoom_id() != booking.getShowtime().getScreeningRoom().getRoom_id()) {
                throw new InvalidRequestException(
                        "Seat does not belong to the booking showtime screening room. seatId=" + seat.getSeat_id()
                );
            }

            boolean changedBookingOrSeat =
                    existingBookingSeat.getBooking().getBooking_id() != booking.getBooking_id()
                            || existingBookingSeat.getSeat().getSeat_id() != seat.getSeat_id();

            if (changedBookingOrSeat) {
                boolean alreadySelectedInShowtime = bookingSeatRepository.existsActiveSeatByShowtimeAndSeatIdExcludingBookingSeatId(
                        booking.getShowtime().getShowtime_id(),
                        seat.getSeat_id(),
                        existingBookingSeat.getBooking_seat_id()
                );
                if (alreadySelectedInShowtime) {
                    throw new InvalidRequestException(
                            "Seat has already been selected. showtimeId=" + booking.getShowtime().getShowtime_id()
                                    + ", seatId=" + seat.getSeat_id()
                    );
                }

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
            if (bookingSeat.getBooking().getBooking_status() != BookingStatusEnum.PENDING) {
                throw new InvalidRequestException("Booking must be PENDING to delete seat");
            }

            long showtimeId = bookingSeat.getBooking().getShowtime().getShowtime_id();
            long seatId = bookingSeat.getSeat().getSeat_id();
            ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
            if (sts != null
                    && sts.getStatus() == com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.HELD
                    && String.valueOf(bookingSeat.getBooking().getBooking_id()).equals(sts.getHold_token())) {
                sts.setStatus(com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum.AVAILABLE);
                sts.setHold_token(null);
                sts.setHold_expires_at(null);
                showTimeSeatRepository.save(sts);
            }

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
                .status(bookingSeat.getStatus())
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
    }
}