package com.example.common.events.review;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewSubmittedEvent implements DomainEvent {

    private final String aggregateId;   // reviewId
    private final String productId;
    private final String orderId;
    private final String customerId;
    private final int rating;
    private final String title;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "REVIEW_SUBMITTED";
    }
}
