package com.example.inventory.interfaces.rest;

import com.example.inventory.application.command.dto.AdjustStockCommand;
import com.example.inventory.application.command.dto.ReplenishStockCommand;
import com.example.inventory.application.command.handler.AdjustStockHandler;
import com.example.inventory.application.command.handler.ReplenishStockHandler;
import com.example.inventory.application.query.dto.StockLevelResponse;
import com.example.inventory.application.query.handler.StockQueryHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock level management")
public class InventoryController {

    private final ReplenishStockHandler replenishHandler;
    private final AdjustStockHandler    adjustHandler;
    private final StockQueryHandler     queryHandler;

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get stock level for a product")
    public ResponseEntity<StockLevelResponse> getStockLevel(
            @PathVariable String productId) {
        return queryHandler.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/{productId}/available")
    @Operation(summary = "Check if enough stock is available")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String productId,
            @RequestParam int quantity) {
        boolean available = queryHandler.isAvailable(productId, quantity);
        return ResponseEntity.ok(new AvailabilityResponse(productId, quantity, available));
    }

    @PostMapping("/products/{productId}/replenish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replenish stock for a product")
    public ResponseEntity<Void> replenish(
            @PathVariable String productId,
            @Valid @RequestBody ReplenishRequest request) {
        replenishHandler.handle(ReplenishStockCommand.builder()
                .productId(productId)
                .quantity(request.quantity())
                .reference(request.reference())
                .build());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually adjust stock (stocktake, damage, correction)")
    public ResponseEntity<Void> adjust(
            @PathVariable String productId,
            @Valid @RequestBody AdjustRequest request) {
        adjustHandler.handle(AdjustStockCommand.builder()
                .productId(productId)
                .delta(request.delta())
                .reason(request.reason())
                .build());
        return ResponseEntity.noContent().build();
    }

    // ─── Request / Response records ───────────────────────────────────────────

    public record ReplenishRequest(int quantity, String reference) {}
    public record AdjustRequest(int delta, String reason) {}
    public record AvailabilityResponse(String productId, int requestedQty, boolean available) {}
}
