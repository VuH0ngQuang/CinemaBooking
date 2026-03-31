package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.PaymentStatusEnum;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Positive(message = "Amount must be greater than 0")
    private Long amount;
    @Size(min = 1, message = "Payment ref must not be blank")
    private String ref;
    private PaymentStatusEnum status;
}