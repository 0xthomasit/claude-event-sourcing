package com.example.review.application.query.dto;

import com.example.review.domain.model.Review;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponse {
    private UUID id;
    private String productId;
    private String orderId;
    private String customerId;
    private int rating;
    private String title;
    private String content;
    private String imageUrls;
    private String status;
    private String rejectReason;
    private Instant createdAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .orderId(r.getOrderId())
                .customerId(r.getCustomerId())
                .rating(r.getRating())
                .title(r.getTitle())
                .content(r.getContent())
                .imageUrls(r.getImageUrls())
                .status(r.getStatus().name())
                .rejectReason(r.getRejectReason())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
