package com.example.order.application.query.handler;

import com.example.order.application.query.dto.OrderEventHistoryResponse;
import com.example.order.application.query.dto.OrderResponse;
import com.example.order.infrastructure.persistence.entity.EventStoreEntry;
import com.example.order.infrastructure.persistence.entity.OrderDocument;
import com.example.order.infrastructure.persistence.repository.JpaEventStoreRepository;
import com.example.order.infrastructure.persistence.repository.MongoOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public Page<OrderResponse> findByCustomerId(String customerId, int page, int size) {
        return mongoRepo.findByCustomerId(customerId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
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
