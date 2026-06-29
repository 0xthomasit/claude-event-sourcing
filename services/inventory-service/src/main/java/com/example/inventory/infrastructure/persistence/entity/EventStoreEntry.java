package com.example.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "event_store",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_aggregate_sequence",
                columnNames = {"aggregate_id", "sequence_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventStoreEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id",    nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "aggregate_type",  nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type",      nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version",   nullable = false)
    private int eventVersion;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;
}