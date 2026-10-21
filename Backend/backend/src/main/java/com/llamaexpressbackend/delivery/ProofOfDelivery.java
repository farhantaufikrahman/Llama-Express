package com.llamaexpressbackend.delivery;

import com.llamaexpressbackend.order.DeliveryOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "proofs_of_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private DeliveryOrder order;

    @Column(nullable = false, length = 500)
    private String photoUrl;

    @Column(nullable = false, length = 255)
    private String photoPublicId;

    @Column(length = 100)
    private String receivedBy;

    @Column(nullable = false)
    private Instant capturedAt;

    public static ProofOfDelivery create(DeliveryOrder order, String photoUrl, String photoPublicId,
            String receivedBy, Instant capturedAt) {
        ProofOfDelivery p = new ProofOfDelivery();
        p.order = order;
        p.replace(photoUrl, photoPublicId, receivedBy, capturedAt);
        return p;
    }

    public void replace(String photoUrl, String photoPublicId, String receivedBy, Instant capturedAt) {
        this.photoUrl = photoUrl;
        this.photoPublicId = photoPublicId;
        this.receivedBy = receivedBy;
        this.capturedAt = capturedAt;
    }
}