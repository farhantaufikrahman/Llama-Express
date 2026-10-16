package com.llamaexpressbackend.common.kafka;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventStoreTest extends AbstractIntegrationTest {

    @Autowired
    ProcessedEventStore store;
    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void sameEvent_isAcceptedOnlyOnce() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        String eventId = UUID.randomUUID().toString();

        Boolean first = tx.execute(s -> store.markProcessed("test", eventId));
        Boolean duplicate = tx.execute(s -> store.markProcessed("test", eventId));
        Boolean otherConsumer = tx.execute(s -> store.markProcessed("other-consumer", eventId));

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse(); // event sama, consumer sama → ditolak
        assertThat(otherConsumer).isTrue(); // consumer lain boleh memproses event yang sama
    }
}