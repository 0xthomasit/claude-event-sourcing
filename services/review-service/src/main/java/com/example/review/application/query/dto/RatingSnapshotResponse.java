package com.example.review.application.query.dto;

import com.example.review.domain.model.ProductRatingSnapshot;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingSnapshotResponse {
    private String productId;
    private double averageRating;
    private int totalReviews;
    private int rating1;
    private int rating2;
    private int rating3;
    private int rating4;
    private int rating5;

    public static RatingSnapshotResponse from(ProductRatingSnapshot s) {
        return RatingSnapshotResponse.builder()
                .productId(s.getProductId())
                .averageRating(s.getAverageRating())
                .totalReviews(s.getTotalReviews())
                .rating1(s.getRating1())
                .rating2(s.getRating2())
                .rating3(s.getRating3())
                .rating4(s.getRating4())
                .rating5(s.getRating5())
                .build();
    }
}
