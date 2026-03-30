package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.ShowtimeStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDto {
    private long showtime_id;
    private ShowtimeStatusEnum status;
    private Date start_time;
    private Date end_time;
    private Date created_at;
    private Date updated_at;
    private int buffer_time;
    private Long movie_id;
    private Long screening_room_id;
}