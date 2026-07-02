package com.example.review.infrastructure.persistence.repository;

import com.example.review.domain.model.Review;
import com.example.review.domain.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndStatus(String productId, ReviewStatus status, Pageable pageable);
    Page<Review> findByCustomerId(String customerId, Pageable pageable);
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);
    boolean existsByOrderIdAndProductId(String orderId, String productId);
}
