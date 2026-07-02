package com.example.promotion.application.command.handler;

import com.example.promotion.domain.model.Promotion;
import com.example.promotion.domain.model.PromotionRedemption;
import com.example.promotion.domain.model.RedemptionStatus;
import com.example.promotion.domain.repository.RedemptionRepository;
import com.example.promotion.infrastructure.messaging.publisher.PromotionEventPublisher;
import com.example.promotion.infrastructure.persistence.repository.JpaPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Releases (compensates) all promotion redemptions for a cancelled order.
 * Restores promotion quantity so other customers can use it.
 *
 * <p>Called by Kafka consumer when OrderCancelledEvent is received,
 * or directly during saga compensation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReleasePromotionHandler {

    private final RedemptionRepository    redemptionRepository;
    private final JpaPromotionRepository  promotionJpaRepo;
    private final PromotionEventPublisher eventPublisher;

    @Transactional
    public void handleByOrderId(String orderId) {
        List<PromotionRedemption> redemptions = redemptionRepository
                .findByOrderIdAndStatus(orderId, RedemptionStatus.APPLIED);

        if (redemptions.isEmpty()) {
            log.debug("No active redemptions to release for order {}", orderId);
            return;
        }

        for (PromotionRedemption redemption : redemptions) {
            // Lock promotion row
            Promotion promo = promotionJpaRepo.findByIdForUpdate(redemption.getPromotionId())
                    .orElse(null);

            if (promo != null) {
                promo.release();
                eventPublisher.publishReleased(promo, redemption, "Order cancelled");
            }

            redemption.release();
            redemptionRepository.save(redemption);

            log.info("Released promotion {} for order {}, discount {} restored",
                    redemption.getPromotionCode(), orderId, redemption.getDiscountApplied());
        }
    }
}
