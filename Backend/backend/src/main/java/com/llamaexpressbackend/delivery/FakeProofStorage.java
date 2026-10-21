package com.llamaexpressbackend.delivery;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "fake")
public class FakeProofStorage implements ProofStorage {

    @PostConstruct
    void warn() {
        log.warn("FakeProofStorage aktif — foto TIDAK benar-benar diunggah (khusus test/dev)");
    }

    @Override
    public StoredFile store(String orderCode, byte[] content, String contentType) {
        return new StoredFile("fake://proofs/" + orderCode + "?bytes=" + content.length, "fake/" + orderCode);
    }
}