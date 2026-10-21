package com.llamaexpressbackend.order;

import com.jayway.jsonpath.JsonPath;
import com.llamaexpressbackend.common.kafka.KafkaTopics;
import com.llamaexpressbackend.common.outbox.OutboxEventRepository;
import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderEventsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    KafkaContainer kafka;
    @Autowired
    OutboxEventRepository outboxRepository;

    @Test
    void createAndCancel_publishesEventsToKafka_inOrder() throws Exception {
        String token = registerCustomer();

        MvcResult created = mockMvc.perform(post("/api/v1/orders").header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content(orderJson("CASH", "1.5")))
                .andExpect(status().isCreated())
                .andReturn();
        String code = read(created, "$.code");

        mockMvc.perform(post("/api/v1/orders/" + code + "/cancel").header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Berubah pikiran\"}"))
                .andExpect(status().isOk());

        // Harapan: 1 × order.created + 2 × order.status-changed (SEARCHING_COURIER,
        // CANCELLED)
        List<ConsumerRecord<String, String>> events;
        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of(KafkaTopics.ORDER_CREATED, KafkaTopics.ORDER_STATUS_CHANGED));
            events = pollForKey(consumer, code, 3, Duration.ofSeconds(20));
        }
        assertThat(events).hasSize(3);

        var createdEvents = events.stream().filter(r -> r.topic().equals(KafkaTopics.ORDER_CREATED)).toList();
        assertThat(createdEvents).hasSize(1);
        ConsumerRecord<String, String> createdEvent = createdEvents.getFirst();
        assertThat(createdEvent.key()).isEqualTo(code);
        assertThat((String) JsonPath.read(createdEvent.value(), "$.paymentMethod")).isEqualTo("CASH");
        assertThat(header(createdEvent, "eventType")).isEqualTo(KafkaTopics.ORDER_CREATED);
        assertThat(header(createdEvent, "eventId")).isNotBlank();

        // satu key → satu partisi → urutan dijamin
        List<String> transitions = events.stream()
                .filter(r -> r.topic().equals(KafkaTopics.ORDER_STATUS_CHANGED))
                .map(r -> (String) JsonPath.read(r.value(), "$.toStatus"))
                .toList();
        assertThat(transitions).containsExactly("SEARCHING_COURIER", "CANCELLED");

        // semua baris outbox untuk order ini sudah ditandai terkirim
        assertThat(waitUntilAllPublished(code, Duration.ofSeconds(5))).isTrue();
    }

    // ---------- helpers ----------

    private KafkaConsumer<String, String> newConsumer() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(p);
    }

    /**
     * Topic dipakai bersama semua test → ambil hanya message dengan key = kode
     * order kita
     */
    private static List<ConsumerRecord<String, String>> pollForKey(KafkaConsumer<String, String> consumer,
            String key, int expected, Duration timeout) {
        List<ConsumerRecord<String, String>> found = new ArrayList<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (found.size() < expected && System.nanoTime() < deadline) {
            for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                if (key.equals(r.key()))
                    found.add(r);
            }
        }
        return found;
    }

    private boolean waitUntilAllPublished(String aggregateId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (outboxRepository.countByAggregateIdAndPublishedAtIsNull(aggregateId) == 0)
                return true;
            Thread.sleep(200);
        }
        return false;
    }

    private static String header(ConsumerRecord<String, String> r, String name) {
        return new String(r.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}