package com.group10.cinemabooking.services.events;

import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.services.TicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentSucceededListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededListener.class);

    private final TicketService ticketService;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event.paymentId() == null) {
            return;
        }

        try {
            ticketService.generateTicketsAfterSuccessfulPayment(event.paymentId());
        } catch (Exception ex) {
            // Keep this handler re-entrant: retries or manual replay can regenerate missing tickets.
            log.warn("Async ticket generation skipped/failed for payment {}: {}", event.paymentId(), ex.getMessage());
        }

        Payments payment = paymentRepository.findById(event.paymentId()).orElse(null);
        if (payment == null) {
            return;
        }

        Bookings booking = bookingRepository.findById(payment.getBooking().getBooking_id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + payment.getBooking().getBooking_id()
                ));

        List<BookingSeats> bookingSeats = bookingSeatRepository.findAllByBookingId(booking.getBooking_id());
        for (BookingSeats bs : bookingSeats) {
            if (bs.getStatus() != BookingSeatStatusEnum.CONFIRMED) {
                bs.setStatus(BookingSeatStatusEnum.CONFIRMED);
                bookingSeatRepository.save(bs);
            }
        }

        if (booking.getBooking_status() == BookingStatusEnum.PAID) {
            booking.setBooking_status(BookingStatusEnum.CONFIRMED);
            bookingRepository.save(booking);
        }
    }
}
