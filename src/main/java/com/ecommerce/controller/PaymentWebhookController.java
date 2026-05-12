package com.ecommerce.controller;

import com.ecommerce.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final RazorpayService razorpayService;

    @PostMapping
    public ResponseEntity<String> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader) {
        
        System.out.println("====== Webhook Endpoint Triggered ======");
        try {
            boolean processed = razorpayService.processWebhook(payload, signatureHeader);
            if (processed) {
                return ResponseEntity.ok("Webhook processed successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process webhook event.");
            }
        } catch (SecurityException se) {
            System.err.println("Webhook verification failed: " + se.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature.");
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error processing event.");
        }
    }
}
