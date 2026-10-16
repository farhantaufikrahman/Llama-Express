package com.llamaexpressbackend.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final long SEND_TIMEOUT_MS = 10_000;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = repository.findTop100ByPublishedAtIsNullOrderByIdAsc();
        for (OutboxEvent event : batch) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(event.getEventType(),
                        event.getAggregateId(), event.getPayload());
                record.headers().add("eventId", String.valueOf(event.getId()).getBytes(StandardCharsets.UTF_8));
                record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

                kafkaTemplate.send(record).get(SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS); // tunggu ack broker
                event.markPublished(clock.instant());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException ex) {
                // Berhenti di event pertama yang gagal → urutan event tetap terjaga. Dicoba
                // lagi di siklus berikutnya.
                log.warn("Gagal publish outbox event id={} type={}: {}", event.getId(), event.getEventType(),
                        ex.getMessage());
                return;
            }
        }
    }
}