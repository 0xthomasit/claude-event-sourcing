package com.example.cart.application.service;

import com.example.cart.domain.model.CartItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cart Service — Redis Hash per user.
 *
 * Key:   cart:{userId}
 * Field: {productId}
 * Value: CartItem JSON
 * TTL:   24 hours (reset on every write)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper                  objectMapper;

    @Value("${cart.ttl-hours:24}")
    private long ttlHours;

    private String cartKey(String userId) {
        return "cart:" + userId;
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    public void addItem(String userId, CartItem item) {
        if (item.getQuantity() <= 0)
            throw new IllegalArgumentException("Quantity must be > 0");

        String key = cartKey(userId);

        // If item already exists, merge quantity
        Object existing = redisTemplate.opsForHash().get(key, item.getProductId());
        if (existing != null) {
            CartItem existingItem = objectMapper.convertValue(existing, CartItem.class);
            item.setQuantity(existingItem.getQuantity() + item.getQuantity());
        } else {
            item.setAddedAt(Instant.now());
        }

        redisTemplate.opsForHash().put(key, item.getProductId(), item);
        resetTtl(key);
        log.debug("Added item to cart: userId={}, productId={}, qty={}",
                userId, item.getProductId(), item.getQuantity());
    }

    public void updateQuantity(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            removeItem(userId, productId);
            return;
        }
        String key = cartKey(userId);
        Object existing = redisTemplate.opsForHash().get(key, productId);
        if (existing == null)
            throw new IllegalArgumentException("Item not found in cart: " + productId);

        CartItem item = objectMapper.convertValue(existing, CartItem.class);
        item.setQuantity(quantity);
        redisTemplate.opsForHash().put(key, productId, item);
        resetTtl(key);
    }

    public void removeItem(String userId, String productId) {
        redisTemplate.opsForHash().delete(cartKey(userId), productId);
        log.debug("Removed item from cart: userId={}, productId={}", userId, productId);
    }

    public void clearCart(String userId) {
        redisTemplate.delete(cartKey(userId));
        log.debug("Cleared cart for userId={}", userId);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public List<CartItem> getCart(String userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey(userId));
        if (entries.isEmpty()) return Collections.emptyList();
        return entries.values().stream()
                .map(v -> objectMapper.convertValue(v, CartItem.class))
                .toList();
    }

    public BigDecimal getTotal(String userId) {
        return getCart(userId).stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getItemCount(String userId) {
        return (int) redisTemplate.opsForHash().size(cartKey(userId));
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void resetTtl(String key) {
        redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
    }
}
