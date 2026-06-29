package com.example.notification.infrastructure.persistence.repository;

import com.example.notification.domain.model.NotificationStatus;
import com.example.notification.domain.model.NotificationType;
import com.example.notification.infrastructure.persistence.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<NotificationLog> findByOrderId(String orderId);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetry);

    boolean existsByOrderIdAndType(String orderId, NotificationType type);
}
