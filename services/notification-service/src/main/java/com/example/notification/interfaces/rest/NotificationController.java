package com.example.notification.interfaces.rest;

import com.example.notification.infrastructure.persistence.entity.NotificationLog;
import com.example.notification.infrastructure.persistence.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification log queries")
public class NotificationController {

    private final NotificationLogRepository logRepository;

    @GetMapping
    @Operation(summary = "Get notifications for a customer")
    public ResponseEntity<List<NotificationLog>> getByCustomer(
            @RequestParam String customerId) {
        return ResponseEntity.ok(
                logRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get notifications for an order")
    public ResponseEntity<List<NotificationLog>> getByOrder(
            @PathVariable String orderId) {
        return ResponseEntity.ok(logRepository.findByOrderId(orderId));
    }
}
