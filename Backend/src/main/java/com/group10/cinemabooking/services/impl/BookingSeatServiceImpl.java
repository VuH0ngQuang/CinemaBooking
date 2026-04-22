package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.BookingSeatDto;
import com.group10.cinemabooking.dtos.BookingSeatRequestDto;
import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.models.ShowTimeSeats;
import com.group10.cinemabooking.models.cache.SeatHoldCacheEntry;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.repository.ShowTimeSeatRepository;
import com.group10.cinemabooking.services.BookingSeatService;
import com.group10.cinemabooking.services.BookingService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    @Qualifier("seatHoldCache")
    private final InAppCache<String, SeatHoldCacheEntry> seatHoldCache;

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
                    || booking.getShowtime().getScreeningRoom() == null
                    || seat.getScreeningRoom().getRoom_id() != booking.getShowtime().getScreeningRoom().getRoom_id()) {
                throw new InvalidRequestException(
                        "Seat does not belong to the booking showtime screening room. seatId=" + seat.getSeat_id()
                );
            }

            String holdKey = buildSeatHoldKey(booking.getShowtime().getShowtime_id(), seat.getSeat_id());
            SeatHoldCacheEntry cachedHold = seatHoldCache.get(holdKey);
            if (cachedHold != null) {
                if (cachedHold.isExpired()) {
                    seatHoldCache.remove(holdKey);
                } else {
                    throw new InvalidRequestException(
                            "Seat is currently held in cache. showtimeId=" + booking.getShowtime().getShowtime_id()
                                    + ", seatId=" + seat.getSeat_id()
                    );
                }
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
                if (sts.getStatus() == ShowtimeSeatsStatusEnum.BOOKED) {
                    throw new InvalidRequestException(
                            "Seat has already been booked. showtimeId=" + booking.getShowtime().getShowtime_id()
                                    + ", seatId=" + seat.getSeat_id()
                    );
                }
                if (sts.getStatus() == ShowtimeSeatsStatusEnum.HELD
                        && sts.getHold_expires_at() != null
                        && sts.getHold_expires_at().after(new Date())) {
                    throw new InvalidRequestException(
                            "Seat is currently held in database. showtimeId=" + booking.getShowtime().getShowtime_id()
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

            applySeatHold(booking, seat);

            long currentTotal = booking.getTotal_price() == null ? 0L : booking.getTotal_price();
            bookingService.updateTotalPrice(booking.getBooking_id(), currentTotal + savedBookingSeat.getPrice());

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

        BookingSeats existingBookingSeat = bookingSeatRepository.findById(bookingSeatId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BookingSeat not found with id: " + bookingSeatId
                ));

        Bookings oldBooking = existingBookingSeat.getBooking();
        Seats oldSeat = existingBookingSeat.getSeat();

        Bookings newBooking = bookingRepository.findById(requestDto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + requestDto.getBookingId()
                ));

        if (newBooking.getBooking_status() != BookingStatusEnum.PENDING) {
            throw new InvalidRequestException("Booking must be PENDING to update seat");
        }

        Seats newSeat = seatRepository.findById(requestDto.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seat not found with id: " + requestDto.getSeatId()
                ));

        if (newSeat.getScreeningRoom() == null
                || newBooking.getShowtime().getScreeningRoom() == null
                || newSeat.getScreeningRoom().getRoom_id() != newBooking.getShowtime().getScreeningRoom().getRoom_id()) {
            throw new InvalidRequestException(
                    "Seat does not belong to the booking showtime screening room. seatId=" + newSeat.getSeat_id()
            );
        }

        String mainLockKey = "bookingSeat:update:" + bookingSeatId;
        ReentrantLock mainLock = lockManager.getLock(mainLockKey);

        Set<String> seatLockKeys = new LinkedHashSet<>();
        seatLockKeys.add("seat:showtime:lock:" + oldBooking.getShowtime().getShowtime_id() + ":" + oldSeat.getSeat_id());
        seatLockKeys.add("seat:showtime:lock:" + newBooking.getShowtime().getShowtime_id() + ":" + newSeat.getSeat_id());

        List<ReentrantLock> seatLocks = new ArrayList<>();
        for (String key : seatLockKeys) {
            seatLocks.add(lockManager.getLock(key));
        }

        mainLock.lock();
        seatLocks.forEach(ReentrantLock::lock);

        try {
            boolean changedBookingOrSeat =
                    oldBooking.getBooking_id() != newBooking.getBooking_id()
                            || oldSeat.getSeat_id() != newSeat.getSeat_id();

            long oldPrice = existingBookingSeat.getPrice() == null ? 0L : existingBookingSeat.getPrice();
            long newPrice = requestDto.getPrice() == null || requestDto.getPrice() <= 0
                    ? newBooking.getShowtime().getSeat_price()
                    : requestDto.getPrice();

            if (changedBookingOrSeat) {
                String newHoldKey = buildSeatHoldKey(newBooking.getShowtime().getShowtime_id(), newSeat.getSeat_id());
                SeatHoldCacheEntry cachedHold = seatHoldCache.get(newHoldKey);
                if (cachedHold != null) {
                    if (cachedHold.isExpired()) {
                        seatHoldCache.remove(newHoldKey);
                    } else if (!String.valueOf(oldBooking.getBooking_id()).equals(String.valueOf(cachedHold.getBookingId()))) {
                        throw new InvalidRequestException(
                                "Seat is currently held in cache. showtimeId=" + newBooking.getShowtime().getShowtime_id()
                                        + ", seatId=" + newSeat.getSeat_id()
                        );
                    }
                }

                boolean alreadySelectedInShowtime = bookingSeatRepository.existsActiveSeatByShowtimeAndSeatIdExcludingBookingSeatId(
                        newBooking.getShowtime().getShowtime_id(),
                        newSeat.getSeat_id(),
                        existingBookingSeat.getBooking_seat_id()
                );
                if (alreadySelectedInShowtime) {
                    throw new InvalidRequestException(
                            "Seat has already been selected. showtimeId=" + newBooking.getShowtime().getShowtime_id()
                                    + ", seatId=" + newSeat.getSeat_id()
                    );
                }

                ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(
                        newBooking.getShowtime().getShowtime_id(),
                        newSeat.getSeat_id()
                ).orElse(null);

                if (sts != null) {
                    if (sts.getStatus() == ShowtimeSeatsStatusEnum.BOOKED) {
                        throw new InvalidRequestException(
                                "Seat has already been booked. showtimeId=" + newBooking.getShowtime().getShowtime_id()
                                        + ", seatId=" + newSeat.getSeat_id()
                        );
                    }
                    if (sts.getStatus() == ShowtimeSeatsStatusEnum.HELD
                            && sts.getHold_expires_at() != null
                            && sts.getHold_expires_at().after(new Date())
                            && !String.valueOf(oldBooking.getBooking_id()).equals(sts.getHold_token())) {
                        throw new InvalidRequestException(
                                "Seat is currently held in database. showtimeId=" + newBooking.getShowtime().getShowtime_id()
                                        + ", seatId=" + newSeat.getSeat_id()
                        );
                    }
                }

                releaseSeatHold(oldBooking, oldSeat);
                applySeatHold(newBooking, newSeat);
            }

            updateFromDto(existingBookingSeat, requestDto, newBooking, newSeat);

            if (existingBookingSeat.getPrice() == null || existingBookingSeat.getPrice() <= 0) {
                existingBookingSeat.setPrice(newPrice);
            } else {
                existingBookingSeat.setPrice(newPrice);
            }

            BookingSeats updatedBookingSeat = bookingSeatRepository.save(existingBookingSeat);
            bookingSeatCache.put(updatedBookingSeat.getBooking_seat_id(), updatedBookingSeat);

            if (oldBooking.getBooking_id() == newBooking.getBooking_id()) {
                long currentTotal = newBooking.getTotal_price() == null ? 0L : newBooking.getTotal_price();
                long recalculatedTotal = currentTotal - oldPrice + newPrice;
                bookingService.updateTotalPrice(newBooking.getBooking_id(), Math.max(recalculatedTotal, 0L));
            } else {
                long oldBookingTotal = oldBooking.getTotal_price() == null ? 0L : oldBooking.getTotal_price();
                long newBookingTotalCurrent = newBooking.getTotal_price() == null ? 0L : newBooking.getTotal_price();

                bookingService.updateTotalPrice(oldBooking.getBooking_id(), Math.max(oldBookingTotal - oldPrice, 0L));
                bookingService.updateTotalPrice(newBooking.getBooking_id(), newBookingTotalCurrent + newPrice);
            }

            return toDto(updatedBookingSeat);
        } finally {
            for (int i = seatLocks.size() - 1; i >= 0; i--) {
                seatLocks.get(i).unlock();
            }
            mainLock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteBookingSeat(BookingSeatRequestDto requestDto) {
        validateRequest(requestDto);

        BookingSeats bookingSeat = bookingSeatRepository.findByBookingIdAndSeatId(
                requestDto.getBookingId(),
                requestDto.getSeatId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "BookingSeat not found for bookingId=" + requestDto.getBookingId()
                        + ", seatId=" + requestDto.getSeatId()
        ));

        Long bookingSeatId = bookingSeat.getBooking_seat_id();

        if (bookingSeat.getBooking().getBooking_status() != BookingStatusEnum.PENDING) {
            throw new InvalidRequestException("Booking must be PENDING to delete seat");
        }

        String mainLockKey = "bookingSeat:delete:" + bookingSeatId;
        String seatLockKey = "seat:showtime:lock:" + bookingSeat.getBooking().getShowtime().getShowtime_id()
                + ":" + bookingSeat.getSeat().getSeat_id();

        ReentrantLock mainLock = lockManager.getLock(mainLockKey);
        ReentrantLock seatLock = lockManager.getLock(seatLockKey);

        mainLock.lock();
        seatLock.lock();
        try {
            releaseSeatHold(bookingSeat.getBooking(), bookingSeat.getSeat());

            long currentTotal = bookingSeat.getBooking().getTotal_price() == null ? 0L : bookingSeat.getBooking().getTotal_price();
            long seatPrice = bookingSeat.getPrice() == null ? 0L : bookingSeat.getPrice();
            bookingService.updateTotalPrice(
                    bookingSeat.getBooking().getBooking_id(),
                    Math.max(currentTotal - seatPrice, 0L)
            );

            bookingSeatRepository.delete(bookingSeat);
            bookingSeatCache.remove(bookingSeatId);
        } finally {
            seatLock.unlock();
            mainLock.unlock();
        }
    }

    private void applySeatHold(Bookings booking, Seats seat) {
        String holdKey = buildSeatHoldKey(booking.getShowtime().getShowtime_id(), seat.getSeat_id());

        SeatHoldCacheEntry holdEntry = new SeatHoldCacheEntry(
                booking.getBooking_id(),
                booking.getShowtime().getShowtime_id(),
                seat.getSeat_id(),
                booking.getExpired_at()
        );
        seatHoldCache.put(holdKey, holdEntry);

        ShowTimeSeats existing = showTimeSeatRepository.findByShowtimeIdAndSeatId(
                booking.getShowtime().getShowtime_id(),
                seat.getSeat_id()
        ).orElse(null);

        ShowTimeSeats toSave = existing != null ? existing : ShowTimeSeats.builder()
                .showtime(booking.getShowtime())
                .seat(seat)
                .build();

        toSave.setStatus(ShowtimeSeatsStatusEnum.HELD);
        toSave.setHold_expires_at(booking.getExpired_at());
        toSave.setHold_token(String.valueOf(booking.getBooking_id()));
        showTimeSeatRepository.save(toSave);
    }

    private void releaseSeatHold(Bookings booking, Seats seat) {
        long showtimeId = booking.getShowtime().getShowtime_id();
        long seatId = seat.getSeat_id();
        String holdKey = buildSeatHoldKey(showtimeId, seatId);

        seatHoldCache.remove(holdKey);

        ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
        if (sts != null
                && sts.getStatus() == ShowtimeSeatsStatusEnum.HELD
                && String.valueOf(booking.getBooking_id()).equals(sts.getHold_token())) {
            sts.setStatus(ShowtimeSeatsStatusEnum.AVAILABLE);
            sts.setHold_token(null);
            sts.setHold_expires_at(null);
            showTimeSeatRepository.save(sts);
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

    private String buildSeatHoldKey(Long showtimeId, Long seatId) {
        return "showtime:" + showtimeId + ":seat:" + seatId;
    }
}