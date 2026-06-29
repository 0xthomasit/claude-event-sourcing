package com.example.order.interfaces.rest;

import com.example.order.application.command.dto.CancelOrderCommand;
import com.example.order.application.command.dto.ConfirmOrderCommand;
import com.example.order.application.command.dto.PlaceOrderCommand;
import com.example.order.application.command.handler.CancelOrderHandler;
import com.example.order.application.command.handler.ConfirmOrderHandler;
import com.example.order.application.command.handler.PlaceOrderHandler;
import com.example.order.application.query.dto.OrderEventHistoryResponse;
import com.example.order.application.query.dto.OrderResponse;
import com.example.order.application.query.handler.OrderQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderHandler   placeOrderHandler;
    private final ConfirmOrderHandler confirmOrderHandler;
    private final CancelOrderHandler  cancelOrderHandler;
    private final OrderQueryHandler   queryHandler;

    @PostMapping
    public ResponseEntity<Void> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {

        PlaceOrderCommand command = PlaceOrderCommand.builder()
                .customerId(userId)
                .items(request.items().stream()
                        .map(i -> PlaceOrderCommand.OrderItemRequest.builder()
                                .productId(i.productId())
                                .productName(i.productName())
                                .quantity(i.quantity())
                                .unitPrice(i.unitPrice())
                                .currency(i.currency())
                                .build())
                        .toList())
                .build();

        UUID orderId = placeOrderHandler.handle(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(orderId).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return queryHandler.findById(id.toString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @RequestParam String customerId) {
        return ResponseEntity.ok(queryHandler.findByCustomerId(customerId));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        confirmOrderHandler.handle(new ConfirmOrderCommand(id, userId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {
        cancelOrderHandler.handle(new CancelOrderCommand(id, request.reason(), userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<OrderEventHistoryResponse>> getOrderHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(queryHandler.findHistoryByOrderId(id.toString()));
    }

    // ─── Request records ──────────────────────────────────────────────────────

    public record PlaceOrderRequest(List<OrderItemRequest> items) {
        public record OrderItemRequest(
                String productId, String productName,
                int quantity, BigDecimal unitPrice, String currency) {}
    }

    public record CancelOrderRequest(String reason) {}
}