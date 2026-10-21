package com.llamaexpressbackend.rating;

import com.llamaexpressbackend.order.DeliveryOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ratings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private DeliveryOrder order;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(nullable = false)
    private int score;

    @Column(length = 500)
    private String review;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Rating create(DeliveryOrder order, Long customerId, Long courierId, int score, String review) {
        Rating r = new Rating();
        r.order = order;
        r.customerId = customerId;
        r.courierId = courierId;
        r.score = score;
        r.review = (review == null || review.isBlank()) ? null : review.trim();
        return r;
    }
}