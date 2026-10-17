package com.llamaexpressbackend.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {
    Optional<ProofOfDelivery> findByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);
}