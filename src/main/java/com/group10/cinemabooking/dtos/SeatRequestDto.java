package com.group10.cinemabooking.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatRequestDto {
    @Positive(message = "Seat row must be greater than 0")
    private int seat_row;
    @Positive(message = "Seat col must be greater than 0")
    private int seat_col;
    private char seat_label;
    private boolean is_active;
    @NotNull(message = "Room id must not be null")
    private Long room_id;
}