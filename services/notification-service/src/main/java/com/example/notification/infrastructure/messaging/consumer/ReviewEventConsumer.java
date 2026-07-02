package com.example.notification.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.review.ReviewApprovedEvent;
import com.example.notification.application.service.NotificationService;
import com.example.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes review events and sends notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.REVIEW_APPROVED, groupId = "notification-service")
    public void onReviewApproved(ReviewApprovedEvent event) {
        log.info("Notification: ReviewApproved product={}, avg={}", 
                event.getProductId(), event.getNewAverageRating());
        try {
            // Notify the reviewer that their review is now published
            notificationService.sendEmail(
                    "system",       // sender = system
                    event.getProductId(),
                    NotificationType.REVIEW_APPROVED,
                    "admin@example.com",    // in real: lookup product seller email
                    "Đánh giá mới cho sản phẩm " + event.getProductId(),
                    String.format(
                            "Một đánh giá mới đã được duyệt cho sản phẩm %s.\n\n" +
                            "Điểm đánh giá: %d/5\n" +
                            "Điểm trung bình mới: %.2f (%d đánh giá)",
                            event.getProductId(),
                            event.getRating(),
                            event.getNewAverageRating(),
                            event.getTotalReviews()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send review notification for product {}: {}",
                    event.getProductId(), e.getMessage(), e);
        }
    }
}
