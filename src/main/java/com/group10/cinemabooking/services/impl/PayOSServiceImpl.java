package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.dtos.PaymentRequestDto;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.services.PayOSService;
import com.group10.cinemabooking.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import static com.group10.cinemabooking.enums.PaymentStatusEnum.SUCCESS;

@RequiredArgsConstructor
@Service
public class PayOSServiceImpl implements PayOSService {
    private static final Logger log = LoggerFactory.getLogger(PayOSServiceImpl.class);
    private final AppConf appConf;
    private final PayOS payOS;

    @Override
    public String createPaymentRequests(Payments payments) {
        try {
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(payments.getPayment_id())
                    .amount(payments.getAmount())
                    .description("Payment ref: "+payments.getRef())
                    .cancelUrl(appConf.getAppDomain() + "/payment/cancel")
                    .returnUrl(appConf.getAppDomain()+"/payment/success")
                    .expiredAt(java.time.Instant.now()
                            .plus(java.time.Duration.ofMinutes(5))
                            .getEpochSecond())
                    .build();
            CreatePaymentLinkResponse paymentLink = payOS.paymentRequests().create(paymentRequest);
            return paymentLink.getCheckoutUrl();
        } catch (Exception e) {
            log.error("Error create payment link id {}, {}",payments.getPayment_id(),e.getMessage());
            return null;
        }
    }

    @Override
    public WebhookData verifyPayment(Webhook webhook) {
        try {
            return payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.error("Error verifying payment id: {}, {}",webhook.getData().getOrderCode(), e.getMessage());
            return null;
        }
    }
}
