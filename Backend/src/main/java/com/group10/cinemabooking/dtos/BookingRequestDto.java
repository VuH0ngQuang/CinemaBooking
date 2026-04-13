package com.group10.cinemabooking.dtos;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequestDto {
    @NotNull(message = "User id must not be null")
    private Long userId;
    @NotNull(message = "Showtime id must not be null")
    private Long showtimeId;
}