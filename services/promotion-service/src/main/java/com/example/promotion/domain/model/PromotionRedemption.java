package com.example.promotion.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a single use of a promotion for an order.
 * Supports saga lifecycle: APPLIED → CONFIRMED or RELEASED.
 */
@Entity
@Table(name = "promotion_redemptions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    @Column(name = "promotion_code", nullable = false, length = 50)
    private String promotionCode;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "discount_applied", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RedemptionStatus status = RedemptionStatus.APPLIED;

    @Column(name = "redeemed_at", nullable = false)
    @Builder.Default
    private Instant redeemedAt = Instant.now();

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    // ─── Domain behaviour ────────────────────────────────────────────────────

    public void confirm() {
        if (status != RedemptionStatus.APPLIED)
            throw new IllegalStateException("Cannot confirm redemption in status: " + status);
        this.status = RedemptionStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void release() {
        if (status != RedemptionStatus.APPLIED)
            throw new IllegalStateException("Cannot release redemption in status: " + status);
        this.status = RedemptionStatus.RELEASED;
        this.releasedAt = Instant.now();
    }
}
