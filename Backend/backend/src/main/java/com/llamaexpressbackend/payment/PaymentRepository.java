package com.llamaexpressbackend.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByOrder_IdOrderByIdDesc(Long orderId);

    Optional<Payment> findFirstByOrder_IdAndStatusOrderByIdDesc(Long orderId, PaymentTransactionStatus status);

    long countByOrder_Id(Long orderId);

    /**
     * Webhook & sync bisa datang bersamaan untuk transaksi yang sama → kunci
     * barisnya
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.merchantOrderId = :merchantOrderId")
    Optional<Payment> findForUpdateByMerchantOrderId(@Param("merchantOrderId") String merchantOrderId);
}