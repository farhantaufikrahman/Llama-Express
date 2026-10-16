package com.llamaexpressbackend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    Optional<DeliveryOrder> findByOrderCodeAndCustomer_Id(String orderCode, Long customerId);

    Page<DeliveryOrder> findByCustomer_Id(Long customerId, Pageable pageable);

    Page<DeliveryOrder> findByCustomer_IdAndStatus(Long customerId, OrderStatus status, Pageable pageable);
}