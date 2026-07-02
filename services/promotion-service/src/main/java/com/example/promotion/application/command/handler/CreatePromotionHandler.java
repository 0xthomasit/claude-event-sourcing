package com.example.promotion.application.command.handler;

import com.example.promotion.application.command.dto.CreatePromotionCommand;
import com.example.promotion.application.query.dto.PromotionResponse;
import com.example.promotion.domain.model.*;
import com.example.promotion.domain.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePromotionHandler {

    private final PromotionRepository promotionRepository;

    @Transactional
    public PromotionResponse handle(CreatePromotionCommand cmd) {
        if (promotionRepository.existsByCode(cmd.getCode().toUpperCase()))
            throw new IllegalArgumentException("Promotion code already exists: " + cmd.getCode());

        if (cmd.getEndDate().isBefore(cmd.getStartDate()))
            throw new IllegalArgumentException("End date must be after start date");

        Promotion promotion = promotionRepository.save(Promotion.builder()
                .code(cmd.getCode().toUpperCase())
                .name(cmd.getName())
                .description(cmd.getDescription())
                .promotionType(PromotionType.valueOf(cmd.getPromotionType()))
                .discountType(DiscountType.valueOf(cmd.getDiscountType()))
                .discountValue(cmd.getDiscountValue())
                .minOrderAmount(cmd.getMinOrderAmount() != null ? cmd.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(cmd.getMaxDiscountAmount())
                .totalQuantity(cmd.getTotalQuantity())
                .maxPerCustomer(cmd.getMaxPerCustomer() != null ? cmd.getMaxPerCustomer() : 1)
                .startDate(cmd.getStartDate())
                .endDate(cmd.getEndDate())
                .status(PromotionStatus.ACTIVE)
                .build());

        log.info("Promotion created: code={}, type={}, discount={}",
                promotion.getCode(), promotion.getPromotionType(), promotion.getDiscountValue());

        return PromotionResponse.from(promotion);
    }
}
