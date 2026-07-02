package com.example.review.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Review Aggregate Root.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Rating must be 1-5</li>
 *   <li>Content must be 10-2000 characters</li>
 *   <li>One review per product per order (enforced by DB unique constraint)</li>
 *   <li>Can only review products from DELIVERED orders</li>
 *   <li>Reviews start as PENDING — require admin approval before being visible</li>
 * </ul>
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id", "product_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_urls", columnDefinition = "JSONB DEFAULT '[]'")
    private String imageUrls;   // JSON array of URLs

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ─── Domain behaviour ────────────────────────────────────────────────────

    /**
     * Validates and submits a new review.
     */
    public static Review submit(String productId, String orderId, String customerId,
                                 int rating, String title, String content, List<String> imageUrls) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        if (content == null || content.length() < 10)
            throw new IllegalArgumentException("Review content must be at least 10 characters");
        if (content.length() > 2000)
            throw new IllegalArgumentException("Review content must not exceed 2000 characters");

        return Review.builder()
                .productId(productId)
                .orderId(orderId)
                .customerId(customerId)
                .rating(rating)
                .title(title)
                .content(content)
                .imageUrls(imageUrls != null ? imageUrls.toString() : "[]")
                .status(ReviewStatus.PENDING)
                .build();
    }

    /**
     * Admin approves this review — makes it publicly visible.
     */
    public void approve() {
        if (status != ReviewStatus.PENDING)
            throw new IllegalStateException("Can only approve PENDING reviews, current: " + status);
        this.status = ReviewStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    /**
     * Admin rejects this review with a reason.
     */
    public void reject(String reason) {
        if (status != ReviewStatus.PENDING)
            throw new IllegalStateException("Can only reject PENDING reviews, current: " + status);
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Reject reason is required");

        this.status = ReviewStatus.REJECTED;
        this.rejectReason = reason;
        this.updatedAt = Instant.now();
    }
}
