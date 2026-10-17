package com.llamaexpressbackend.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * Spring Boot otomatis memasang bean CommonErrorHandler ini ke
     * SEMUA @KafkaListener.
     * Partisi -1 = biarkan Kafka memilih partisi DLT (DLT kita hanya punya 1
     * partisi).
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        handler.addNotRetryableExceptions(JacksonException.class); // JSON rusak → langsung DLT
        handler.setRetryListeners((record, ex, attempt) -> log.warn("Retry #{} topic={} key={}: {}", attempt,
                record.topic(), record.key(), ex.getMessage()));
        return handler;
    }
}