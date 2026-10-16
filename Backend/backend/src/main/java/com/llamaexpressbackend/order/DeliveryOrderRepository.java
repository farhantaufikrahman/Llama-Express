package com.llamaexpressbackend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    Optional<DeliveryOrder> findByOrderCodeAndCustomer_Id(String orderCode, Long customerId);

    Optional<DeliveryOrder> findByOrderCode(String orderCode);

    boolean existsByCourier_IdAndStatusIn(Long courierId, Collection<OrderStatus> statuses);

    Page<DeliveryOrder> findByCustomer_Id(Long customerId, Pageable pageable);

    Page<DeliveryOrder> findByCustomer_IdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    @Query("select o.orderCode from DeliveryOrder o where o.status = :status")
    List<String> findCodesByStatus(@Param("status") OrderStatus status);
}