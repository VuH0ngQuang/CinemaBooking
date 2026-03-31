package com.group10.cinemabooking.jobs;

import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.ShowTimeSeats;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.ShowTimeSeatRepository;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowTimeSeatRepository showTimeSeatRepository;
    private final LockManager<String> lockManager;

    @Scheduled(fixedDelayString = "30000")
    @Transactional
    public void expirePendingBookings() {
        Date now = new Date();
        List<Bookings> expired = bookingRepository.findExpired(BookingStatusEnum.PENDING, now);
        for (Bookings booking : expired) {
            booking.setBooking_status(BookingStatusEnum.EXPIRED);
            booking.setUpdated_at(now);
            bookingRepository.save(booking);

            List<BookingSeats> seats = bookingSeatRepository.findAllByBookingId(booking.getBooking_id());
            for (BookingSeats bs : seats) {
                long showtimeId = booking.getShowtime().getShowtime_id();
                long seatId = bs.getSeat().getSeat_id();
                String lockKey = "seat:showtime:lock:" + showtimeId + ":" + seatId;
                var lock = lockManager.getLock(lockKey);
                lock.lock();
                try {
                    bs.setStatus(BookingSeatStatusEnum.RELEASED);
                    bookingSeatRepository.save(bs);

                    ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
                    if (sts == null) {
                        continue;
                    }
                    if (sts.getStatus() == ShowtimeSeatsStatusEnum.HELD
                            && String.valueOf(booking.getBooking_id()).equals(sts.getHold_token())) {
                        sts.setStatus(ShowtimeSeatsStatusEnum.AVAILABLE);
                        sts.setHold_token(null);
                        sts.setHold_expires_at(null);
                        showTimeSeatRepository.save(sts);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}

