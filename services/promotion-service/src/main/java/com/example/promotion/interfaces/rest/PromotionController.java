package com.example.promotion.interfaces.rest;

import com.example.promotion.application.command.dto.*;
import com.example.promotion.application.command.handler.*;
import com.example.promotion.application.query.dto.*;
import com.example.promotion.application.query.handler.PromotionQueryHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Coupon, voucher, and flash sale management")
public class PromotionController {

    private final CreatePromotionHandler   createHandler;
    private final ValidatePromotionHandler validateHandler;
    private final RedeemPromotionHandler   redeemHandler;
    private final PromotionQueryHandler    queryHandler;

    // ─── Commands ─────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new promotion (ADMIN)")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody CreatePromotionCommand cmd) {
        PromotionResponse response = createHandler.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a promotion code for an order — does NOT redeem")
    public ResponseEntity<DiscountResult> validate(@Valid @RequestBody ValidatePromotionCommand cmd) {
        return ResponseEntity.ok(validateHandler.handle(cmd));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem a promotion for an order — decrements quantity")
    public ResponseEntity<DiscountResult> redeem(@Valid @RequestBody RedeemPromotionCommand cmd) {
        return ResponseEntity.ok(redeemHandler.handle(cmd));
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List active promotions")
    public ResponseEntity<Page<PromotionResponse>> listActive(Pageable pageable) {
        return ResponseEntity.ok(queryHandler.findActive(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by ID")
    public ResponseEntity<PromotionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryHandler.findById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get promotion by code")
    public ResponseEntity<PromotionResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(queryHandler.findByCode(code));
    }
}
