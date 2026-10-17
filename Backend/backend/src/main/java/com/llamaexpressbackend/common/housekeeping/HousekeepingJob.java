package com.llamaexpressbackend.common.housekeeping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.housekeeping", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HousekeepingJob {

    private final JdbcTemplate jdbc;
    private final HousekeepingProperties props;
    private final Clock clock;

    /**
     * lockAtLeastFor 1 menit: kalau jam antar-pod sedikit beda, job harian tidak
     * jalan dua kali
     */
    @Scheduled(cron = "${app.housekeeping.cron:0 0 3 * * *}", zone = "${app.housekeeping.zone:Asia/Jakarta}")
    @SchedulerLock(name = "housekeeping", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void run() {
        Result r = purge();
        log.info("Housekeeping selesai: {} outbox event & {} processed event dihapus",
                r.outboxDeleted(), r.processedEventsDeleted());
    }

    /**
     * Dipisah dari run() supaya bisa dipanggil langsung dari test tanpa melewati
     * kunci
     */
    public Result purge() {
        Instant now = clock.instant();
        int outbox = deleteInBatches(
                "DELETE FROM outbox_events WHERE published_at < ? AND ROWNUM <= ?",
                now.minus(props.outboxRetention()));
        int processed = deleteInBatches(
                "DELETE FROM processed_events WHERE processed_at < ? AND ROWNUM <= ?",
                now.minus(props.processedEventsRetention()));
        return new Result(outbox, processed);
    }

    /**
     * Hapus per batch, setiap batch commit sendiri (JdbcTemplate
     * tanpa @Transactional = autocommit).
     * Satu DELETE raksasa akan menahan lock lama dan membengkakkan undo log Oracle.
     * Hanya baris yang SUDAH terkirim (published_at terisi) yang dihapus — event
     * yang belum terkirim aman.
     */
    private int deleteInBatches(String sql, Instant cutoff) {
        OffsetDateTime ts = cutoff.atOffset(ZoneOffset.UTC);
        int total = 0;
        int deleted;
        do {
            deleted = jdbc.update(sql, ts, props.batchSize());
            total += deleted;
        } while (deleted == props.batchSize());
        return total;
    }

    public record Result(int outboxDeleted, int processedEventsDeleted) {
    }
}