package com.example.promotion.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Promotion Aggregate Root.
 *
 * <p>Encapsulates all business rules for coupon validation, discount calculation,
 * flash sale quantity management, and redemption lifecycle.
 *
 * <p>Key invariants:
 * <ul>
 *   <li>Cannot redeem an expired, exhausted, or inactive promotion</li>
 *   <li>Discount never exceeds maxDiscountAmount (if set)</li>
 *   <li>Flash sales enforce totalQuantity with pessimistic lock</li>
 *   <li>Each customer limited to maxPerCustomer redemptions</li>
 * </ul>
 */
@Entity
@Table(name = "promotions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 20)
    private PromotionType promotionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "used_quantity", nullable = false)
    @Builder.Default
    private int usedQuantity = 0;

    @Column(name = "max_per_customer")
    @Builder.Default
    private int maxPerCustomer = 1;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ─── Domain behaviour ────────────────────────────────────────────────────

    /**
     * Validates whether this promotion can be applied to an order.
     *
     * @param orderAmount the total order amount before discount
     * @return validation result with reason if invalid
     */
    public ValidationResult validate(BigDecimal orderAmount) {
        Instant now = Instant.now();

        if (status != PromotionStatus.ACTIVE)
            return ValidationResult.invalid("Promotion is not active (status: " + status + ")");
        if (now.isBefore(startDate))
            return ValidationResult.invalid("Promotion has not started yet");
        if (now.isAfter(endDate))
            return ValidationResult.invalid("Promotion has expired");
        if (usedQuantity >= totalQuantity)
            return ValidationResult.invalid("Promotion is fully redeemed");
        if (orderAmount.compareTo(minOrderAmount) < 0)
            return ValidationResult.invalid(
                    "Order amount " + orderAmount + " is below minimum " + minOrderAmount);

        return ValidationResult.valid(calculateDiscount(orderAmount));
    }

    /**
     * Calculates the effective discount for a given order amount.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        BigDecimal discount;

        switch (discountType) {
            case PERCENTAGE -> {
                discount = orderAmount.multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            }
            case FIXED_AMOUNT -> discount = discountValue;
            case FREE_SHIPPING -> discount = BigDecimal.ZERO; // handled at shipping level
            default -> discount = BigDecimal.ZERO;
        }

        // Cap at max discount if configured
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        // Discount cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    /**
     * Redeems this promotion — decrements available quantity.
     * Must be called within a transaction holding a pessimistic lock.
     */
    public void redeem() {
        if (usedQuantity >= totalQuantity)
            throw new IllegalStateException("Promotion " + code + " is fully redeemed");

        this.usedQuantity++;
        this.updatedAt = Instant.now();

        if (usedQuantity >= totalQuantity) {
            this.status = PromotionStatus.EXHAUSTED;
        }
    }

    /**
     * Releases a redemption — increments available quantity back.
     * Used for saga compensation when order is cancelled.
     */
    public void release() {
        if (usedQuantity <= 0)
            throw new IllegalStateException("Cannot release — no redemptions to reverse");

        this.usedQuantity--;
        this.updatedAt = Instant.now();

        // Reactivate if was exhausted
        if (status == PromotionStatus.EXHAUSTED && Instant.now().isBefore(endDate)) {
            this.status = PromotionStatus.ACTIVE;
        }
    }

    /**
     * Deactivates this promotion.
     */
    public void deactivate() {
        this.status = PromotionStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks remaining quantity for flash sale display.
     */
    public int getRemainingQuantity() {
        return Math.max(0, totalQuantity - usedQuantity);
    }

    public boolean isFlashSale() {
        return promotionType == PromotionType.FLASH_SALE;
    }

    // ─── Validation Result ───────────────────────────────────────────────────

    public record ValidationResult(boolean valid, BigDecimal discountAmount, String reason) {
        public static ValidationResult valid(BigDecimal discountAmount) {
            return new ValidationResult(true, discountAmount, null);
        }
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, BigDecimal.ZERO, reason);
        }
    }
}
