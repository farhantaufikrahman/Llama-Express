package com.llamaexpressbackend.tracking;

import com.llamaexpressbackend.common.kafka.KafkaTopics;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class LocationHistoryListener {

    private final JsonMapper jsonMapper;
    private final CourierLocationRepository locationRepository;
    private final DeliveryOrderRepository orderRepository;

    /** Satu group untuk SEMUA instance → setiap titik disimpan tepat sekali */
    @KafkaListener(topics = KafkaTopics.COURIER_LOCATION, groupId = "llama-location-history")
    @Transactional
    public void onLocation(ConsumerRecord<String, String> record) {
        CourierLocationEvent event = jsonMapper.readValue(record.value(), CourierLocationEvent.class);
        Long orderId = orderRepository.findIdByOrderCode(event.orderCode()).orElse(null);
        locationRepository.save(CourierLocation.from(event, orderId));
    }
}