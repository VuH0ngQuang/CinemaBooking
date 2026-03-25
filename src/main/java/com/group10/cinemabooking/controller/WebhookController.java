package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.services.PayOSService;
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

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final PayOSService payOSService;

    @Autowired
    public WebhookController(PayOSService payOSService) {
        this.payOSService = payOSService;
    }

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(@RequestBody Webhook webhook) {
        payOSService.verifyPayment(webhook);
        return ResponseEntity.ok().build();
    }
}
