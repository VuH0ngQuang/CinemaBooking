package com.group10.cinemabooking.jobs;

import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "60000")
    @Transactional()
    public void reconcilePendingPayments() {
        Date cutoff = new Date(System.currentTimeMillis() - Duration.ofMinutes(2).toMillis());
        List<Payments> pending = paymentRepository.findPendingBefore(PaymentStatusEnum.PENDING, cutoff);
        for (Payments payment : pending) {
            try {
                // Internal self-heal path: if booking is already PAID/CONFIRMED, replay payment success idempotently.
                if (payment.getBooking().getBooking_status() == BookingStatusEnum.PAID
                        || payment.getBooking().getBooking_status() == BookingStatusEnum.CONFIRMED) {
                    paymentService.markPaymentSuccess(payment.getPayment_id());
                }
            } catch (Exception ex) {
                log.warn("Reconcile payment {} failed: {}", payment.getPayment_id(), ex.getMessage());
            }
        }
    }
}
