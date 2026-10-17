package com.llamaexpressbackend.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MidtransWebhookController {

    private final PaymentService paymentService;

    /**
     * Body diterima mentah (String) supaya bisa disimpan apa adanya ke
     * raw_notification
     */
    @PostMapping("/api/v1/payments/midtrans/notification")
    public Map<String, String> notification(@RequestBody String rawBody) {
        paymentService.handleNotification(rawBody);
        return Map.of("status", "ok"); // 2xx → Midtrans berhenti mengirim ulang
    }
}