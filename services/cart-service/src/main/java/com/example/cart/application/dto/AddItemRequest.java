package com.example.cart.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AddItemRequest {

    @NotBlank
    String productId;

    @NotBlank
    String productName;

    @Min(1)
    int quantity;

    @NotNull
    @DecimalMin("0.01")
    BigDecimal unitPrice;

    @NotBlank
    String currency;

    String imageUrl;
}
