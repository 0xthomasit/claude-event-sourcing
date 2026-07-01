package com.example.order.interfaces.rest;

import com.example.order.application.command.dto.*;
import com.example.order.application.command.handler.*;
import com.example.order.application.query.dto.OrderEventHistoryResponse;
import com.example.order.application.query.dto.OrderResponse;
import com.example.order.application.query.handler.OrderQueryHandler;
import com.example.common.domain.exception.AggregateNotFoundException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for unit testing
@DisplayName("OrderController API Tests")
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PlaceOrderHandler placeOrderHandler;
    @MockitoBean ConfirmOrderHandler confirmOrderHandler;
    @MockitoBean CancelOrderHandler cancelOrderHandler;
    @MockitoBean ShipOrderHandler shipOrderHandler;
    @MockitoBean DeliverOrderHandler deliverOrderHandler;
    @MockitoBean OrderQueryHandler queryHandler;

    @Nested
    @DisplayName("POST /api/orders")
    class PlaceOrderTests {

        @Test
        @DisplayName("201 Created — valid order")
        void placeOrder_201() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(placeOrderHandler.handle(any())).thenReturn(orderId);

            String body = """
                {
                  "items": [{
                    "productId": "prod-1",
                    "productName": "iPhone 15",
                    "quantity": 2,
                    "unitPrice": 25000000,
                    "currency": "VND"
                  }]
                }
                """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "customer-001")
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));

            verify(placeOrderHandler).handle(any(PlaceOrderCommand.class));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderTests {

        @Test
        @DisplayName("200 OK — order found")
        void getOrder_200() throws Exception {
            UUID id = UUID.randomUUID();
            OrderResponse response = OrderResponse.builder()
                    .id(id.toString())
                    .customerId("customer-001")
                    .status("PENDING")
                    .totalAmount(new BigDecimal("25000000"))
                    .currency("VND")
                    .items(List.of())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(queryHandler.findById(id.toString())).thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/orders/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("404 Not Found — order does not exist")
        void getOrder_404() throws Exception {
            UUID id = UUID.randomUUID();
            when(queryHandler.findById(id.toString())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/orders/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/orders?customerId=...&page=...&size=...")
    class ListOrdersTests {

        @Test
        @DisplayName("200 OK — paginated results")
        void listOrders_paginated() throws Exception {
            OrderResponse response = OrderResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .customerId("customer-001")
                    .status("PENDING")
                    .totalAmount(new BigDecimal("100000"))
                    .currency("VND")
                    .items(List.of())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Page<OrderResponse> page = new PageImpl<>(List.of(response));
            when(queryHandler.findByCustomerId("customer-001", 0, 20)).thenReturn(page);

            mockMvc.perform(get("/api/orders")
                            .param("customerId", "customer-001")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].customerId").value("customer-001"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/orders/{id}/confirm")
    class ConfirmOrderTests {

        @Test
        @DisplayName("204 No Content — successful confirm")
        void confirmOrder_204() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(confirmOrderHandler).handle(any());

            mockMvc.perform(patch("/api/orders/{id}/confirm", id)
                            .header("X-User-Id", "admin"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 Not Found — order not found")
        void confirmOrder_404() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new AggregateNotFoundException("Order not found: " + id))
                    .when(confirmOrderHandler).handle(any());

            mockMvc.perform(patch("/api/orders/{id}/confirm", id)
                            .header("X-User-Id", "admin"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/orders/{id}/cancel")
    class CancelOrderTests {

        @Test
        @DisplayName("204 No Content — successful cancel")
        void cancelOrder_204() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(cancelOrderHandler).handle(any());

            mockMvc.perform(patch("/api/orders/{id}/cancel", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "customer-001")
                            .content("""
                                { "reason": "Changed my mind" }
                                """))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /api/orders/{id}/ship")
    class ShipOrderTests {

        @Test
        @DisplayName("204 No Content — successful ship")
        void shipOrder_204() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(shipOrderHandler).handle(any());

            mockMvc.perform(patch("/api/orders/{id}/ship", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "operator")
                            .content("""
                                { "trackingNumber": "GHN-ABC123" }
                                """))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /api/orders/{id}/deliver")
    class DeliverOrderTests {

        @Test
        @DisplayName("204 No Content — successful deliver")
        void deliverOrder_204() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(deliverOrderHandler).handle(any());

            mockMvc.perform(patch("/api/orders/{id}/deliver", id)
                            .header("X-User-Id", "operator"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{id}/history")
    class EventHistoryTests {

        @Test
        @DisplayName("200 OK — returns event history")
        void getHistory_200() throws Exception {
            UUID id = UUID.randomUUID();
            List<OrderEventHistoryResponse> history = List.of(
                    OrderEventHistoryResponse.builder()
                            .sequenceNumber(1)
                            .eventType("ORDER_PLACED")
                            .eventVersion(1)
                            .payload("{}")
                            .occurredOn(Instant.now())
                            .build());

            when(queryHandler.findHistoryByOrderId(id.toString())).thenReturn(history);

            mockMvc.perform(get("/api/orders/{id}/history", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventType").value("ORDER_PLACED"));
        }
    }
}
