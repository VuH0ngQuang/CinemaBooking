package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.PaymentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private Long bookingId;
    private Long amount;
    private String ref;
    private PaymentStatusEnum status;
}