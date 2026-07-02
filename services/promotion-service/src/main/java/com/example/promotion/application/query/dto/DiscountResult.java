package com.example.promotion.application.query.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscountResult {
    private boolean valid;
    private String promotionCode;
    private BigDecimal discountAmount;
    private String discountType;
    private String reason;

    public static DiscountResult valid(String code, BigDecimal amount, String type) {
        return DiscountResult.builder()
                .valid(true).promotionCode(code)
                .discountAmount(amount).discountType(type)
                .build();
    }

    public static DiscountResult invalid(String reason) {
        return DiscountResult.builder()
                .valid(false).discountAmount(BigDecimal.ZERO).reason(reason)
                .build();
    }
}
