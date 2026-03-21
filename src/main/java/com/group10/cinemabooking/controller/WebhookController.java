package com.group10.cinemabooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(@RequestBody Object payload) {
        log.info("Received payment webhook: {}", payload.toString());
        return ResponseEntity.ok().build();
    }
}
