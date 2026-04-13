package com.group10.cinemabooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {
    private long seat_id;
    private int seat_row;
    private int seat_col;
    private char seat_label;
    private boolean is_active;
    private Long room_id;
}