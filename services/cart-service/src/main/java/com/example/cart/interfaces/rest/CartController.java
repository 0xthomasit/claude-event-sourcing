package com.example.cart.interfaces.rest;

import com.example.cart.application.dto.AddItemRequest;
import com.example.cart.application.dto.CartResponse;
import com.example.cart.application.dto.CheckoutResponse;
import com.example.cart.application.service.CartCheckoutService;
import com.example.cart.application.service.CartService;
import com.example.cart.domain.model.CartItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart — Redis only, TTL 24h")
public class CartController {

    private final CartService cartService;
    private final CartCheckoutService cartCheckoutService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get cart contents for a user")
    public ResponseEntity<CartResponse> getCart(@PathVariable String userId) {
        List<CartItem> items = cartService.getCart(userId);
        return ResponseEntity.ok(CartResponse.builder()
                .userId(userId)
                .items(items)
                .itemCount(items.size())
                .total(cartService.getTotal(userId))
                .currency("VND")
                .build());
    }

    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to cart (merges quantity if product already exists)")
    public ResponseEntity<Void> addItem(
            @PathVariable String userId,
            @Valid @RequestBody AddItemRequest req) {
        CartItem item = CartItem.builder()
                .productId(req.getProductId())
                .productName(req.getProductName())
                .quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice())
                .currency(req.getCurrency())
                .imageUrl(req.getImageUrl())
                .build();
        cartService.addItem(userId, item);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/items/{productId}")
    @Operation(summary = "Update item quantity (0 = remove)")
    public ResponseEntity<Void> updateQuantity(
            @PathVariable String userId,
            @PathVariable String productId,
            @RequestParam int quantity) {
        cartService.updateQuantity(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @Operation(summary = "Remove a specific item from cart")
    public ResponseEntity<Void> removeItem(
            @PathVariable String userId,
            @PathVariable String productId) {
        cartService.removeItem(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Clear entire cart (called after checkout)")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/count")
    @Operation(summary = "Get number of distinct items in cart")
    public ResponseEntity<Map<String, Integer>> getItemCount(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("count", cartService.getItemCount(userId)));
    }

    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Checkout cart: convert cart to order then clear cart")
    public ResponseEntity<CheckoutResponse> checkout(@PathVariable String userId) {
        return ResponseEntity.ok(cartCheckoutService.checkout(userId));
    }
}
