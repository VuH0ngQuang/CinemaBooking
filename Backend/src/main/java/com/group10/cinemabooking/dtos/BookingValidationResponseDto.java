package com.group10.cinemabooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingValidationResponseDto {
    private boolean success;
    private String message;
    private String bookingCode;
    private List<TicketDto> tickets;
}
