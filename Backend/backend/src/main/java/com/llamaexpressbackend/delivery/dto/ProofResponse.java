package com.llamaexpressbackend.delivery.dto;

import com.llamaexpressbackend.delivery.ProofOfDelivery;
import java.time.Instant;

public record ProofResponse(String orderCode, String photoUrl, String receivedBy, Instant capturedAt) {
    public static ProofResponse from(String orderCode, ProofOfDelivery p) {
        return new ProofResponse(orderCode, p.getPhotoUrl(), p.getReceivedBy(), p.getCapturedAt());
    }
}