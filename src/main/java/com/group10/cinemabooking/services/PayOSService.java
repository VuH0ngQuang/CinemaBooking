package com.group10.cinemabooking.services;

import com.group10.cinemabooking.models.Payments;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

public interface PayOSService {
    String createPaymentRequests (Payments payments);
    WebhookData verifyPayment (Webhook webhook);
}
