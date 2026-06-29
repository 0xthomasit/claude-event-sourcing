package com.example.notification.application.service;

import com.example.notification.domain.model.NotificationChannel;
import com.example.notification.domain.model.NotificationStatus;
import com.example.notification.domain.model.NotificationType;
import com.example.notification.infrastructure.persistence.entity.NotificationLog;
import com.example.notification.infrastructure.persistence.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Core notification service — logs every notification attempt and delegates sending.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final EmailSenderService        emailSender;

    @Transactional
    public void sendEmail(String customerId, String orderId,
                          NotificationType type, String recipient,
                          String subject, String body) {

        // Idempotency — don't send duplicate notification for same order+type
        if (orderId != null && logRepository.existsByOrderIdAndType(orderId, type)) {
            log.warn("Duplicate notification skipped: orderId={}, type={}", orderId, type);
            return;
        }

        NotificationLog entry = NotificationLog.builder()
                .customerId(customerId)
                .orderId(orderId)
                .channel(NotificationChannel.EMAIL)
                .type(type)
                .status(NotificationStatus.PENDING)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .build();
        logRepository.save(entry);

        try {
            emailSender.send(recipient, subject, body);
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
        } catch (Exception e) {
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMsg(e.getMessage());
            entry.setRetryCount(entry.getRetryCount() + 1);
            log.error("Notification failed: type={}, recipient={}", type, recipient);
        } finally {
            logRepository.save(entry);
        }
    }
}
