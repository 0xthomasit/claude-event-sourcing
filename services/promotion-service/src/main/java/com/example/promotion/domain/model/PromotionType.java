package com.example.promotion.domain.model;

/**
 * Type of promotion offering.
 */
public enum PromotionType {
    COUPON,        // Standard coupon code
    VOUCHER,       // Gift voucher with fixed value
    FLASH_SALE,    // Time-limited, quantity-limited discount
    CAMPAIGN       // Marketing campaign (auto-applied)
}
