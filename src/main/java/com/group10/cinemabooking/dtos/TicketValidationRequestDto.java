package com.group10.cinemabooking.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationRequestDto {
    private String ticketCode;
}