package com.llamaexpressbackend.payment;

import com.llamaexpressbackend.payment.gateway.FakePaymentGateway;
import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String SERVER_KEY = "SB-Mid-server-TEST"; // sama dengan application-test.yml

    @Autowired
    FakePaymentGateway gateway;

    private record PendingPayment(String customerToken, String code, String merchantOrderId, String gross) {
    }

    @Test
    void settlementWebhook_marksPaid_andStartsDispatch_exactlyOnce() throws Exception {
        PendingPayment p = pendingPayment(-6.10, 106.70);

        webhook(p, "settlement", p.gross(), "signature-palsu").andExpect(status().isForbidden());

        webhook(p, "settlement", p.gross(), null).andExpect(status().isOk());
        webhook(p, "settlement", p.gross(), null).andExpect(status().isOk()); // duplikat → tetap 200, tanpa efek

        mockMvc.perform(get("/api/v1/orders/" + p.code()).header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.timeline[?(@.note == 'Pembayaran diterima')]", hasSize(1)));

        mockMvc.perform(
                post("/api/v1/orders/" + p.code() + "/payment").header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PAID"));
    }

    @Test
    void validSignature_butWrongAmount_isIgnored() throws Exception {
        PendingPayment p = pendingPayment(-6.12, 106.72);

        webhook(p, "settlement", "1.00", null).andExpect(status().isOk()); // signature cocok untuk "1.00", tapi tagihan
                                                                           // 12000

        mockMvc.perform(get("/api/v1/orders/" + p.code()).header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createPayment_twice_reusesSameToken_andExpiredPaymentCanBeRetried() throws Exception {
        PendingPayment p = pendingPayment(-6.15, 106.75);

        mockMvc.perform(
                post("/api/v1/orders/" + p.code() + "/payment").header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantOrderId").value(p.merchantOrderId()));

        webhook(p, "expire", p.gross(), null).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders/" + p.code()).header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(
                post("/api/v1/orders/" + p.code() + "/payment").header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantOrderId", endsWith("-P2")));
    }

    @Test
    void sync_pullsStatusFromGateway() throws Exception {
        PendingPayment p = pendingPayment(-6.25, 106.85);
        gateway.simulate(p.merchantOrderId(), "settlement");

        mockMvc.perform(
                post("/api/v1/orders/" + p.code() + "/payment/sync").header(AUTHORIZATION, bearer(p.customerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void cashOrder_cannotBePaidViaMidtrans() throws Exception {
        String token = registerCustomer();
        String code = createCashOrderAt(token, -6.30, 106.90);

        mockMvc.perform(post("/api/v1/orders/" + code + "/payment").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_MIDTRANS_ORDER"));
    }

    // ---------- helper khusus test ini ----------

    private PendingPayment pendingPayment(double lat, double lng) throws Exception {
        String token = registerCustomer();
        String code = createOrderAt(token, lat, lng, "MIDTRANS");
        MvcResult r = mockMvc.perform(post("/api/v1/orders/" + code + "/payment").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.snapToken").isNotEmpty())
                .andReturn();
        Number amount = read(r, "$.amount");
        return new PendingPayment(token, code, read(r, "$.merchantOrderId"),
                String.format(Locale.ROOT, "%.2f", amount.doubleValue()));
    }

    /**
     * signature null → dihitung dengan rumus Midtrans; selain null → dipakai apa
     * adanya (untuk uji pemalsuan)
     */
    private ResultActions webhook(PendingPayment p, String transactionStatus, String gross, String signature)
            throws Exception {
        String sig = (signature != null) ? signature : sha512(p.merchantOrderId() + "200" + gross + SERVER_KEY);
        String body = """
                {"order_id":"%s","status_code":"200","gross_amount":"%s","signature_key":"%s",
                 "transaction_status":"%s","fraud_status":"accept","payment_type":"bank_transfer","transaction_id":"tx-%s"}
                """
                .formatted(p.merchantOrderId(), gross, sig, transactionStatus, p.merchantOrderId());
        return mockMvc
                .perform(post("/api/v1/payments/midtrans/notification").contentType(APPLICATION_JSON).content(body));
    }

    private static String sha512(String input) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-512").digest(input.getBytes(StandardCharsets.UTF_8)));
    }
}