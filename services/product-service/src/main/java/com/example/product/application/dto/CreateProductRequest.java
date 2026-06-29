package com.example.product.application.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@Jacksonized
public class CreateProductRequest {

    @NotBlank
    @Size(max = 50)
    private final String sku;

    @NotBlank
    @Size(max = 255)
    private final String name;

    private final String description;

    @NotNull
    @DecimalMin("0.01")
    private final BigDecimal price;

    @NotBlank
    @Size(min = 3, max = 3)
    private final String currency;

    private final UUID categoryId;

    private final String imageUrl;
}