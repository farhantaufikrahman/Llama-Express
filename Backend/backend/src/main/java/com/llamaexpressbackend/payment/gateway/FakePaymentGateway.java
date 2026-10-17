package com.llamaexpressbackend.payment.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.midtrans", name = "provider", havingValue = "fake")
public class FakePaymentGateway implements PaymentGateway {

    private final Map<String, String> statuses = new ConcurrentHashMap<>();
    private final Map<String, Long> amounts = new ConcurrentHashMap<>();

    @Override
    public SnapResponse createTransaction(SnapRequest request) {
        String id = request.transactionDetails().orderId();
        amounts.put(id, request.transactionDetails().grossAmount());
        statuses.put(id, "pending");
        return new SnapResponse("fake-token-" + id, "fake://snap/" + id);
    }

    @Override
    public MidtransNotification getStatus(String merchantOrderId) {
        return new MidtransNotification(merchantOrderId, "200", amounts.getOrDefault(merchantOrderId, 0L) + ".00",
                null, statuses.getOrDefault(merchantOrderId, "pending"), "accept", "bank_transfer",
                "fake-tx-" + merchantOrderId);
    }

    /** Dipakai test untuk mensimulasikan perubahan status di sisi Midtrans */
    public void simulate(String merchantOrderId, String transactionStatus) {
        statuses.put(merchantOrderId, transactionStatus);
    }
}