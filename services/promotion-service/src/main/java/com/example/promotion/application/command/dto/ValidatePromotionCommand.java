package com.example.promotion.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValidatePromotionCommand {
    @NotBlank private String code;
    @NotBlank private String orderId;
    @NotBlank private String customerId;
    @NotNull @Positive private BigDecimal orderAmount;
    @NotBlank private String currency;
}
