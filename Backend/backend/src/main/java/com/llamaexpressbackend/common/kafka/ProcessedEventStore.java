package com.llamaexpressbackend.common.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcessedEventStore {

    private final JdbcTemplate jdbc;

    /**
     * @return true kalau event ini BARU (berhasil dicatat), false kalau sudah
     *         pernah diproses.
     *         MANDATORY: harus satu transaksi dengan pemrosesannya — kalau proses
     *         gagal, catatan ini ikut di-rollback
     *         sehingga event akan diproses ulang saat Kafka mengirimnya kembali.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String consumer, String eventId) {
        try {
            jdbc.update("INSERT INTO processed_events (consumer, event_id) VALUES (?, ?)", consumer, eventId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}