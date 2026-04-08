package com.group10.cinemabooking.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingValidationRequestDto {
    @NotBlank(message = "Booking code must not be blank")
    private String bookingCode;
}
