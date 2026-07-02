package com.example.promotion.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreatePromotionCommand {
    @NotBlank private String code;
    @NotBlank private String name;
    private String description;
    @NotBlank private String promotionType;     // COUPON, VOUCHER, FLASH_SALE, CAMPAIGN
    @NotBlank private String discountType;      // PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING
    @NotNull @Positive private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    @NotNull private Integer totalQuantity;
    private Integer maxPerCustomer;
    @NotNull private Instant startDate;
    @NotNull private Instant endDate;
}
