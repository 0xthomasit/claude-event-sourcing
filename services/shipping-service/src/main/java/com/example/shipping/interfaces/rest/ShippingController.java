package com.example.shipping.interfaces.rest;

import com.example.shipping.application.service.ShippingService;
import com.example.shipping.domain.model.Shipment;
import com.example.shipping.domain.model.ShipmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Shipment tracking and management")
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get shipment by order ID")
    public ResponseEntity<Shipment> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(shippingService.getByOrderId(orderId));
    }

    @GetMapping
    @Operation(summary = "Get all shipments for a customer")
    public ResponseEntity<List<Shipment>> getByCustomer(@RequestParam String customerId) {
        return ResponseEntity.ok(shippingService.getByCustomerId(customerId));
    }

    @PostMapping("/order/{orderId}/tracking")
    @Operation(summary = "Update shipment tracking (webhook from GHN/GHTK)")
    public ResponseEntity<Shipment> updateTracking(
            @PathVariable String orderId,
            @RequestParam String trackingCode,
            @RequestParam ShipmentStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(
                shippingService.updateTracking(orderId, trackingCode, status, location, description));
    }
}
