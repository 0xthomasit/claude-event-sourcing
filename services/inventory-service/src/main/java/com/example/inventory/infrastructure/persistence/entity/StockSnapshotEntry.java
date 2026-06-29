package com.example.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Snapshot of StockItem state — taken every SNAPSHOT_THRESHOLD events.
 * On load: restore from snapshot, then replay only events AFTER snapshot's sequence_number.
 * This bounds replay cost to at most SNAPSHOT_THRESHOLD events.
 */
@Entity
@Table(name = "stock_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockSnapshotEntry {

    @Id
    @Column(name = "aggregate_id", length = 36)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "jsonb")
    private String snapshotData;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}