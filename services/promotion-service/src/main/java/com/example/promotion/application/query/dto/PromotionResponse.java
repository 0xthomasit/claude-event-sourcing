package com.example.promotion.application.query.dto;

import com.example.promotion.domain.model.Promotion;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String promotionType;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private int totalQuantity;
    private int usedQuantity;
    private int remainingQuantity;
    private int maxPerCustomer;
    private Instant startDate;
    private Instant endDate;
    private String status;
    private Instant createdAt;

    public static PromotionResponse from(Promotion p) {
        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .promotionType(p.getPromotionType().name())
                .discountType(p.getDiscountType().name())
                .discountValue(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .totalQuantity(p.getTotalQuantity())
                .usedQuantity(p.getUsedQuantity())
                .remainingQuantity(p.getRemainingQuantity())
                .maxPerCustomer(p.getMaxPerCustomer())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
