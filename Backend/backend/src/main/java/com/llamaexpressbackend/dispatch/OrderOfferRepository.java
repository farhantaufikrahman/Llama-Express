package com.llamaexpressbackend.dispatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrderOfferRepository extends JpaRepository<OrderOffer, Long> {

        Optional<OrderOffer> findByIdAndCourierId(Long id, Long courierId);

        boolean existsByOrder_IdAndStatusAndExpiresAtAfter(Long orderId, OfferStatus status, Instant now);

        @Query("select o.courierId from OrderOffer o where o.order.id = :orderId")
        Set<Long> findCourierIdsByOrderId(@Param("orderId") Long orderId);

        @Query("""
                        select o from OrderOffer o join fetch o.order ord
                        where o.courierId = :courierId and o.status = :status and o.expiresAt > :now
                          and ord.status = com.llamaexpressbackend.order.OrderStatus.SEARCHING_COURIER
                        order by o.offeredAt desc
                        """)
        List<OrderOffer> findActiveForCourier(@Param("courierId") Long courierId,
                        @Param("status") OfferStatus status,
                        @Param("now") Instant now);

        @Modifying(flushAutomatically = true)
        @Query("""
                        update OrderOffer o set o.status = :expired, o.respondedAt = :now
                        where o.order.id = :orderId and o.id <> :keepId and o.status = :offered
                        """)
        int expireOthers(@Param("orderId") Long orderId, @Param("keepId") Long keepId, @Param("now") Instant now,
                        @Param("offered") OfferStatus offered, @Param("expired") OfferStatus expired);

        @Modifying
        @Query("update OrderOffer o set o.status = :expired where o.status = :offered and o.expiresAt <= :now")
        int expireStale(@Param("now") Instant now,
                        @Param("offered") OfferStatus offered, @Param("expired") OfferStatus expired);
}