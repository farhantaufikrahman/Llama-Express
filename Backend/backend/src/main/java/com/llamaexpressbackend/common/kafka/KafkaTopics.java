package com.llamaexpressbackend.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import java.time.Duration;

/** Topic dibuat otomatis saat startup oleh KafkaAdmin (bawaan Spring Kafka) */
@Configuration
public class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_STATUS_CHANGED = "order.status-changed";

    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(ORDER_STATUS_CHANGED).partitions(3).replicas(1).build();
    }

    public static final String COURIER_LOCATION = "courier.location";

    @Bean
    NewTopic courierLocationTopic() {
        return TopicBuilder.name(COURIER_LOCATION).partitions(3).replicas(1).build();
    }

    public static final String DLT_SUFFIX = "-dlt";

    @Bean
    KafkaAdmin.NewTopics deadLetterTopics() {
        return new KafkaAdmin.NewTopics(dlt(ORDER_CREATED), dlt(ORDER_STATUS_CHANGED), dlt(COURIER_LOCATION));
    }

    private static NewTopic dlt(String topic) {
        return TopicBuilder.name(topic + DLT_SUFFIX).partitions(1).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}