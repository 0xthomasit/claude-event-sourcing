package com.example.promotion.application.command.handler;

import com.example.promotion.application.command.dto.RedeemPromotionCommand;
import com.example.promotion.application.query.dto.DiscountResult;
import com.example.promotion.domain.model.Promotion;
import com.example.promotion.domain.model.PromotionRedemption;
import com.example.promotion.domain.repository.RedemptionRepository;
import com.example.promotion.infrastructure.messaging.publisher.PromotionEventPublisher;
import com.example.promotion.infrastructure.persistence.repository.JpaPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redeems a promotion for an order — decrements quantity, creates redemption record.
 *
 * <p>Uses pessimistic locking (SELECT FOR UPDATE) on the promotion row
 * to prevent race conditions on flash sale quantity.
 *
 * <p>Idempotent by orderId: if the order already has an APPLIED redemption
 * for this promotion, returns the existing discount without re-decrementing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedeemPromotionHandler {

    private final JpaPromotionRepository promotionJpaRepo;
    private final RedemptionRepository   redemptionRepository;
    private final PromotionEventPublisher eventPublisher;

    @Transactional
    public DiscountResult handle(RedeemPromotionCommand cmd) {
        String code = cmd.getCode().toUpperCase();

        // Idempotency — check if already redeemed for this order
        var existingRedemptions = redemptionRepository.findByOrderId(cmd.getOrderId());
        var alreadyRedeemed = existingRedemptions.stream()
                .filter(r -> r.getPromotionCode().equals(code))
                .filter(r -> r.getStatus() == com.example.promotion.domain.model.RedemptionStatus.APPLIED)
                .findFirst();
        if (alreadyRedeemed.isPresent()) {
            log.info("Promotion {} already redeemed for order {} — returning existing discount",
                    code, cmd.getOrderId());
            return DiscountResult.valid(code,
                    alreadyRedeemed.get().getDiscountApplied(),
                    "EXISTING");
        }

        // Pessimistic lock — critical for flash sale race conditions
        Promotion promo = promotionJpaRepo.findByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + code));

        // Validate
        Promotion.ValidationResult validation = promo.validate(cmd.getOrderAmount());
        if (!validation.valid()) {
            return DiscountResult.invalid(validation.reason());
        }

        // Check per-customer limit
        long customerUses = redemptionRepository.countByCustomerIdAndPromotionId(
                cmd.getCustomerId(), promo.getId());
        if (customerUses >= promo.getMaxPerCustomer()) {
            return DiscountResult.invalid("Customer usage limit reached");
        }

        // Redeem — decrement quantity
        promo.redeem();

        // Create redemption record
        PromotionRedemption redemption = redemptionRepository.save(
                PromotionRedemption.builder()
                        .promotionId(promo.getId())
                        .promotionCode(promo.getCode())
                        .orderId(cmd.getOrderId())
                        .customerId(cmd.getCustomerId())
                        .discountApplied(validation.discountAmount())
                        .build());

        // Publish event
        eventPublisher.publishRedeemed(promo, redemption);

        log.info("Promotion {} redeemed for order {}: discount={}, remaining={}",
                code, cmd.getOrderId(), validation.discountAmount(), promo.getRemainingQuantity());

        return DiscountResult.valid(code, validation.discountAmount(), promo.getDiscountType().name());
    }
}
