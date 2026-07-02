package com.example.promotion.domain.model;

/**
 * Lifecycle status of a promotion.
 */
public enum PromotionStatus {
    DRAFT,      // Created but not yet active
    ACTIVE,     // Available for use
    PAUSED,     // Temporarily suspended
    EXPIRED,    // Past end date
    EXHAUSTED   // All quantity used up
}
