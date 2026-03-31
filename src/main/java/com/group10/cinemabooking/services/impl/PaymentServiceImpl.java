package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.PaymentDto;
import com.group10.cinemabooking.dtos.PaymentRequestDto;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.services.PaymentService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Payments> paymentCache;

    @Override
    @Transactional
    public PaymentDto createPayment(PaymentRequestDto requestDto) {
        validateCreateRequest(requestDto);

        String lockKey = "payment:create:" + requestDto.getBookingId();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Bookings booking = bookingRepository.findById(requestDto.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with id: " + requestDto.getBookingId()
                    ));

            if (booking.getBooking_status() == BookingStatusEnum.CANCELLED
                    || booking.getBooking_status() == BookingStatusEnum.EXPIRED
                    || booking.getBooking_status() == BookingStatusEnum.CONFIRMED
                    || booking.getBooking_status() == BookingStatusEnum.PAID) {
                throw new InvalidRequestException(
                        "Cannot create payment for booking with status: " + booking.getBooking_status()
                );
            }

            if (requestDto.getAmount() != booking.getTotal_price()) {
                throw new InvalidRequestException("Payment amount must match booking total price");
            }

            boolean hasSuccessPayment = paymentRepository.existsByBookingIdAndStatus(
                    requestDto.getBookingId(),
                    PaymentStatusEnum.SUCCESS
            );

            if (hasSuccessPayment) {
                throw new InvalidRequestException("This booking already has a successful payment");
            }

            paymentRepository.findByRef(requestDto.getRef()).ifPresent(existing -> {
                throw new InvalidRequestException("Payment ref already exists: " + requestDto.getRef());
            });

            Payments payment = Payments.builder()
                    .booking(booking)
                    .amount(requestDto.getAmount())
                    .ref(requestDto.getRef())
                    .status(PaymentStatusEnum.PENDING)
                    .build();

            Payments savedPayment = paymentRepository.save(payment);
            paymentCache.put(savedPayment.getPayment_id(), savedPayment);

            return toDto(savedPayment);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public PaymentDto getPaymentById(Long paymentId) {
        Payments payment = paymentCache.getOrLoad(paymentId, key ->
                paymentRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + key))
        );

        return toDto(payment);
    }

    @Override
    @Transactional
    public PaymentDto updatePayment(Long paymentId, PaymentRequestDto requestDto) {
        validateUpdateRequest(requestDto);

        String lockKey = "payment:update:" + paymentId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Payments existingPayment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

            if (existingPayment.getStatus() == PaymentStatusEnum.SUCCESS) {
                throw new InvalidRequestException("Cannot update a successful payment");
            }

            if (requestDto.getRef() != null && !requestDto.getRef().equals(existingPayment.getRef())) {
                paymentRepository.findByRef(requestDto.getRef()).ifPresent(found -> {
                    if (found.getPayment_id() != existingPayment.getPayment_id()) {
                        throw new InvalidRequestException("Payment ref already exists: " + requestDto.getRef());
                    }
                });
            }

            Bookings booking = existingPayment.getBooking();

            if (requestDto.getAmount() != null && requestDto.getAmount() != booking.getTotal_price()) {
                throw new InvalidRequestException("Payment amount must match booking total price");
            }

            updateFromDto(existingPayment, requestDto);

            if (existingPayment.getStatus() == PaymentStatusEnum.SUCCESS) {
                booking.setBooking_status(BookingStatusEnum.PAID);
                booking.setConfirmed_at(new Date());
                booking.setUpdated_at(new Date());
                bookingRepository.save(booking);
            }

            Payments updatedPayment = paymentRepository.save(existingPayment);
            paymentCache.put(updatedPayment.getPayment_id(), updatedPayment);

            return toDto(updatedPayment);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deletePayment(Long paymentId) {
        String lockKey = "payment:delete:" + paymentId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Payments payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

            if (payment.getStatus() == PaymentStatusEnum.SUCCESS) {
                throw new InvalidRequestException("Cannot delete a successful payment");
            }

            paymentRepository.delete(payment);
            paymentCache.remove(paymentId);
        } finally {
            lock.unlock();
        }
    }

    private PaymentDto toDto(Payments payment) {
        return PaymentDto.builder()
                .paymentId(payment.getPayment_id())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .ref(payment.getRef())
                .createdAt(payment.getCreated_at())
                .updatedAt(payment.getUpdated_at())
                .bookingId(payment.getBooking() != null ? payment.getBooking().getBooking_id() : null)
                .build();
    }

    private void updateFromDto(Payments payment, PaymentRequestDto requestDto) {
        if (requestDto.getAmount() != null) {
            payment.setAmount(requestDto.getAmount());
        }

        if (requestDto.getRef() != null && !requestDto.getRef().isBlank()) {
            payment.setRef(requestDto.getRef());
        }

        if (requestDto.getStatus() != null) {
            payment.setStatus(requestDto.getStatus());
        }

        payment.setUpdated_at(new Date());
    }

    private void validateCreateRequest(PaymentRequestDto requestDto) {
        if (requestDto.getBookingId() == null) {
            throw new InvalidRequestException("Booking id must not be null");
        }

        if (requestDto.getAmount() == null || requestDto.getAmount() <= 0) {
            throw new InvalidRequestException("Amount must be greater than 0");
        }

        if (requestDto.getRef() == null || requestDto.getRef().isBlank()) {
            throw new InvalidRequestException("Payment ref must not be blank");
        }
    }

    private void validateUpdateRequest(PaymentRequestDto requestDto) {
        if (requestDto.getAmount() != null && requestDto.getAmount() <= 0) {
            throw new InvalidRequestException("Amount must be greater than 0");
        }

        if (requestDto.getRef() != null && requestDto.getRef().isBlank()) {
            throw new InvalidRequestException("Payment ref must not be blank");
        }

        if (requestDto.getStatus() == null
                && requestDto.getAmount() == null
                && requestDto.getRef() == null) {
            throw new InvalidRequestException("At least one field must be provided for update");
        }
    }
}