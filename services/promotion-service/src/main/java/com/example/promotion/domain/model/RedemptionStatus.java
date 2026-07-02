package com.example.promotion.domain.model;

/**
 * Status of a promotion redemption.
 */
public enum RedemptionStatus {
    APPLIED,     // Coupon applied to order — pending confirmation
    CONFIRMED,   // Order confirmed — discount finalized
    RELEASED     // Order cancelled — discount reversed, quantity restored
}
