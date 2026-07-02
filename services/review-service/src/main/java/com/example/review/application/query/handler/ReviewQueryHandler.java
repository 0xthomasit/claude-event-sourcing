package com.example.review.application.query.handler;

import com.example.review.application.query.dto.RatingSnapshotResponse;
import com.example.review.application.query.dto.ReviewResponse;
import com.example.review.domain.model.ReviewStatus;
import com.example.review.infrastructure.persistence.repository.JpaProductRatingRepository;
import com.example.review.infrastructure.persistence.repository.JpaReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewQueryHandler {

    private final JpaReviewRepository        reviewRepository;
    private final JpaProductRatingRepository  ratingRepository;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByProduct(String productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public RatingSnapshotResponse getRatingSnapshot(String productId) {
        return ratingRepository.findById(productId)
                .map(RatingSnapshotResponse::from)
                .orElse(RatingSnapshotResponse.builder()
                        .productId(productId)
                        .averageRating(0).totalReviews(0)
                        .build());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByCustomer(String customerId, Pageable pageable) {
        return reviewRepository.findByCustomerId(customerId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public ReviewResponse findById(UUID id) {
        return reviewRepository.findById(id)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findPending(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PENDING, pageable)
                .map(ReviewResponse::from);
    }
}
