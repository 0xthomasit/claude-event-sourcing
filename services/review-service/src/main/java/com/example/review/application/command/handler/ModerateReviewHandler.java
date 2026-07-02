package com.example.review.application.command.handler;

import com.example.review.domain.model.ProductRatingSnapshot;
import com.example.review.domain.model.Review;
import com.example.review.infrastructure.messaging.publisher.ReviewEventPublisher;
import com.example.review.infrastructure.persistence.repository.JpaProductRatingRepository;
import com.example.review.infrastructure.persistence.repository.JpaReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin moderation handler — approves or rejects reviews.
 * Updates product rating snapshot on approval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerateReviewHandler {

    private final JpaReviewRepository        reviewRepository;
    private final JpaProductRatingRepository  ratingRepository;
    private final ReviewEventPublisher        eventPublisher;

    @Transactional
    public Review approve(UUID reviewId) {
        Review review = findOrThrow(reviewId);
        review.approve();
        reviewRepository.save(review);

        // Update product rating snapshot
        ProductRatingSnapshot snapshot = ratingRepository.findById(review.getProductId())
                .orElse(ProductRatingSnapshot.builder()
                        .productId(review.getProductId())
                        .build());
        snapshot.addRating(review.getRating());
        ratingRepository.save(snapshot);

        // Publish event
        eventPublisher.publishApproved(review, snapshot);

        log.info("Review approved: id={}, product={}, newAvg={}",
                reviewId, review.getProductId(), snapshot.getAverageRating());

        return review;
    }

    @Transactional
    public Review reject(UUID reviewId, String reason) {
        Review review = findOrThrow(reviewId);
        review.reject(reason);
        reviewRepository.save(review);

        log.info("Review rejected: id={}, reason={}", reviewId, reason);
        return review;
    }

    private Review findOrThrow(UUID id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + id));
    }
}
