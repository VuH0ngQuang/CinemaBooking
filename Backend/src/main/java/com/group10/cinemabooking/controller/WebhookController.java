package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.services.PayOSService;
import com.group10.cinemabooking.services.PaymentService;
import com.group10.cinemabooking.services.TicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final PayOSService payOSService;
    private final PaymentService paymentService;
    private final TicketService ticketService;

    @PostMapping("/payment")
    public ResponseEntity<String> handlePaymentWebhook(@RequestBody Webhook webhook) {
        WebhookData data = payOSService.verifyPayment(webhook);
        if (data != null) {
            paymentService.markPaymentSuccess(data.getOrderCode());
            ticketService.generateTicketsAfterSuccessfulPayment(data.getOrderCode());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Invalid webhook");
        }
    }
}
