package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.TicketDto;
import com.group10.cinemabooking.dtos.BookingValidationRequestDto;
import com.group10.cinemabooking.dtos.BookingValidationResponseDto;
import com.group10.cinemabooking.dtos.TicketValidationRequestDto;
import com.group10.cinemabooking.dtos.TicketValidationResponseDto;

import java.util.List;

public interface TicketService {
    List<TicketDto> getAllTickets();

    TicketDto getTicketById(Long ticketId);

    TicketDto getTicketByCode(String ticketCode);

    List<TicketDto> generateTicketsAfterSuccessfulPayment(Long paymentId);

    TicketValidationResponseDto validateTicket(TicketValidationRequestDto requestDto);

    BookingValidationResponseDto validateBookingCode(BookingValidationRequestDto requestDto);
}