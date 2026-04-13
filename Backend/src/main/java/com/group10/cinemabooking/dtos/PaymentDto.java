package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.PaymentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long paymentId;
    private Long amount;
    private PaymentStatusEnum status;
    private String ref;
    private Date createdAt;
    private Date updatedAt;
    private Long bookingId;
}