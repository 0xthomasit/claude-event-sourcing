package com.example.product.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ProductResponse {
    private final UUID id;
    private final String sku;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final String currency;
    private final String status;
    private final String imageUrl;
    private final CategoryInfo category;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Getter
    @Builder
    public static class CategoryInfo {
        private final UUID id;
        private final String name;
        private final String slug;
    }
}
