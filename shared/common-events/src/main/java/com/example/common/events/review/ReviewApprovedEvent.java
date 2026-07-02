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
public class ReviewApprovedEvent implements DomainEvent {

    private final String aggregateId;   // reviewId
    private final String productId;
    private final int rating;
    private final double newAverageRating;
    private final int totalReviews;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "REVIEW_APPROVED";
    }
}
