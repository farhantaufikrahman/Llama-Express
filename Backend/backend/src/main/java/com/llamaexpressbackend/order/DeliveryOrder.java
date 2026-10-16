package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.persistance.BaseEntity;
import com.llamaexpressbackend.courier.VehicleType;
import com.llamaexpressbackend.order.dto.CreateOrderRequest;
import com.llamaexpressbackend.order.dto.QuoteResponse;
import com.llamaexpressbackend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String orderCode;

    @Column(nullable = false, length = 64, unique = true)
    private String trackingToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemCategory itemCategory;

    @Column(length = 255)
    private String itemDesc;

    @Column(precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "pickup_address", nullable = false, length = 255))
    @AttributeOverride(name = "lat", column = @Column(name = "pickup_lat", nullable = false, precision = 9, scale = 6))
    @AttributeOverride(name = "lng", column = @Column(name = "pickup_lng", nullable = false, precision = 9, scale = 6))
    private GeoPoint pickup;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "dropoff_address", nullable = false, length = 255))
    @AttributeOverride(name = "lat", column = @Column(name = "dropoff_lat", nullable = false, precision = 9, scale = 6))
    @AttributeOverride(name = "lng", column = @Column(name = "dropoff_lng", nullable = false, precision = 9, scale = 6))
    private GeoPoint dropoff;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String recipientPhone;

    @Column(length = 500)
    private String notes;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceMeters;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false, length = 6)
    private String verificationCode;

    @Version // optimistic locking: UPDATE ... WHERE id = ? AND version = ?
    private Long version;

    private Instant assignedAt;
    private Instant pickedUpAt;
    private Instant deliveredAt;
    private Instant cancelledAt;

    @Column(length = 255)
    private String cancelReason;

    /**
     * Catatan: DTO dipakai langsung agar ringkas. Di domain yang lebih besar,
     * pisahkan ke "command object".
     */
    public static DeliveryOrder place(User customer, CreateOrderRequest req, QuoteResponse quote,
            String orderCode, String trackingToken, String verificationCode) {
        DeliveryOrder o = new DeliveryOrder();
        o.customer = customer;
        o.orderCode = orderCode;
        o.trackingToken = trackingToken;
        o.verificationCode = verificationCode;
        o.serviceType = req.serviceType();
        o.vehicleType = req.vehicleType();
        o.itemCategory = req.itemCategory();
        o.itemDesc = req.itemDesc();
        o.weightKg = req.weightKg();
        o.pickup = GeoPoint.of(req.pickup().address(), req.pickup().lat(), req.pickup().lng());
        o.dropoff = GeoPoint.of(req.dropoff().address(), req.dropoff().lat(), req.dropoff().lng());
        o.recipientName = req.recipientName().trim();
        o.recipientPhone = req.recipientPhone().trim();
        o.notes = req.notes();
        o.paymentMethod = req.paymentMethod();
        o.distanceMeters = quote.distanceMeters();
        o.priceTotal = quote.total();
        return o;
    }

    public void startSearchingCourier() {
        transitionTo(OrderStatus.SEARCHING_COURIER);
    }

    public void cancelByCustomer(String reason, Instant now) {
        if (!status.isCancellableByCustomer()) {
            throw new ConflictException("ORDER_NOT_CANCELLABLE",
                    "Order dengan status " + status + " tidak bisa dibatalkan");
        }
        transitionTo(OrderStatus.CANCELLED);
        this.cancelledAt = now;
        this.cancelReason = reason.trim();
    }

    private void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Status tidak bisa berubah dari " + status + " ke " + next);
        }
        this.status = next;
    }
}