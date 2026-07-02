package com.example.review.application.command.handler;

import com.example.review.application.command.dto.SubmitReviewCommand;
import com.example.review.domain.model.OrderDeliveryRecord;
import com.example.review.domain.model.Review;
import com.example.review.infrastructure.messaging.publisher.ReviewEventPublisher;
import com.example.review.infrastructure.persistence.repository.JpaOrderDeliveryRepository;
import com.example.review.infrastructure.persistence.repository.JpaReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles review submission.
 * Validates that the order has been delivered before allowing review.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitReviewHandler {

    private final JpaReviewRepository        reviewRepository;
    private final JpaOrderDeliveryRepository  deliveryRepository;
    private final ReviewEventPublisher        eventPublisher;

    @Transactional
    public Review handle(String customerId, SubmitReviewCommand cmd) {
        // Verify order was delivered
        OrderDeliveryRecord delivery = deliveryRepository.findByOrderId(cmd.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order " + cmd.getOrderId() + " has not been delivered yet"));

        // Verify the customer owns this order
        if (!delivery.getCustomerId().equals(customerId))
            throw new IllegalArgumentException("Order does not belong to this customer");

        // Verify the product was part of this order
        if (!delivery.getProductIds().contains(cmd.getProductId()))
            throw new IllegalArgumentException(
                    "Product " + cmd.getProductId() + " was not in order " + cmd.getOrderId());

        // Check for duplicate review
        if (reviewRepository.existsByOrderIdAndProductId(cmd.getOrderId(), cmd.getProductId()))
            throw new IllegalStateException("Review already submitted for this product in this order");

        // Create and save review
        Review review = Review.submit(
                cmd.getProductId(), cmd.getOrderId(), customerId,
                cmd.getRating(), cmd.getTitle(), cmd.getContent(), cmd.getImageUrls());

        review = reviewRepository.save(review);

        // Publish event
        eventPublisher.publishSubmitted(review);

        log.info("Review submitted: id={}, product={}, rating={}, status=PENDING",
                review.getId(), review.getProductId(), review.getRating());

        return review;
    }
}
