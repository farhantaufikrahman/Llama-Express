package com.llamaexpressbackend.order;

import com.llamaexpressbackend.courier.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("""
            select t from Tariff t
            where t.serviceType = :serviceType
              and t.vehicleType = :vehicleType
              and t.activeFrom <= :now
              and (t.activeTo is null or t.activeTo > :now)
            order by t.activeFrom desc
            """)
    List<Tariff> findActive(@Param("serviceType") ServiceType serviceType,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("now") Instant now);
}