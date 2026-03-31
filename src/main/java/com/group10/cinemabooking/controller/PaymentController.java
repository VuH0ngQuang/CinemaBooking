package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.PaymentDto;
import com.group10.cinemabooking.dtos.PaymentRequestDto;
import com.group10.cinemabooking.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody PaymentRequestDto requestDto) {
        PaymentDto createdPayment = paymentService.createPayment(requestDto);
        return new ResponseEntity<>(createdPayment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getAllPayments() {
        List<PaymentDto> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable Long paymentId) {
        PaymentDto payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> updatePayment(@PathVariable Long paymentId,
                                                    @Valid @RequestBody PaymentRequestDto requestDto) {
        PaymentDto updatedPayment = paymentService.updatePayment(paymentId, requestDto);
        return ResponseEntity.ok(updatedPayment);
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<String> deletePayment(@PathVariable Long paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.ok("Payment deleted successfully");
    }
}