package com.group10.cinemabooking.services.events;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Cinemas;
import com.group10.cinemabooking.models.ScreeningRooms;
import com.group10.cinemabooking.models.Showtimes;
import com.group10.cinemabooking.models.Tickets;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.TicketRepository;
import com.group10.cinemabooking.services.MailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingConfirmationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationEmailListener.class);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TicketRepository ticketRepository;
    private final MailService mailService;
    private final AppConf appConf;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingConfirmationEmail(BookingConfirmationEmailEvent event) {
        if (event.bookingId() == null) {
            return;
        }
        try {
            sendBookingConfirmationEmail(event.bookingId());
        } catch (Exception ex) {
            log.warn(
                    "Async booking confirmation email failed for booking {}: {}",
                    event.bookingId(),
                    ex.getMessage()
            );
        }
    }

    private void sendBookingConfirmationEmail(Long bookingId) {
        List<Tickets> tickets = ticketRepository.findByBookingId(bookingId);
        if (tickets == null || tickets.isEmpty()) {
            return;
        }

        Bookings booking = bookingRepository.findByIdWithDetails(bookingId).orElse(null);
        if (booking == null || booking.getUser() == null || booking.getUser().getEmail() == null) {
            return;
        }

        List<BookingSeats> bookingSeats = bookingSeatRepository.findAllByBookingIdJoinFetchSeat(bookingId);
        if (bookingSeats.isEmpty()) {
            return;
        }

        Showtimes showtime = booking.getShowtime();
        if (showtime == null || showtime.getMovie() == null || showtime.getScreeningRoom() == null) {
            return;
        }

        ScreeningRooms room = showtime.getScreeningRoom();
        Cinemas cinema = room.getCinema();
        String bookingCode = (booking.getBooking_code() == null || booking.getBooking_code().isBlank())
                ? "BOOKING-" + booking.getBooking_id()
                : booking.getBooking_code();
        String seatNumbers = bookingSeats.stream()
                .map(bs -> String.valueOf(bs.getSeat().getSeat_label()) + bs.getSeat().getSeat_col())
                .sorted()
                .collect(Collectors.joining(", "));
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=" + bookingCode;
        String showDateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(showtime.getStart_time());
        String totalAmount = String.format("%,d VNĐ", booking.getTotal_price());
        Map<String, Object> vars = new HashMap<>();
        vars.put("username", booking.getUser().getFull_name());
        vars.put("movieName", showtime.getMovie().getTitle());
        vars.put("showDateTime", showDateTime);
        vars.put("screenRoom", room.getRoom_name());
        vars.put("seatNumbers", seatNumbers);
        vars.put("ticketCode", bookingCode);
        vars.put("qrCodeUrl", qrCodeUrl);
        vars.put("cinemaName", cinema != null ? cinema.getName() : "Cinema");
        vars.put("cinemaAddress", cinema != null ? cinema.getAddress() : "");
        vars.put("totalAmount", totalAmount);
        vars.put("myBookingsUrl", appConf.getAppDomain() + "/my-bookings");

        mailService.sendTemplateEmail(
                booking.getUser().getEmail(),
                "Your booking is confirmed",
                "booking-confirmation",
                vars
        );
    }
}
