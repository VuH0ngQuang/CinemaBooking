package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingSeatDto {
    private Long bookingSeatId;
    private Long bookingId;
    private Long seatId;
    private Long price;
    private BookingSeatStatusEnum status;
}