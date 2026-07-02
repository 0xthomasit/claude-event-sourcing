package com.example.common.events.promotion;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromotionRedeemedEvent implements DomainEvent {

    private final String aggregateId;   // promotionId
    private final String orderId;
    private final String customerId;
    private final String promotionCode;
    private final BigDecimal discountAmount;
    private final String discountType;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "PROMOTION_REDEEMED";
    }
}
