package com.example.order.application.query.handler;

import com.example.order.application.query.dto.OrderEventHistoryResponse;
import com.example.order.application.query.dto.OrderResponse;
import com.example.order.infrastructure.persistence.entity.EventStoreEntry;
import com.example.order.infrastructure.persistence.entity.OrderDocument;
import com.example.order.infrastructure.persistence.repository.JpaEventStoreRepository;
import com.example.order.infrastructure.persistence.repository.MongoOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderQueryHandler {

    private final MongoOrderRepository mongoRepo;
    private final JpaEventStoreRepository eventStoreRepo;

    public Optional<OrderResponse> findById(String orderId) {
        return mongoRepo.findById(orderId).map(this::toResponse);
    }

    public List<OrderResponse> findByCustomerId(String customerId) {
        return mongoRepo.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                        .toList();
    }

    public List<OrderEventHistoryResponse> findHistoryByOrderId(String orderId) {
        return eventStoreRepo.findByAggregateIdOrderBySequenceNumberAsc(orderId).stream()
                .map(this::toHistoryResponse)
                        .toList();
    }

    private OrderResponse toResponse(OrderDocument doc) {
        List<OrderResponse.OrderItemResponse> items = doc.getItems() == null ? List.of()
                : doc.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .currency(i.getCurrency())
                        .build())
                .toList();
        return OrderResponse.builder()
                .id(doc.getId())
                .customerId(doc.getCustomerId())
                .status(doc.getStatus())
                .totalAmount(doc.getTotalAmount())
                .currency(doc.getCurrency())
                .items(items)
                .cancellationReason(doc.getCancellationReason())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
}

    private OrderEventHistoryResponse toHistoryResponse(EventStoreEntry entry) {
        return OrderEventHistoryResponse.builder()
                .sequenceNumber(entry.getSequenceNumber())
                .eventType(entry.getEventType())
                .eventVersion(entry.getEventVersion())
                .payload(entry.getPayload())
                .occurredOn(entry.getOccurredOn())
                .build();
}
}
