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

CREATE UNIQUE INDEX IF NOT EXISTS uq_aggregate_sequence
    ON event_store (aggregate_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_id
    ON event_store (aggregate_id);

CREATE INDEX IF NOT EXISTS idx_event_store_event_type
    ON event_store (event_type);

-- Snapshot table — bounds event replay to SNAPSHOT_THRESHOLD events
CREATE TABLE IF NOT EXISTS stock_snapshots (
    aggregate_id     VARCHAR(36)  PRIMARY KEY,
    aggregate_type   VARCHAR(100) NOT NULL,
    snapshot_data    JSONB        NOT NULL,
    sequence_number  BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL
);