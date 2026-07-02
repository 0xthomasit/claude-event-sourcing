package com.example.promotion.domain.model;

/**
 * How the discount is calculated.
 */
public enum DiscountType {
    PERCENTAGE,     // e.g., 10% off
    FIXED_AMOUNT,   // e.g., 50,000 VND off
    FREE_SHIPPING   // waives shipping fee
}
