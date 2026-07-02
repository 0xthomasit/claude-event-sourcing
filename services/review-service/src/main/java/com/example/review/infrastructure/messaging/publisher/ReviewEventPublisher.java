package com.example.review.infrastructure.messaging.publisher;

import com.example.common.events.KafkaTopics;
import com.example.common.events.review.ReviewApprovedEvent;
import com.example.common.events.review.ReviewSubmittedEvent;
import com.example.review.domain.model.ProductRatingSnapshot;
import com.example.review.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSubmitted(Review review) {
        ReviewSubmittedEvent event = ReviewSubmittedEvent.builder()
                .aggregateId(review.getId().toString())
                .productId(review.getProductId())
                .orderId(review.getOrderId())
                .customerId(review.getCustomerId())
                .rating(review.getRating())
                .title(review.getTitle())
                .occurredOn(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.REVIEW_SUBMITTED, review.getProductId(), event);
        log.debug("Published ReviewSubmittedEvent: product={}", review.getProductId());
    }

    public void publishApproved(Review review, ProductRatingSnapshot snapshot) {
        ReviewApprovedEvent event = ReviewApprovedEvent.builder()
                .aggregateId(review.getId().toString())
                .productId(review.getProductId())
                .rating(review.getRating())
                .newAverageRating(snapshot.getAverageRating())
                .totalReviews(snapshot.getTotalReviews())
                .occurredOn(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.REVIEW_APPROVED, review.getProductId(), event);
        log.debug("Published ReviewApprovedEvent: product={}, avg={}", review.getProductId(), snapshot.getAverageRating());
    }
}
