package com.group10.cinemabooking.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationResponseDto {
    private boolean success;
    private String message;
    private TicketDto ticket;
}