package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSeatStatusDto {
    private long seatId;
    private int seatRow;
    private int seatCol;
    private char seatLabel;
    private boolean active;
    private ShowtimeSeatsStatusEnum status;
    private Date holdExpiresAt;
    private boolean selectedByCurrentBooking;
}
