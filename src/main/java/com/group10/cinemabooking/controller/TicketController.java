package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.TicketDto;
import com.group10.cinemabooking.dtos.TicketValidationRequestDto;
import com.group10.cinemabooking.dtos.TicketValidationResponseDto;
import com.group10.cinemabooking.services.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketDto>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDto> getTicketById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketDto> getTicketByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.getTicketByCode(ticketCode));
    }

    @PostMapping("/generate/payment/{paymentId}")
    public ResponseEntity<List<TicketDto>> generateTicketsAfterSuccessfulPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(ticketService.generateTicketsAfterSuccessfulPayment(paymentId));
    }

    @PostMapping("/validate")
    public ResponseEntity<TicketValidationResponseDto> validateTicket(
            @RequestBody TicketValidationRequestDto requestDto
    ) {
        return ResponseEntity.ok(ticketService.validateTicket(requestDto));
    }
}