package com.llamaexpressbackend.payment.gateway;

import com.llamaexpressbackend.common.error.BadGatewayException;
import com.llamaexpressbackend.payment.MidtransProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.midtrans", name = "provider", havingValue = "midtrans", matchIfMissing = true)
public class MidtransClient implements PaymentGateway {

    private final RestClient snapClient;
    private final RestClient apiClient;
    private final MidtransProperties props;

    public MidtransClient(MidtransProperties props) {
        if (!StringUtils.hasText(props.serverKey())) {
            throw new IllegalStateException(
                    "MIDTRANS_SERVER_KEY belum di-set. Isi di backend/.env, atau app.midtrans.provider=fake");
        }
        this.props = props;

        // Midtrans: Basic auth dengan username = Server Key, password kosong
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((props.serverKey() + ":").getBytes(StandardCharsets.UTF_8));

        // SELALU pasang timeout pada HTTP keluar — tanpa ini, Midtrans yang lambat bisa
        // membuat thread kita menggantung
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.snapClient = RestClient.builder().requestFactory(factory).baseUrl(props.snapBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiClient = RestClient.builder().requestFactory(factory).baseUrl(props.apiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public SnapResponse createTransaction(SnapRequest request) {
        try {
            return snapClient.post().uri("/snap/v1/transactions")
                    .headers(h -> {
                        if (StringUtils.hasText(props.notificationUrl())) {
                            h.set("X-Override-Notification", props.notificationUrl()); // URL webhook khusus dev
                                                                                       // (tunnel)
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SnapResponse.class);
        } catch (RestClientException e) {
            log.error("Midtrans Snap gagal untuk {}: {}", request.transactionDetails().orderId(), e.getMessage());
            throw new BadGatewayException("PAYMENT_GATEWAY_ERROR",
                    "Gagal membuat transaksi pembayaran, silakan coba lagi");
        }
    }

    @Override
    public MidtransNotification getStatus(String merchantOrderId) {
        try {
            return apiClient.get().uri("/v2/{orderId}/status", merchantOrderId)
                    .retrieve()
                    .body(MidtransNotification.class);
        } catch (RestClientException e) {
            log.error("Midtrans Status API gagal untuk {}: {}", merchantOrderId, e.getMessage());
            throw new BadGatewayException("PAYMENT_GATEWAY_ERROR", "Gagal mengambil status pembayaran");
        }
    }
}