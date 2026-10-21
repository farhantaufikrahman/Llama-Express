package com.llamaexpressbackend.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourierLocationRepository extends JpaRepository<CourierLocation, Long> {
    List<CourierLocation> findTop500ByOrderIdOrderByRecordedAtAsc(Long orderId);
}