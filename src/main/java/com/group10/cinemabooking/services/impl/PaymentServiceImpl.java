package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.PaymentDto;
import com.group10.cinemabooking.dtos.PaymentRequestDto;
import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.models.ShowTimeSeats;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.BookingSeatRepository;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.repository.ShowTimeSeatRepository;
import com.group10.cinemabooking.services.events.PaymentSucceededEvent;
import com.group10.cinemabooking.services.PaymentService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowTimeSeatRepository showTimeSeatRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Payments> paymentCache;
    private final ApplicationEventPublisher eventPublisher;

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

            PaymentStatusEnum oldStatus = existingPayment.getStatus();
            updateFromDto(existingPayment, requestDto);

            Payments updatedPayment = paymentRepository.save(existingPayment);
            paymentCache.put(updatedPayment.getPayment_id(), updatedPayment);

            if (oldStatus != PaymentStatusEnum.SUCCESS && updatedPayment.getStatus() == PaymentStatusEnum.SUCCESS) {
                return markPaymentSuccess(updatedPayment.getPayment_id());
            }

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

    @Override
    @Transactional
    public PaymentDto markPaymentSuccess(Long paymentId) {
        if (paymentId == null) {
            throw new InvalidRequestException("Payment id must not be null");
        }

        String lockKey = "payment:success:" + paymentId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Payments payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

            if (payment.getStatus() == PaymentStatusEnum.FAILED) {
                throw new InvalidRequestException("Cannot mark a FAILED payment as SUCCESS");
            }

            Bookings booking = bookingRepository.findByIdForUpdate(payment.getBooking().getBooking_id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with id: " + payment.getBooking().getBooking_id()
                    ));

            if (booking.getBooking_status() == BookingStatusEnum.CANCELLED
                    || booking.getBooking_status() == BookingStatusEnum.EXPIRED) {
                throw new InvalidRequestException("Cannot mark payment success for inactive booking");
            }
            if (payment.getAmount() != booking.getTotal_price()) {
                throw new InvalidRequestException("Payment amount must match booking total price");
            }

            boolean statusTransitioned = payment.getStatus() != PaymentStatusEnum.SUCCESS;
            if (statusTransitioned) {
                payment.setStatus(PaymentStatusEnum.SUCCESS);
                payment.setUpdated_at(new Date());
            }
            Payments updatedPayment = paymentRepository.save(payment);

            if (booking.getBooking_status() != BookingStatusEnum.CONFIRMED) {
                booking.setBooking_status(BookingStatusEnum.PAID);
                booking.setConfirmed_at(new Date());
                booking.setUpdated_at(new Date());
                bookingRepository.save(booking);
            }

            finalizeSeatsForSuccessfulPayment(booking);

            paymentCache.put(updatedPayment.getPayment_id(), updatedPayment);
            if (statusTransitioned || booking.getBooking_status() != BookingStatusEnum.CONFIRMED) {
                eventPublisher.publishEvent(new PaymentSucceededEvent(updatedPayment.getPayment_id()));
            }
            return toDto(updatedPayment);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public PaymentDto markPaymentSuccessByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new InvalidRequestException("Payment ref must not be blank");
        }
        Payments payment = paymentRepository.findByRefForUpdate(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ref: " + ref));
        return markPaymentSuccess(payment.getPayment_id());
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

    private void finalizeSeatsForSuccessfulPayment(Bookings booking) {
        List<BookingSeats> bookingSeats = bookingSeatRepository.findAllByBookingId(booking.getBooking_id());
        for (BookingSeats bookingSeat : bookingSeats) {
            long showtimeId = booking.getShowtime().getShowtime_id();
            long seatId = bookingSeat.getSeat().getSeat_id();
            String seatLockKey = "seat:showtime:lock:" + showtimeId + ":" + seatId;
            ReentrantLock seatLock = lockManager.getLock(seatLockKey);
            seatLock.lock();
            try {
                ShowTimeSeats sts = showTimeSeatRepository.findByShowtimeIdAndSeatId(showtimeId, seatId)
                        .orElseGet(() -> ShowTimeSeats.builder()
                                .showtime(booking.getShowtime())
                                .seat(bookingSeat.getSeat())
                                .build());
                sts.setStatus(ShowtimeSeatsStatusEnum.BOOKED);
                sts.setHold_token(null);
                sts.setHold_expires_at(null);
                showTimeSeatRepository.save(sts);
            } finally {
                seatLock.unlock();
            }

            bookingSeat.setStatus(BookingSeatStatusEnum.CONFIRMED);
            bookingSeatRepository.save(bookingSeat);
        }
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