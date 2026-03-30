package com.group10.cinemabooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequestDto {
    private int seat_row;
    private int seat_col;
    private char seat_label;
    private boolean is_active;
    private Long room_id;
}