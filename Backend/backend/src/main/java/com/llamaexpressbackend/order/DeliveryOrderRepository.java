package com.llamaexpressbackend.order;

import com.llamaexpressbackend.admin.dto.DeliveredRow;
import com.llamaexpressbackend.admin.dto.StatusCount;
import com.llamaexpressbackend.courier.earnings.EarningRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long>,
        JpaSpecificationExecutor<DeliveryOrder> {

    // ---------- Customer (langkah 5) ----------
    Optional<DeliveryOrder> findByOrderCodeAndCustomer_Id(String orderCode, Long customerId);

    Page<DeliveryOrder> findByCustomer_Id(Long customerId, Pageable pageable);

    Page<DeliveryOrder> findByCustomer_IdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    // ---------- Dispatch (langkah 6B) ----------
    Optional<DeliveryOrder> findByOrderCode(String orderCode);

    boolean existsByCourier_IdAndStatusIn(Long courierId, Collection<OrderStatus> statuses);

    @Query("select o.orderCode from DeliveryOrder o where o.status = :status")
    List<String> findCodesByStatus(@Param("status") OrderStatus status);

    // ---------- Kurir (langkah 7) ----------
    Optional<DeliveryOrder> findByOrderCodeAndCourier_Id(String orderCode, Long courierId);

    Optional<DeliveryOrder> findFirstByCourier_IdAndStatusIn(Long courierId, Collection<OrderStatus> statuses);

    // ---------- Tracking (langkah 7B) ----------
    Optional<DeliveryOrder> findByTrackingToken(String trackingToken);

    @Query("select o.id from DeliveryOrder o where o.orderCode = :code")
    Optional<Long> findIdByOrderCode(@Param("code") String code);

    /** Apakah user ini customer ATAU kurir dari order tersebut */
    @Query("""
            select case when count(o) > 0 then true else false end
            from DeliveryOrder o
            where o.orderCode = :code and (o.customer.id = :userId or o.courier.id = :userId)
            """)
    boolean isParticipant(@Param("code") String code, @Param("userId") Long userId);

    // ---------- Earnings (langkah 9) ----------
    @Query("""
            select new com.llamaexpressbackend.courier.earnings.EarningRow(o.deliveredAt, o.priceTotal, o.paymentMethod)
            from DeliveryOrder o
            where o.courier.id = :courierId and o.status = :status
              and o.deliveredAt >= :from and o.deliveredAt < :to
            """)
    List<EarningRow> findEarningRows(@Param("courierId") Long courierId, @Param("status") OrderStatus status,
            @Param("from") Instant from, @Param("to") Instant to);

    // ---------- Admin (langkah 10) ----------
    /** Pencarian admin: ambil customer & kurir dalam 1 query (cegah N+1) */
    @Override
    @EntityGraph(attributePaths = { "customer", "courier" })
    Page<DeliveryOrder> findAll(Specification<DeliveryOrder> spec, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant from);

    long countByStatusIn(Collection<OrderStatus> statuses);

    long countByStatus(OrderStatus status);

    @Query("""
            select new com.llamaexpressbackend.admin.dto.StatusCount(o.status, count(o))
            from DeliveryOrder o where o.createdAt >= :from group by o.status
            """)
    List<StatusCount> countByStatusSince(@Param("from") Instant from);

    @Query("""
            select new com.llamaexpressbackend.admin.dto.DeliveredRow(o.createdAt, o.deliveredAt, o.priceTotal)
            from DeliveryOrder o where o.status = :status and o.deliveredAt >= :from
            """)
    List<DeliveredRow> findDeliveredSince(@Param("status") OrderStatus status, @Param("from") Instant from);
}