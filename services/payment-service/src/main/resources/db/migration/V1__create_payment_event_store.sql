-- Payment Event Store — append-only, never UPDATE or DELETE
CREATE TABLE IF NOT EXISTS event_store (
    id               BIGSERIAL    PRIMARY KEY,
    aggregate_id     VARCHAR(36)  NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    event_version    INT          NOT NULL DEFAULT 1,
    sequence_number  BIGINT       NOT NULL,
    payload          JSONB        NOT NULL,
    metadata         JSONB,
    occurred_on      TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_aggregate_sequence
    ON event_store (aggregate_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_aggregate_id
    ON event_store (aggregate_id);

CREATE INDEX IF NOT EXISTS idx_event_type
    ON event_store (event_type);

CREATE INDEX IF NOT EXISTS idx_occurred_on
    ON event_store (occurred_on);
