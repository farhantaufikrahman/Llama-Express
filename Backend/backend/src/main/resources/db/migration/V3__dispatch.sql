-- Jumlah putaran dispatch yang sudah dicoba untuk sebuah order
ALTER TABLE orders ADD (dispatch_attempts NUMBER(3) DEFAULT 0 NOT NULL);

-- Catatan event yang sudah diproses consumer (idempotency)
CREATE TABLE processed_events (
    consumer      VARCHAR2(50 CHAR) NOT NULL,
    event_id      VARCHAR2(50 CHAR) NOT NULL,
    processed_at  TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (consumer, event_id)
);

-- Untuk cek "masih ada tawaran aktif untuk order ini?"
CREATE INDEX ix_offers_order_status ON order_offers (order_id, status);