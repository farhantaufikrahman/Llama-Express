package com.llamaexpressbackend.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DeliveryOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus toStatus;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private Instant occurredAt;

    public static OrderStatusHistory record(DeliveryOrder order, OrderStatus from, Long actorId, String note,
            Instant at) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.order = order;
        h.fromStatus = from;
        h.toStatus = order.getStatus();
        h.actorId = actorId;
        h.note = note;
        h.occurredAt = at;
        return h;
    }
}