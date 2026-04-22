package com.group10.cinemabooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningRoomDto {
    private long room_id;
    private String room_name;
    private int amount_rows;
    private int amount_cols;
    private Long cinema_id;
}