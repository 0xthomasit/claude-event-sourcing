package com.example.product.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@Jacksonized
public class UpdateProductRequest {

    @Size(max = 255)
    private final String name;

    private final String description;

    @DecimalMin("0.01")
    private final BigDecimal price;

    private final UUID categoryId;

    private final String imageUrl;
}