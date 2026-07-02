package com.example.review.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Denormalized read model for product rating summary.
 * Updated whenever a review is approved or rejected.
 */
@Entity
@Table(name = "product_rating_snapshots")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProductRatingSnapshot {

    @Id
    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private double averageRating = 0;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private int totalReviews = 0;

    @Column(name = "rating_1", nullable = false)  @Builder.Default private int rating1 = 0;
    @Column(name = "rating_2", nullable = false)  @Builder.Default private int rating2 = 0;
    @Column(name = "rating_3", nullable = false)  @Builder.Default private int rating3 = 0;
    @Column(name = "rating_4", nullable = false)  @Builder.Default private int rating4 = 0;
    @Column(name = "rating_5", nullable = false)  @Builder.Default private int rating5 = 0;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ─── Domain behaviour ────────────────────────────────────────────────────

    public void addRating(int rating) {
        switch (rating) {
            case 1 -> rating1++;
            case 2 -> rating2++;
            case 3 -> rating3++;
            case 4 -> rating4++;
            case 5 -> rating5++;
            default -> throw new IllegalArgumentException("Invalid rating: " + rating);
        }
        totalReviews++;
        recalculateAverage();
        updatedAt = Instant.now();
    }

    public void removeRating(int rating) {
        switch (rating) {
            case 1 -> rating1 = Math.max(0, rating1 - 1);
            case 2 -> rating2 = Math.max(0, rating2 - 1);
            case 3 -> rating3 = Math.max(0, rating3 - 1);
            case 4 -> rating4 = Math.max(0, rating4 - 1);
            case 5 -> rating5 = Math.max(0, rating5 - 1);
            default -> throw new IllegalArgumentException("Invalid rating: " + rating);
        }
        totalReviews = Math.max(0, totalReviews - 1);
        recalculateAverage();
        updatedAt = Instant.now();
    }

    private void recalculateAverage() {
        if (totalReviews == 0) {
            averageRating = 0;
            return;
        }
        double sum = rating1 + (2.0 * rating2) + (3.0 * rating3)
                + (4.0 * rating4) + (5.0 * rating5);
        averageRating = Math.round(sum / totalReviews * 100.0) / 100.0;
    }
}
