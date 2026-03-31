package com.group10.cinemabooking.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingSeatRequestDto {
    @NotNull(message = "Booking id must not be null")
    private Long bookingId;
    @NotNull(message = "Seat id must not be null")
    private Long seatId;
    @NotNull(message = "Price must not be null")
    @Positive(message = "Price must be greater than 0")
    private Double price;
}