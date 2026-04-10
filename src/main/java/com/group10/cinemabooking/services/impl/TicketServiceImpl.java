package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.dtos.BookingValidationRequestDto;
import com.group10.cinemabooking.dtos.BookingValidationResponseDto;
import com.group10.cinemabooking.dtos.TicketDto;
import com.group10.cinemabooking.dtos.TicketValidationRequestDto;
import com.group10.cinemabooking.dtos.TicketValidationResponseDto;
import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.enums.TicketStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.*;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.repository.TicketRepository;
import com.group10.cinemabooking.services.MailService;
import com.group10.cinemabooking.services.TicketService;
import com.group10.cinemabooking.utils.BoundedFlushHelper;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {
    private static final int BATCH_FLUSH_SIZE = 1000;
    private static final long BATCH_FLUSH_INTERVAL_MS = 1000L;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Tickets> ticketCache;
    private final EntityManager entityManager;
    private final MailService mailService;
    private final AppConf appConf;

    @Override
    public List<TicketDto> getAllTickets() {
        return ticketRepository.findAllJoinFetch()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public TicketDto getTicketById(Long ticketId) {
        Tickets ticket = ticketCache.getOrLoad(ticketId, key ->
                ticketRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + key))
        );

        return toDto(ticket);
    }

    @Override
    public TicketDto getTicketByCode(String ticketCode) {
        validateTicketCode(ticketCode);

        Tickets ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));

        ticketCache.put(ticket.getTicket_id(), ticket);
        return toDto(ticket);
    }

    @Override
    @Transactional
    public List<TicketDto> generateTicketsAfterSuccessfulPayment(Long paymentId) {
        if (paymentId == null) {
            throw new InvalidRequestException("Payment id must not be null");
        }

        String lockKey = "ticket:generate:payment:" + paymentId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Payments payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

            if (payment.getStatus() != PaymentStatusEnum.SUCCESS) {
                throw new InvalidRequestException("Tickets can only be generated when payment status is SUCCESS");
            }

            // Refetch booking with the full association graph required for the post-commit
            // confirmation email so its lazy proxies are initialized inside this transaction.
            Long bookingId = payment.getBooking().getBooking_id();
            Bookings booking = bookingRepository.findByIdWithDetails(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with id: " + bookingId
                    ));
            // Join-fetch seat so booking-seat → seat is initialized for the email and ticket build.
            List<BookingSeats> bookingSeats = bookingSeatRepository.findAllByBookingIdJoinFetchSeat(bookingId);

            if (bookingSeats.isEmpty()) {
                throw new InvalidRequestException("Cannot generate ticket because booking has no seats");
            }

            // Batch the per-seat existence check.
            List<Long> seatIds = bookingSeats.stream()
                    .map(bs -> bs.getSeat().getSeat_id())
                    .toList();
            Set<Long> existingSeatIds = new HashSet<>(
                    ticketRepository.findExistingSeatIdsByBookingIdAndSeatIdIn(bookingId, seatIds)
            );

            List<TicketDto> generatedTickets = new ArrayList<>();
            BoundedFlushHelper boundedFlushHelper = new BoundedFlushHelper(
                    entityManager,
                    BATCH_FLUSH_SIZE,
                    BATCH_FLUSH_INTERVAL_MS
            );

            for (BookingSeats bookingSeat : bookingSeats) {
                long seatId = bookingSeat.getSeat().getSeat_id();

                if (existingSeatIds.contains(seatId)) {
                    continue;
                }

                Tickets ticket = Tickets.builder()
                        .booking(booking)
                        .seat(bookingSeat.getSeat())
                        .ticket_code(generateUniqueTicketCode())
                        .issued_at(new Date())
                        .valid_until(buildValidUntil(booking))
                        .status(TicketStatusEnum.VALID)
                        .build();

                Tickets savedTicket = ticketRepository.save(ticket);
                boundedFlushHelper.onWrite();
                ticketCache.put(savedTicket.getTicket_id(), savedTicket);

                generatedTickets.add(toDto(savedTicket));
            }
            boundedFlushHelper.forceFlush();
            if (generatedTickets.isEmpty()) {
                return ticketRepository.findByBookingId(bookingId)
                        .stream()
                        .map(this::toDto)
                        .toList();
            }
            registerBookingConfirmationEmailAfterCommit(booking, bookingSeats, generatedTickets);
            return generatedTickets;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public TicketValidationResponseDto validateTicket(TicketValidationRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidRequestException("Validation request must not be null");
        }

        validateTicketCode(requestDto.getTicketCode());

        String lockKey = "ticket:validate:" + requestDto.getTicketCode();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Tickets ticket = ticketRepository.findByTicketCode(requestDto.getTicketCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ticket not found with code: " + requestDto.getTicketCode()
                    ));

            Date now = new Date();

            if (ticket.getStatus() == TicketStatusEnum.USED) {
                return TicketValidationResponseDto.builder()
                        .success(false)
                        .message("Ticket already used")
                        .ticket(toDto(ticket))
                        .build();
            }

            if (ticket.getStatus() == TicketStatusEnum.EXPIRED || ticket.getValid_until().before(now)) {
                if (ticket.getStatus() != TicketStatusEnum.EXPIRED) {
                    ticket.setStatus(TicketStatusEnum.EXPIRED);
                    Tickets expiredTicket = ticketRepository.save(ticket);
                    ticketCache.put(expiredTicket.getTicket_id(), expiredTicket);
                    ticket = expiredTicket;
                }

                return TicketValidationResponseDto.builder()
                        .success(false)
                        .message("Ticket expired")
                        .ticket(toDto(ticket))
                        .build();
            }

            ticket.setStatus(TicketStatusEnum.USED);
            ticket.setUsed_at(now);

            Tickets updatedTicket = ticketRepository.save(ticket);
            ticketCache.put(updatedTicket.getTicket_id(), updatedTicket);

            return TicketValidationResponseDto.builder()
                    .success(true)
                    .message("Ticket is valid and has been marked as used")
                    .ticket(toDto(updatedTicket))
                    .build();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public BookingValidationResponseDto validateBookingCode(BookingValidationRequestDto requestDto) {
        if (requestDto == null || requestDto.getBookingCode() == null || requestDto.getBookingCode().isBlank()) {
            throw new InvalidRequestException("Booking code must not be blank");
        }

        String bookingCode = requestDto.getBookingCode().trim();
        String lockKey = "ticket:validate:booking:" + bookingCode;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Bookings booking = bookingRepository.findByBookingCodeForUpdate(bookingCode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with code: " + bookingCode
                    ));

            List<Tickets> tickets = ticketRepository.findByBookingId(booking.getBooking_id());
            if (tickets.isEmpty()) {
                throw new InvalidRequestException("No tickets found for booking code: " + bookingCode);
            }

            Date now = new Date();
            boolean hasValid = false;
            boolean hasUsed = false;
            List<TicketDto> updatedTickets = new ArrayList<>();

            for (Tickets ticket : tickets) {
                if (ticket.getStatus() == TicketStatusEnum.USED) {
                    hasUsed = true;
                    updatedTickets.add(toDto(ticket));
                    continue;
                }
                if (ticket.getStatus() == TicketStatusEnum.EXPIRED || ticket.getValid_until().before(now)) {
                    if (ticket.getStatus() != TicketStatusEnum.EXPIRED) {
                        ticket.setStatus(TicketStatusEnum.EXPIRED);
                        ticket = ticketRepository.save(ticket);
                    }
                    updatedTickets.add(toDto(ticket));
                    continue;
                }

                hasValid = true;
                ticket.setStatus(TicketStatusEnum.USED);
                ticket.setUsed_at(now);
                Tickets saved = ticketRepository.save(ticket);
                ticketCache.put(saved.getTicket_id(), saved);
                updatedTickets.add(toDto(saved));
            }

            if (hasValid) {
                return BookingValidationResponseDto.builder()
                        .success(true)
                        .message("Booking is valid and all tickets have been marked as used")
                        .bookingCode(bookingCode)
                        .tickets(updatedTickets)
                        .build();
            }

            if (hasUsed) {
                return BookingValidationResponseDto.builder()
                        .success(false)
                        .message("All booking tickets already used")
                        .bookingCode(bookingCode)
                        .tickets(updatedTickets)
                        .build();
            }

            return BookingValidationResponseDto.builder()
                    .success(false)
                    .message("All booking tickets expired")
                    .bookingCode(bookingCode)
                    .tickets(updatedTickets)
                    .build();
        } finally {
            lock.unlock();
        }
    }

    private TicketDto toDto(Tickets ticket) {
        return TicketDto.builder()
                .ticketId(ticket.getTicket_id())
                .ticketCode(ticket.getTicket_code())
                .issuedAt(ticket.getIssued_at())
                .usedAt(ticket.getUsed_at())
                .validUntil(ticket.getValid_until())
                .status(ticket.getStatus())
                .bookingId(ticket.getBooking() != null ? ticket.getBooking().getBooking_id() : null)
                .seatId(ticket.getSeat() != null ? ticket.getSeat().getSeat_id() : null)
                .seatNumber(buildSeatNumber(ticket))
                .build();
    }

    private String buildSeatNumber(Tickets ticket) {
        if (ticket.getSeat() == null) {
            return null;
        }

        return String.valueOf(ticket.getSeat().getSeat_label()) + ticket.getSeat().getSeat_col();
    }

    private String generateUniqueTicketCode() {
        String ticketCode;
        do {
            ticketCode = "TICKET-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 100000);
        } while (ticketRepository.existsByTicketCode(ticketCode));

        return ticketCode;
    }

    private Date buildValidUntil(Bookings booking) {
        if (booking.getShowtime() == null || booking.getShowtime().getEnd_time() == null) {
            return new Date();
        }

        long thirtyMinutesInMillis = 30L * 60L * 1000L;
        return new Date(booking.getShowtime().getEnd_time().getTime() + thirtyMinutesInMillis);
    }

    private void validateTicketCode(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw new InvalidRequestException("Ticket code must not be blank");
        }
    }

    private void registerBookingConfirmationEmailAfterCommit(
            Bookings booking,
            List<BookingSeats> bookingSeats,
            List<TicketDto> tickets
    ) {
        if (booking == null || booking.getUser() == null || booking.getUser().getEmail() == null) {
            throw new InvalidRequestException("Booking or User must not be null");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            sendBookingConfirmationEmail(booking, tickets, bookingSeats);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendBookingConfirmationEmail(booking, tickets, bookingSeats);
            }
        });
    }

    private void sendBookingConfirmationEmail(
            Bookings booking,
            List<TicketDto> tickets,
            List<BookingSeats> bookingSeats
    ) {
        if (tickets == null || tickets.isEmpty()) return;
        Showtimes showtime = booking.getShowtime();
        if (showtime == null || showtime.getMovie() == null || showtime.getScreeningRoom() == null) return;
        ScreeningRooms room = showtime.getScreeningRoom();
        Cinemas cinema = room.getCinema();
        String bookingCode = (booking.getBooking_code() == null || booking.getBooking_code().isBlank())
                ? "BOOKING-" + booking.getBooking_id()
                : booking.getBooking_code();
        String seatNumbers = bookingSeats
                .stream()
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