package com.group10.cinemabooking.jobs;

import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.ShowTimeSeats;
import com.group10.cinemabooking.models.cache.SeatHoldCacheEntry;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.ShowTimeSeatRepository;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowTimeSeatRepository showTimeSeatRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Bookings> bookingCache;

    @Qualifier("seatHoldCache")
    private final InAppCache<String, SeatHoldCacheEntry> seatHoldCache;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expirePendingBookings() {
        Date now = new Date();

        List<Bookings> expiredBookings = bookingRepository.findAllExpiredPendingBookings(
                BookingStatusEnum.PENDING,
                now
        );

        for (Bookings booking : expiredBookings) {
            expireSingleBookingSafely(booking.getBooking_id());
        }
    }

    private void expireSingleBookingSafely(Long bookingId) {
        String bookingLockKey = "booking:expire:" + bookingId;
        ReentrantLock bookingLock = lockManager.getLock(bookingLockKey);
        bookingLock.lock();

        try {
            Bookings booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                bookingCache.remove(bookingId);
                return;
            }

            if (booking.getBooking_status() != BookingStatusEnum.PENDING) {
                return;
            }

            if (booking.getExpired_at() == null || booking.getExpired_at().after(new Date())) {
                return;
            }

            List<BookingSeats> bookingSeats = bookingSeatRepository.findAllByBookingId(bookingId);

            List<Long> sortedSeatIds = bookingSeats.stream()
                    .map(bs -> bs.getSeat().getSeat_id())
                    .sorted(Comparator.naturalOrder())
                    .toList();

            List<ReentrantLock> seatLocks = new ArrayList<>();
            for (Long seatId : sortedSeatIds) {
                String seatLockKey = "seat:showtime:lock:" + booking.getShowtime().getShowtime_id() + ":" + seatId;
                seatLocks.add(lockManager.getLock(seatLockKey));
            }

            seatLocks.forEach(ReentrantLock::lock);

            try {
                for (BookingSeats bookingSeat : bookingSeats) {
                    long showtimeId = booking.getShowtime().getShowtime_id();
                    long seatId = bookingSeat.getSeat().getSeat_id();

                    // 1. Clear runtime hold cache
                    String holdKey = buildSeatHoldKey(showtimeId, seatId);
                    seatHoldCache.remove(holdKey);

                    // 2. Release DB mirror hold if it belongs to this booking
                    ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(showtimeId, seatId)
                            .orElse(null);

                    if (sts != null
                            && sts.getStatus() == ShowtimeSeatsStatusEnum.HELD
                            && String.valueOf(booking.getBooking_id()).equals(sts.getHold_token())) {
                        sts.setStatus(ShowtimeSeatsStatusEnum.AVAILABLE);
                        sts.setHold_token(null);
                        sts.setHold_expires_at(null);
                        showTimeSeatRepository.save(sts);
                    }

                    // 3. Release booking seat row
                    bookingSeat.setStatus(BookingSeatStatusEnum.RELEASED);
                    bookingSeatRepository.save(bookingSeat);
                }

                // 4. Expire booking
                booking.setBooking_status(BookingStatusEnum.EXPIRED);
                booking.setUpdated_at(new Date());

                Bookings updatedBooking = bookingRepository.save(booking);

                // 5. Update booking cache
                bookingCache.put(updatedBooking.getBooking_id(), updatedBooking);
            } finally {
                for (int i = seatLocks.size() - 1; i >= 0; i--) {
                    seatLocks.get(i).unlock();
                }
            }
        } finally {
            bookingLock.unlock();
        }
    }

    private String buildSeatHoldKey(Long showtimeId, Long seatId) {
        return "showtime:" + showtimeId + ":seat:" + seatId;
    }
}