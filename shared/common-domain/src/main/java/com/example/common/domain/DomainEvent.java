package com.example.common.domain;

import java.time.Instant;

/**
 * Marker contract for all domain events across the system.
 */
public interface DomainEvent {

    String getAggregateId();

    Instant getOccurredOn();

    String getEventType();

    default int getEventVersion() {
        return 1;
    }
}
