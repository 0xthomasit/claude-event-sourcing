package com.example.cart.application.service;

import com.example.cart.application.dto.CheckoutResponse;
import com.example.cart.domain.model.CartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Converts Redis cart -> PlaceOrder request -> clears cart on success.
 * For now uses HTTP call to order-service. In production this may go via gateway
 * or use service discovery + load-balanced client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartCheckoutService {

    private final CartService cartService;
    private final RestClient.Builder restClientBuilder;

    @Value("${services.order-service.base-url:http://localhost:8081}")
    private String orderServiceBaseUrl;

    public CheckoutResponse checkout(String userId) {
        List<CartItem> items = cartService.getCart(userId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot checkout empty cart for userId=" + userId);
        }

        List<Map<String, Object>> payloadItems = items.stream()
                .map(item -> Map.<String, Object>of(
                        "productId", item.getProductId(),
                        "productName", item.getProductName(),
                        "quantity", item.getQuantity(),
                        "unitPrice", item.getUnitPrice(),
                        "currency", item.getCurrency()))
                .toList();

        RestClient restClient = restClientBuilder.baseUrl(orderServiceBaseUrl).build();
        String location = restClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId)
                .body(Map.of("items", payloadItems))
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getFirst(HttpHeaders.LOCATION);

        cartService.clearCart(userId);
        String orderId = extractOrderId(location);
        log.info("Checkout completed: userId={} orderId={}", userId, orderId);

        return CheckoutResponse.builder()
                .orderId(orderId)
                .status("CREATED")
                .build();
    }

    private String extractOrderId(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        int idx = location.lastIndexOf('/');
        return idx >= 0 ? location.substring(idx + 1) : location;
    }
}
