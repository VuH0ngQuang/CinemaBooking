package com.group10.cinemabooking.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationRequestDto {
    @NotBlank(message = "Ticket code must not be blank")
    private String ticketCode;
}