package com.example.promotion.application.command.handler;

import com.example.promotion.application.command.dto.ValidatePromotionCommand;
import com.example.promotion.application.query.dto.DiscountResult;
import com.example.promotion.domain.model.Promotion;
import com.example.promotion.domain.repository.PromotionRepository;
import com.example.promotion.domain.repository.RedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates a promotion code against an order — does NOT redeem.
 * Idempotent: safe to call multiple times for price preview.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidatePromotionHandler {

    private final PromotionRepository promotionRepository;
    private final RedemptionRepository redemptionRepository;

    @Transactional(readOnly = true)
    public DiscountResult handle(ValidatePromotionCommand cmd) {
        var promoOpt = promotionRepository.findByCode(cmd.getCode().toUpperCase());
        if (promoOpt.isEmpty()) {
            return DiscountResult.invalid("Promotion code not found: " + cmd.getCode());
        }

        Promotion promo = promoOpt.get();
        Promotion.ValidationResult result = promo.validate(cmd.getOrderAmount());

        if (!result.valid()) {
            return DiscountResult.invalid(result.reason());
        }

        // Check per-customer limit
        long customerUses = redemptionRepository.countByCustomerIdAndPromotionId(
                cmd.getCustomerId(), promo.getId());
        if (customerUses >= promo.getMaxPerCustomer()) {
            return DiscountResult.invalid(
                    "Customer has already used this promotion " + customerUses + " time(s)");
        }

        log.debug("Promotion {} validated for order {}: discount={}",
                cmd.getCode(), cmd.getOrderId(), result.discountAmount());

        return DiscountResult.valid(
                promo.getCode(),
                result.discountAmount(),
                promo.getDiscountType().name());
    }
}
