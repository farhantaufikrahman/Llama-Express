package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.common.error.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class OrderOfferTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

    private static OrderOffer newOffer() {
        return OrderOffer.create(null, 7L, NOW, NOW.plusSeconds(30));
    }

    @Test
    void accept_withinTtl_succeeds() {
        OrderOffer offer = newOffer();
        offer.markAccepted(NOW.plusSeconds(10));
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
    }

    @Test
    void accept_afterExpiry_isRejected() {
        assertThatThrownBy(() -> newOffer().markAccepted(NOW.plusSeconds(30)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "OFFER_EXPIRED");
    }

    @Test
    void cannotAcceptTwice_orAfterReject() {
        OrderOffer accepted = newOffer();
        accepted.markAccepted(NOW);
        assertThatThrownBy(() -> accepted.markAccepted(NOW))
                .hasFieldOrPropertyWithValue("code", "OFFER_NOT_AVAILABLE");

        OrderOffer rejected = newOffer();
        rejected.reject(NOW);
        assertThatThrownBy(() -> rejected.markAccepted(NOW))
                .hasFieldOrPropertyWithValue("code", "OFFER_NOT_AVAILABLE");
    }
}