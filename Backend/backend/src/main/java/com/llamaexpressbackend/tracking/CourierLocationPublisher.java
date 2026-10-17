package com.llamaexpressbackend.tracking;

import com.llamaexpressbackend.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourierLocationPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    /**
     * Fire-and-forget: kehilangan 1 titik tidak masalah, titik berikutnya datang 3
     * detik lagi
     */
    public void publish(CourierLocationEvent event) {
        kafkaTemplate.send(KafkaTopics.COURIER_LOCATION, event.orderCode(), jsonMapper.writeValueAsString(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Gagal kirim lokasi order {}: {}", event.orderCode(), ex.getMessage());
                    }
                });
    }
}