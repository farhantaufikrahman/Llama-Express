package com.llamaexpressbackend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * @ServiceConnection: Spring otomatis memakai URL/username/password container
     *                     ini
     *                     untuk DataSource DAN Flyway — tidak perlu menulis
     *                     spring.datasource.* untuk test.
     */
    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0")); // image sama dengan docker-compose
    }

    @Bean
    @ServiceConnection
    OracleContainer oracleContainer() {
        return new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"));
    }

    @Bean
    @ServiceConnection(name = "redis") // GenericContainer → Spring perlu diberi tahu ini Redis lewat "name"
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }
}