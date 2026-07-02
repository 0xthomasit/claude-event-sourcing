package com.example.promotion.infrastructure.messaging.publisher;

import com.example.common.events.KafkaTopics;
import com.example.common.events.promotion.PromotionRedeemedEvent;
import com.example.common.events.promotion.PromotionReleasedEvent;
import com.example.promotion.domain.model.Promotion;
import com.example.promotion.domain.model.PromotionRedemption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRedeemed(Promotion promo, PromotionRedemption redemption) {
        PromotionRedeemedEvent event = PromotionRedeemedEvent.builder()
                .aggregateId(promo.getId().toString())
                .orderId(redemption.getOrderId())
                .customerId(redemption.getCustomerId())
                .promotionCode(promo.getCode())
                .discountAmount(redemption.getDiscountApplied())
                .discountType(promo.getDiscountType().name())
                .occurredOn(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.PROMOTION_REDEEMED, redemption.getOrderId(), event);
        log.debug("Published PromotionRedeemedEvent: code={}, orderId={}", promo.getCode(), redemption.getOrderId());
    }

    public void publishReleased(Promotion promo, PromotionRedemption redemption, String reason) {
        PromotionReleasedEvent event = PromotionReleasedEvent.builder()
                .aggregateId(promo.getId().toString())
                .orderId(redemption.getOrderId())
                .promotionCode(promo.getCode())
                .reason(reason)
                .occurredOn(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.PROMOTION_RELEASED, redemption.getOrderId(), event);
        log.debug("Published PromotionReleasedEvent: code={}, orderId={}", promo.getCode(), redemption.getOrderId());
    }
}
