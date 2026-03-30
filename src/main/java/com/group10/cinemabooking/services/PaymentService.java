package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.PaymentDto;
import com.group10.cinemabooking.dtos.PaymentRequestDto;

import java.util.List;

public interface PaymentService {
    PaymentDto createPayment(PaymentRequestDto requestDto);

    List<PaymentDto> getAllPayments();

    PaymentDto getPaymentById(Long paymentId);

    PaymentDto updatePayment(Long paymentId, PaymentRequestDto requestDto);

    void deletePayment(Long paymentId);
}