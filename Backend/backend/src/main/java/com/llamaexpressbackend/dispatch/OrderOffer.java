package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.order.DeliveryOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "order_offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DeliveryOrder order;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.OFFERED;

    @Column(nullable = false)
    private Instant offeredAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant respondedAt;

    public static OrderOffer create(DeliveryOrder order, Long courierId, Instant offeredAt, Instant expiresAt) {
        OrderOffer o = new OrderOffer();
        o.order = order;
        o.courierId = courierId;
        o.offeredAt = offeredAt;
        o.expiresAt = expiresAt;
        return o;
    }

    /**
     * Validasi saja — tidak mengubah apa pun (lihat urutan penguncian di
     * OfferService.accept)
     */
    public void ensureAcceptable(Instant now) {
        if (status != OfferStatus.OFFERED) {
            throw new ConflictException("OFFER_NOT_AVAILABLE", "Tawaran ini sudah tidak tersedia");
        }
        if (!now.isBefore(expiresAt)) {
            throw new ConflictException("OFFER_EXPIRED", "Tawaran sudah kedaluwarsa");
        }
    }

    public void markAccepted(Instant now) {
        ensureAcceptable(now);
        this.status = OfferStatus.ACCEPTED;
        this.respondedAt = now;
    }

    public void reject(Instant now) {
        if (status != OfferStatus.OFFERED) {
            throw new ConflictException("OFFER_NOT_AVAILABLE", "Tawaran ini sudah tidak tersedia");
        }
        this.status = OfferStatus.REJECTED;
        this.respondedAt = now;
    }
}