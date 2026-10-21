package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.common.kafka.KafkaTopics;
import com.llamaexpressbackend.order.OrderStatus;
import com.llamaexpressbackend.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OrderDispatchListener {

    static final String CONSUMER = "dispatch";

    private final JsonMapper jsonMapper;
    private final DispatchService dispatchService;

    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "llama-dispatch")
    public void onStatusChanged(ConsumerRecord<String, String> record) {
        OrderStatusChangedEvent event = jsonMapper.readValue(record.value(), OrderStatusChangedEvent.class);
        if (event.toStatus() != OrderStatus.SEARCHING_COURIER) {
            return; // listener ini hanya peduli order yang mulai mencari kurir
        }
        dispatchService.dispatchFromEvent(CONSUMER, eventId(record), event.orderCode());
    }

    private static String eventId(ConsumerRecord<String, String> record) {
        Header h = record.headers().lastHeader("eventId");
        return (h != null)
                ? new String(h.value(), StandardCharsets.UTF_8)
                : record.topic() + "-" + record.partition() + "-" + record.offset(); // fallback unik per message
    }
}