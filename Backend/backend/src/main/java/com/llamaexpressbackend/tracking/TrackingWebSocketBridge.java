package com.llamaexpressbackend.tracking;

import com.llamaexpressbackend.common.kafka.KafkaTopics;
import com.llamaexpressbackend.order.OrderStatus;
import com.llamaexpressbackend.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TrackingWebSocketBridge {

    private final SimpMessagingTemplate messaging;
    private final JsonMapper jsonMapper;

    /**
     * groupId acak per instance → SETIAP instance menerima semua event (broadcast).
     * auto.offset.reset=latest → saat start, JANGAN memutar ulang seluruh riwayat
     * topic ke browser.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "llama-ws-status-#{T(java.util.UUID).randomUUID()}", properties = "auto.offset.reset=latest")
    public void onStatusChanged(ConsumerRecord<String, String> record) {
        OrderStatusChangedEvent e = jsonMapper.readValue(record.value(), OrderStatusChangedEvent.class);
        // kirim versi ringkas — tanpa actorId (id internal user)
        String body = jsonMapper
                .writeValueAsString(new StatusMessage(e.orderCode(), e.toStatus(), e.note(), e.occurredAt()));
        messaging.convertAndSend(statusDestination(e.orderCode()), body);
    }

    @KafkaListener(topics = KafkaTopics.COURIER_LOCATION, groupId = "llama-ws-location-#{T(java.util.UUID).randomUUID()}", properties = "auto.offset.reset=latest")
    public void onLocation(ConsumerRecord<String, String> record) {
        messaging.convertAndSend(locationDestination(record.key()), record.value()); // JSON diteruskan apa adanya
    }

    public static String statusDestination(String orderCode) {
        return "/topic/orders/" + orderCode + "/status";
    }

    public static String locationDestination(String orderCode) {
        return "/topic/orders/" + orderCode + "/location";
    }

    record StatusMessage(String orderCode, OrderStatus status, String note, Instant at) {
    }
}