package com.example.order.application.query.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * One raw event from the Event Store — exposed for audit / history endpoint.
 */
@Getter
@Builder
public class OrderEventHistoryResponse {
    private final long    sequenceNumber;
    private final String  eventType;
    private final int     eventVersion;
    private final String  payload;       // raw JSON
    private final Instant occurredOn;
}
