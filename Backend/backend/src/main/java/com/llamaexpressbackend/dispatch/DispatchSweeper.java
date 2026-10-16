package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.order.DeliveryOrderRepository;
import com.llamaexpressbackend.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Jaring pengaman: tawaran basi, order tanpa kurir, atau event Kafka yang
 * terlambat
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dispatch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DispatchSweeper {

    private final DispatchService dispatchService;
    private final DeliveryOrderRepository orderRepository;

    @Scheduled(fixedDelayString = "${app.dispatch.sweep-interval-ms:5000}")
    public void sweep() {
        int expired = dispatchService.expireStaleOffers();
        if (expired > 0) {
            log.info("{} tawaran kedaluwarsa", expired);
        }
        for (String code : orderRepository.findCodesByStatus(OrderStatus.SEARCHING_COURIER)) {
            try {
                dispatchService.dispatchRound(code); // tiap order = transaksi sendiri; satu gagal tidak menggagalkan
                                                     // yang lain
            } catch (Exception e) {
                log.warn("Dispatch {} gagal: {}", code, e.getMessage());
            }
        }
    }
}