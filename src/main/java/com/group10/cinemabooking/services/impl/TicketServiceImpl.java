package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.TicketDto;
import com.group10.cinemabooking.dtos.TicketValidationRequestDto;
import com.group10.cinemabooking.dtos.TicketValidationResponseDto;
import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.enums.TicketStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.models.Tickets;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.repository.TicketRepository;
import com.group10.cinemabooking.services.TicketService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Tickets> ticketCache;

    @Override
    public List<TicketDto> getAllTickets() {
        return ticketRepository.findAll()
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

            Bookings booking = payment.getBooking();
            List<BookingSeats> bookingSeats = bookingSeatRepository.findAllByBookingId(booking.getBooking_id());

            if (bookingSeats.isEmpty()) {
                throw new InvalidRequestException("Cannot generate ticket because booking has no seats");
            }

            List<TicketDto> generatedTickets = new ArrayList<>();

            for (BookingSeats bookingSeat : bookingSeats) {
                long seatId = bookingSeat.getSeat().getSeat_id();

                boolean alreadyExists = ticketRepository.existsByBookingIdAndSeatId(
                        booking.getBooking_id(),
                        seatId
                );

                if (alreadyExists) {
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
                ticketCache.put(savedTicket.getTicket_id(), savedTicket);

                generatedTickets.add(toDto(savedTicket));
            }

            if (generatedTickets.isEmpty()) {
                throw new InvalidRequestException("All tickets for this payment were already generated");
            }

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
}