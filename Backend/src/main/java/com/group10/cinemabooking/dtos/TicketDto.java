package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.TicketStatusEnum;
import lombok.*;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDto {
    private Long ticketId;
    private String ticketCode;
    private Date issuedAt;
    private Date usedAt;
    private Date validUntil;
    private TicketStatusEnum status;

    private Long bookingId;
    private Long seatId;
    private String seatNumber;
}