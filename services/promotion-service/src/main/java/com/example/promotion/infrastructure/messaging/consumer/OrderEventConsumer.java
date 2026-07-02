package com.example.promotion.infrastructure.messaging.consumer;

import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.common.events.KafkaTopics;
import com.example.promotion.application.command.handler.ReleasePromotionHandler;
import com.example.promotion.domain.model.PromotionRedemption;
import com.example.promotion.domain.model.RedemptionStatus;
import com.example.promotion.domain.repository.RedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listens to order lifecycle events to manage promotion redemption status:
 * <ul>
 *   <li>OrderConfirmed → confirm redemptions (discount is finalized)</li>
 *   <li>OrderCancelled → release redemptions (restore promotion quantity)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ReleasePromotionHandler releaseHandler;
    private final RedemptionRepository    redemptionRepository;

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = "promotion-service")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("OrderConfirmed → confirming promotions for order {}", event.getAggregateId());
        try {
            List<PromotionRedemption> redemptions = redemptionRepository
                    .findByOrderIdAndStatus(event.getAggregateId(), RedemptionStatus.APPLIED);
            for (PromotionRedemption r : redemptions) {
                r.confirm();
                redemptionRepository.save(r);
            }
            log.info("Confirmed {} promotion redemptions for order {}",
                    redemptions.size(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to confirm promotions for order {}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "promotion-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("OrderCancelled → releasing promotions for order {}", event.getAggregateId());
        try {
            releaseHandler.handleByOrderId(event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to release promotions for order {}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }
}
