package com.llamaexpressbackend.payment;

import com.llamaexpressbackend.payment.gateway.MidtransNotification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MidtransSignatureVerifierTest {

    /** SHA512("LX-TEST-P1" + "200" + "12000.00" + "SB-Mid-server-TEST") */
    private static final String KNOWN_SIGNATURE = "abf9500fb32d05dbdf5b850f40ef4b3fe0809c5f017a786cbb7c935e6d79cd35"
            + "a9f9a447cda2b0c9e28991fab03716db332e88576cf9c7889b5db8bbc8448909";

    private final MidtransSignatureVerifier verifier = new MidtransSignatureVerifier(
            new MidtransProperties("fake", "SB-Mid-server-TEST", null, null, null, 15, null));

    @Test
    void acceptsSignature_matchingMidtransFormula() {
        assertThat(verifier.isValid(notification("12000.00", KNOWN_SIGNATURE))).isTrue();
    }

    @Test
    void rejectsTamperedAmount() {
        assertThat(verifier.isValid(notification("1.00", KNOWN_SIGNATURE))).isFalse();
    }

    @Test
    void rejectsMissingOrForgedSignature() {
        assertThat(verifier.isValid(notification("12000.00", null))).isFalse();
        assertThat(verifier.isValid(notification("12000.00", "palsu"))).isFalse();
    }

    private static MidtransNotification notification(String grossAmount, String signature) {
        return new MidtransNotification("LX-TEST-P1", "200", grossAmount, signature,
                "settlement", "accept", "bank_transfer", "tx-1");
    }
}