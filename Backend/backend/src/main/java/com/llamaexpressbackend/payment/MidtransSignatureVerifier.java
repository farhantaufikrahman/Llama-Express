package com.llamaexpressbackend.payment;

import com.llamaexpressbackend.payment.gateway.MidtransNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class MidtransSignatureVerifier {

    private final MidtransProperties props;

    /** signature_key = SHA512(order_id + status_code + gross_amount + ServerKey) */
    public boolean isValid(MidtransNotification n) {
        if (n.signatureKey() == null || n.orderId() == null || n.statusCode() == null || n.grossAmount() == null) {
            return false;
        }
        String expected = sha512Hex(n.orderId() + n.statusCode() + n.grossAmount() + props.serverKey());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                n.signatureKey().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String sha512Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}