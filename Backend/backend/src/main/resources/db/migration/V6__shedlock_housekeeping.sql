CREATE TABLE shedlock (
    name        VARCHAR2(64 CHAR)  NOT NULL,
    lock_until  TIMESTAMP(3)       NOT NULL,
    locked_at   TIMESTAMP(3)       NOT NULL,
    locked_by   VARCHAR2(255 CHAR) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
CREATE INDEX ix_processed_events_at ON processed_events (processed_at);