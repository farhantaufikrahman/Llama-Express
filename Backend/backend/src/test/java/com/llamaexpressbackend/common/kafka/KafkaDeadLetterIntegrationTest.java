package com.llamaexpressbackend.common.kafka;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.fail;

class KafkaDeadLetterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<?, ?> consumerFactory;

    @Test
    void poisonMessage_isMovedToDeadLetterTopic() throws Exception {
        String key = "POISON-" + UUID.randomUUID();
        kafkaTemplate.send(KafkaTopics.ORDER_STATUS_CHANGED, key, "{ini bukan json").get(10, TimeUnit.SECONDS);

        Properties overrides = new Properties();
        overrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<?, ?> consumer = consumerFactory.createConsumer("dlt-test-" + UUID.randomUUID(), null, null,
                overrides)) {
            consumer.subscribe(List.of(KafkaTopics.ORDER_STATUS_CHANGED + KafkaTopics.DLT_SUFFIX));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<?, ?> r : consumer.poll(Duration.ofMillis(500))) {
                    Header group = r.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP);
                    if (key.equals(String.valueOf(r.key())) && group != null
                            && "llama-dispatch".equals(new String(group.value(), StandardCharsets.UTF_8))) {
                        return; // lulus: pesan dari consumer dispatch sampai di DLT
                    }
                }
            }
        }
        fail("Pesan beracun tidak sampai ke dead-letter topic dalam 30 detik");
    }
}