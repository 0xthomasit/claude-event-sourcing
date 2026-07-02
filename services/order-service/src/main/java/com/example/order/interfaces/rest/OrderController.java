package com.example.order.interfaces.rest;

import com.example.order.application.command.dto.*;
import com.example.order.application.command.handler.*;
import com.example.order.application.query.dto.OrderEventHistoryResponse;
import com.example.order.application.query.dto.OrderResponse;
import com.example.order.application.query.handler.OrderQueryHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
@Slf4j
@Tag(name = "Orders", description = "Order lifecycle management — CQRS + Event Sourcing")
public class OrderController {

    private final PlaceOrderHandler   placeOrderHandler;
    private final ConfirmOrderHandler confirmOrderHandler;
    private final CancelOrderHandler  cancelOrderHandler;
    private final ShipOrderHandler    shipOrderHandler;
    private final DeliverOrderHandler deliverOrderHandler;
    private final OrderQueryHandler   queryHandler;

    @PostMapping
    @Operation(summary = "Place a new order", description = "Creates a new order in PENDING status")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed — Location header contains URI"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<Void> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {

        log.info("Placing order for customer={} items={}", userId, request.items().size());

        PlaceOrderCommand command = PlaceOrderCommand.builder()
                .customerId(userId)
                .promotionCode(request.promotionCode())
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

        log.info("Order placed: orderId={}", orderId);
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Reads from the MongoDB read model")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        log.debug("Querying order: {}", id);
        return queryHandler.findById(id.toString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List orders by customer", description = "Paginated query on the read model")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of orders")
    })
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomer(
            @Parameter(description = "Customer ID") @RequestParam String customerId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.debug("Listing orders for customer={} page={} size={}", customerId, page, size);
        return ResponseEntity.ok(queryHandler.findByCustomerId(customerId, page, size));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm an order", description = "Transitions PENDING → CONFIRMED after parallel validation")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order confirmed"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    public ResponseEntity<Void> confirmOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Confirming order: orderId={} by={}", id, userId);
        confirmOrderHandler.handle(new ConfirmOrderCommand(id, userId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Only PENDING or CONFIRMED orders can be cancelled")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Cannot cancel in current status")
    })
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Cancelling order: orderId={} reason='{}' by={}", id, request.reason(), userId);
        cancelOrderHandler.handle(new CancelOrderCommand(id, request.reason(), userId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ship")
    @Operation(summary = "Ship an order", description = "Transitions CONFIRMED/PROCESSING → SHIPPED")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order shipped"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Cannot ship in current status")
    })
    public ResponseEntity<Void> shipOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ShipOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Shipping order: orderId={} tracking={} by={}", id, request.trackingNumber(), userId);
        shipOrderHandler.handle(new ShipOrderCommand(id, request.trackingNumber(), userId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deliver")
    @Operation(summary = "Mark order as delivered", description = "Transitions SHIPPED → DELIVERED")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order delivered"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Cannot deliver in current status")
    })
    public ResponseEntity<Void> deliverOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Delivering order: orderId={} by={}", id, userId);
        deliverOrderHandler.handle(new DeliverOrderCommand(id, userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get order event history", description = "Returns raw events from the Event Store for audit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event history")
    })
    public ResponseEntity<List<OrderEventHistoryResponse>> getOrderHistory(
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        log.debug("Querying event history: orderId={}", id);
        return ResponseEntity.ok(queryHandler.findHistoryByOrderId(id.toString()));
    }

    // ─── Request records ──────────────────────────────────────────────────────

    public record PlaceOrderRequest(List<OrderItemRequest> items, String promotionCode) {
        public record OrderItemRequest(
                String productId, String productName,
                int quantity, BigDecimal unitPrice, String currency) {}
    }

    public record CancelOrderRequest(String reason) {}

    public record ShipOrderRequest(String trackingNumber) {}
}
