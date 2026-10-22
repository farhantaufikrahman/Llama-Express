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

    /*
     * Fan-out ke klien WebSocket di SETIAP pod:
     * - groupId acak per instance → setiap pod menerima semua event (broadcast), bukan membagi partisi.
     * - auto.offset.reset=latest → saat start, JANGAN memutar ulang riwayat topic ke browser
     *   (data yang terlewat diambil frontend lewat REST setelah reconnect).
     * - ackMode MANUAL tanpa Acknowledgment → offset TIDAK PERNAH di-commit. Group sekali pakai ini tidak
     *   butuh posisi tersimpan (selalu mulai dari "latest"), dan group tanpa offset tersimpan dibersihkan
     *   Kafka begitu pod mati → daftar consumer group tidak menumpuk setiap restart/deploy.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED,
            groupId = "llama-ws-status-#{T(java.util.UUID).randomUUID()}",
            properties = "auto.offset.reset=latest",
            ackMode = "MANUAL")
    public void onStatusChanged(ConsumerRecord<String, String> record) {
        OrderStatusChangedEvent e = jsonMapper.readValue(record.value(), OrderStatusChangedEvent.class);
        // kirim versi ringkas — tanpa actorId (id internal user)
        String body = jsonMapper
                .writeValueAsString(new StatusMessage(e.orderCode(), e.toStatus(), e.note(), e.occurredAt()));
        messaging.convertAndSend(statusDestination(e.orderCode()), body);
    }

    @KafkaListener(topics = KafkaTopics.COURIER_LOCATION,
            groupId = "llama-ws-location-#{T(java.util.UUID).randomUUID()}",
            properties = "auto.offset.reset=latest",
            ackMode = "MANUAL")
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