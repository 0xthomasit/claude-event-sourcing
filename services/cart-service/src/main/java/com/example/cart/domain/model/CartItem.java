package com.example.cart.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value Object stored as JSON in Redis Hash field.
 * Key:   cart:{userId}
 * Field: {productId}
 * Value: JSON of CartItem
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private String     productId;
    private String     productName;
    private int        quantity;
    private BigDecimal unitPrice;
    private String     currency;
    private String     imageUrl;
    private Instant    addedAt;

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
