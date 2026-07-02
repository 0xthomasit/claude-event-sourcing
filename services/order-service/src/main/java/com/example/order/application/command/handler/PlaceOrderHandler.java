package com.example.order.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.model.Money;
import com.example.common.events.order.OrderPlacedEvent;
import com.example.order.application.command.dto.PlaceOrderCommand;
import com.example.order.application.saga.OrderSagaData;
import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.client.PromotionClient;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.metrics.OrderMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderProjector  orderProjector;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OrderMetrics orderMetrics;
    private final PromotionClient promotionClient;

    @Transactional
    public UUID handle(PlaceOrderCommand command) {
        List<OrderItem> items = command.getItems().stream()
                .map(i -> OrderItem.of(i.getProductId(), i.getProductName(),
                        i.getQuantity(), Money.of(i.getUnitPrice(), i.getCurrency())))
                .toList();

        Order order = Order.place(command.getCustomerId(), items);

        // Capture events BEFORE save clears them
        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());

        // 1. Persist to Event Store (source of truth)
        orderRepository.save(order);

        // 2. Update Read Model (MongoDB)
        events.stream()
                .filter(e -> e instanceof OrderPlacedEvent)
                .map(e -> (OrderPlacedEvent) e)
                .forEach(orderProjector::on);

        // 3. Calculate total
        Money total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.of(BigDecimal.ZERO, "VND"), Money::add);

        // 4. Validate & redeem promotion (synchronous, pre-saga step)
        String promotionCode = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean promotionRedeemed = false;

        if (command.getPromotionCode() != null && !command.getPromotionCode().isBlank()) {
            PromotionClient.DiscountResult result = promotionClient.redeemPromotion(
                    command.getPromotionCode(),
                    order.getId().toString(),
                    command.getCustomerId(),
                    total.getAmount());

            if (result != null && result.isValid()) {
                promotionCode = result.getPromotionCode();
                discountAmount = result.getDiscountAmount();
                promotionRedeemed = true;
                log.info("Promotion {} applied: discount={}", promotionCode, discountAmount);
            } else {
                // Promotion invalid — fail fast, don't start saga
                String reason = result != null ? result.getReason() : "Promotion service error";
                log.warn("Promotion {} rejected for order {}: {}",
                        command.getPromotionCode(), order.getId(), reason);
                throw new IllegalArgumentException("Promotion code invalid: " + reason);
            }
        }

        // 5. Build saga data (with promotion info if applicable)
        BigDecimal effectiveTotal = total.getAmount().subtract(discountAmount);

        OrderSagaData sagaData = OrderSagaData.builder()
                .orderId(order.getId().toString())
                .customerId(command.getCustomerId())
                .items(command.getItems().stream()
                        .map(i -> OrderSagaData.ItemData.builder()
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .build())
                        .toList())
                .totalAmount(effectiveTotal)
                .currency(total.getCurrency())
                .promotionCode(promotionCode)
                .discountAmount(discountAmount)
                .promotionRedeemed(promotionRedeemed)
                .stockReserved(false)
                .paymentProcessed(false)
                .build();

        // 6. Start Saga (ReserveStock → ProcessPayment → ConfirmOrder)
        sagaOrchestrator.startSaga(order.getId(), sagaData);

        orderMetrics.orderPlaced();
        log.info("Order placed: {}, saga started, promotion={}, discount={}",
                order.getId(), promotionCode, discountAmount);
        return order.getId();
    }
}
