package com.llamaexpressbackend.common.housekeeping;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class HousekeepingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private HousekeepingJob job;

    @Test
    void purge_deletesOldRows_keepsRecentOnes() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        OffsetDateTime old = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        OffsetDateTime recent = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        insertOutbox("old-" + tag, old);
        insertOutbox("new-" + tag, recent);
        insertProcessed("old-" + tag, old);
        insertProcessed("new-" + tag, recent);

        HousekeepingJob.Result result = job.purge();

        assertThat(result.outboxDeleted()).isPositive();
        assertThat(count("SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?", "old-" + tag)).isZero();
        assertThat(count("SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?", "new-" + tag)).isOne();
        assertThat(count("SELECT COUNT(*) FROM processed_events WHERE event_id = ?", "old-" + tag)).isZero();
        assertThat(count("SELECT COUNT(*) FROM processed_events WHERE event_id = ?", "new-" + tag)).isOne();
    }

    @Test
    void scheduledJobs_areGuardedByShedLock() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (count("SELECT COUNT(*) FROM shedlock WHERE name = ?", "outbox-publisher") == 1) {
                return; // OutboxPublisher sudah berjalan lewat kunci ShedLock
            }
            Thread.sleep(200);
        }
        fail("Baris kunci 'outbox-publisher' tidak muncul di tabel shedlock");
    }

    private void insertOutbox(String aggregateId, OffsetDateTime at) {
        jdbc.update("""
                INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, created_at, published_at)
                VALUES ('Test', ?, 'test.event', '{}', ?, ?)
                """, aggregateId, at, at);
    }

    private void insertProcessed(String eventId, OffsetDateTime at) {
        jdbc.update("INSERT INTO processed_events (consumer, event_id, processed_at) VALUES ('test', ?, ?)", eventId,
                at);
    }

    private int count(String sql, String param) {
        Integer n = jdbc.queryForObject(sql, Integer.class, param);
        return (n == null) ? 0 : n;
    }
}