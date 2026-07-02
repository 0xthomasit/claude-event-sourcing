package com.example.cart.application;

import com.example.cart.application.dto.CheckoutResponse;
import com.example.cart.application.service.CartCheckoutService;
import com.example.cart.application.service.CartService;
import com.example.cart.domain.model.CartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartCheckoutService")
class CartCheckoutServiceTest {

    @Mock CartService cartService;
    @Mock RestClient.Builder restClientBuilder;
    @Mock RestClient restClient;
    @Mock RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock RestClient.RequestBodySpec requestBodySpec;
    @Mock RestClient.ResponseSpec responseSpec;

    @Captor ArgumentCaptor<Map<String, Object>> bodyCaptor;

    @InjectMocks CartCheckoutService cartCheckoutService;

    @Test
    @DisplayName("checkout() — creates order then clears cart")
    void checkout_createsOrder_thenClearsCart() {
        String userId = "user-001";
        ReflectionTestUtils.setField(cartCheckoutService, "orderServiceBaseUrl", "http://localhost:8081");

        when(cartService.getCart(userId)).thenReturn(List.of(
                CartItem.builder()
                        .productId("p-1")
                        .productName("Phone")
                        .quantity(2)
                        .unitPrice(new BigDecimal("1000"))
                        .currency("VND")
                        .build()));

        when(restClientBuilder.baseUrl("http://localhost:8081")).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/orders")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("X-User-Id"), eq(userId))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://localhost:8081/api/orders/order-123"));
        when(responseSpec.toBodilessEntity()).thenReturn(new ResponseEntity<>(headers, HttpStatus.CREATED));

        CheckoutResponse response = cartCheckoutService.checkout(userId, null);

        assertThat(response.getOrderId()).isEqualTo("order-123");
        assertThat(response.getStatus()).isEqualTo("CREATED");
        verify(cartService).clearCart(userId);

        verify(requestBodySpec).body(bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).containsKey("items");
    }
}
