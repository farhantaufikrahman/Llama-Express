package com.llamaexpressbackend.common.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final JsonMapper jsonMapper; // Boot 4 = Jackson 3 (package tools.jackson)

    /**
     * MANDATORY: wajib dipanggil di dalam transaksi yang sudah berjalan.
     * Kalau dipanggil di luar transaksi → langsung error, karena outbox tanpa
     * transaksi bersama = tidak ada gunanya.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String aggregateType, String aggregateId, String eventType, Object payload) {
        repository.save(OutboxEvent.of(aggregateType, aggregateId, eventType, jsonMapper.writeValueAsString(payload)));
    }
}