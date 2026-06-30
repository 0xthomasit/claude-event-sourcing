package com.example.order.domain.model;

import com.example.common.domain.ValueObject;
import com.example.common.domain.model.Money;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class OrderItem implements ValueObject {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    private OrderItem(String productId, String productName, int quantity, Money unitPrice) {
        if (productId == null || productId.isBlank())
            throw new IllegalArgumentException("productId must not be blank");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be > 0");
        if (unitPrice == null)
            throw new IllegalArgumentException("unitPrice must not be null");
        this.productId   = productId;
        this.productName = productName;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
    }

    public static OrderItem of(String productId, String productName, int quantity, Money unitPrice) {
        return new OrderItem(productId, productName, quantity, unitPrice);
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem item)) return false;
        return quantity == item.quantity
                && Objects.equals(productId, item.productId)
                && Objects.equals(unitPrice, item.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity, unitPrice);
    }
}
