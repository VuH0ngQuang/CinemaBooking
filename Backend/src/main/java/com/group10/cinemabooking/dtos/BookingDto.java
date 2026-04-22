package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.BookingStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingDto {
    private Long bookingId;
    private BookingStatusEnum bookingStatus;
    private Long totalPrice;
    private Date confirmedAt;
    private Date expiredAt;
    private Date updatedAt;
    private Date createdAt;
    private Date canceledAt;
    private Long userId;
    private Long showtimeId;
    private Boolean currentDraft;
}
