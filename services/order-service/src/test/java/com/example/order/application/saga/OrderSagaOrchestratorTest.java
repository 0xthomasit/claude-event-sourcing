package com.example.order.application.saga;

import com.example.common.events.saga.*;
import com.example.order.domain.model.Order;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.messaging.publisher.KafkaEventPublisher;
import com.example.order.infrastructure.metrics.OrderMetrics;
import com.example.order.infrastructure.persistence.entity.SagaInstance;
import com.example.order.infrastructure.persistence.repository.JpaSagaRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaOrchestrator")
class OrderSagaOrchestratorTest {

    @Mock private JpaSagaRepository sagaRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderProjector orderProjector;
    @Mock private KafkaEventPublisher kafkaPublisher;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private OrderMetrics orderMetrics;

    @InjectMocks private OrderSagaOrchestrator orchestrator;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() throws Exception {
        // Replace the null ObjectMapper injected by Mockito with a real one
        var omField = OrderSagaOrchestrator.class.getDeclaredField("objectMapper");
        omField.setAccessible(true);
        omField.set(orchestrator, objectMapper);

        // Inject real SagaProperties with defaults (not mockable — it's a config POJO)
        var spField = OrderSagaOrchestrator.class.getDeclaredField("sagaProperties");
        spField.setAccessible(true);
        spField.set(orchestrator, new com.example.order.infrastructure.saga.SagaProperties());
    }

    private OrderSagaData createTestSagaData(String orderId) {
        return OrderSagaData.builder()
                .orderId(orderId)
                .customerId("customer-1")
                .items(List.of(OrderSagaData.ItemData.builder()
                        .productId("product-1")
                        .productName("Test Product")
                        .quantity(2)
                        .unitPrice(new BigDecimal("100000"))
                        .build()))
                .totalAmount(new BigDecimal("200000"))
                .currency("VND")
                .stockReserved(false)
                .paymentProcessed(false)
                .build();
    }

    @Nested
    @DisplayName("startSaga")
    class StartSaga {

        @Test
        @DisplayName("should persist saga and send ReserveStockCommand")
        void shouldStartSagaAndSendReserveStockCommand() {
            UUID orderId = UUID.randomUUID();
            OrderSagaData data = createTestSagaData(orderId.toString());

            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(i -> i.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            orchestrator.startSaga(orderId, data);

            // Verify saga persisted with RESERVING_STOCK step
            ArgumentCaptor<SagaInstance> sagaCaptor = ArgumentCaptor.forClass(SagaInstance.class);
            verify(sagaRepository).save(sagaCaptor.capture());
            SagaInstance saved = sagaCaptor.getValue();
            assertThat(saved.getCurrentStep()).isEqualTo("RESERVING_STOCK");
            assertThat(saved.getOrderId()).isEqualTo(orderId.toString());
            assertThat(saved.getSagaType()).isEqualTo("OrderPlacementSaga");

            // Verify command sent to Kafka
            verify(kafkaTemplate).send(
                    eq(SagaTopics.RESERVE_STOCK_CMD),
                    eq(orderId.toString()),
                    any(ReserveStockCommand.class));
        }
    }

    @Nested
    @DisplayName("onStockReservedReply")
    class OnStockReservedReply {

        @Test
        @DisplayName("success → should advance to PROCESSING_PAYMENT")
        void successShouldAdvanceToProcessingPayment() throws Exception {
            UUID sagaId = UUID.randomUUID();
            String orderId = UUID.randomUUID().toString();
            OrderSagaData data = createTestSagaData(orderId);

            SagaInstance saga = SagaInstance.builder()
                    .id(sagaId)
                    .orderId(orderId)
                    .currentStep("RESERVING_STOCK")
                    .payload(objectMapper.writeValueAsString(data))
                    .build();

            when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));
            when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StockReservedReply reply = StockReservedReply.builder()
                    .sagaId(sagaId.toString())
                    .orderId(orderId)
                    .success(true)
                    .build();

            orchestrator.onStockReservedReply(reply);

            assertThat(saga.getCurrentStep()).isEqualTo("PROCESSING_PAYMENT");
            verify(kafkaTemplate).send(
                    eq(SagaTopics.PROCESS_PAYMENT_CMD),
                    eq(orderId),
                    any(ProcessPaymentCommand.class));
        }

        @Test
        @DisplayName("failure → should REJECT saga and cancel order")
        void failureShouldRejectAndCancel() throws Exception {
            UUID sagaId = UUID.randomUUID();
            String orderId = UUID.randomUUID().toString();
            OrderSagaData data = createTestSagaData(orderId);

            SagaInstance saga = SagaInstance.builder()
                    .id(sagaId)
                    .orderId(orderId)
                    .currentStep("RESERVING_STOCK")
                    .payload(objectMapper.writeValueAsString(data))
                    .build();

            when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));
            when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Mock order for cancellation
            Order order = Order.place("customer-1", List.of(
                    com.example.order.domain.model.OrderItem.of("p1", "Product",
                            1, com.example.common.domain.model.Money.of(new BigDecimal("100"), "VND"))));
            when(orderRepository.findById(UUID.fromString(orderId))).thenReturn(Optional.of(order));

            StockReservedReply reply = StockReservedReply.builder()
                    .sagaId(sagaId.toString())
                    .orderId(orderId)
                    .success(false)
                    .failureReason("Insufficient stock")
                    .build();

            orchestrator.onStockReservedReply(reply);

            assertThat(saga.getCurrentStep()).isEqualTo("REJECTED");
            assertThat(saga.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("onPaymentProcessedReply")
    class OnPaymentProcessedReply {

        @Test
        @DisplayName("success → should confirm order and COMPLETE saga")
        void successShouldConfirmAndComplete() throws Exception {
            UUID sagaId = UUID.randomUUID();
            String orderId = UUID.randomUUID().toString();
            OrderSagaData data = createTestSagaData(orderId).withStockReserved(true);

            SagaInstance saga = SagaInstance.builder()
                    .id(sagaId)
                    .orderId(orderId)
                    .currentStep("PROCESSING_PAYMENT")
                    .payload(objectMapper.writeValueAsString(data))
                    .build();

            when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));
            when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Mock order for confirmation
            Order order = Order.place("customer-1", List.of(
                    com.example.order.domain.model.OrderItem.of("p1", "Product",
                            1, com.example.common.domain.model.Money.of(new BigDecimal("100"), "VND"))));
            when(orderRepository.findById(UUID.fromString(orderId))).thenReturn(Optional.of(order));

            PaymentProcessedReply reply = PaymentProcessedReply.builder()
                    .sagaId(sagaId.toString())
                    .orderId(orderId)
                    .success(true)
                    .paymentId(UUID.randomUUID().toString())
                    .build();

            orchestrator.onPaymentProcessedReply(reply);

            assertThat(saga.getCurrentStep()).isEqualTo("COMPLETED");
            assertThat(saga.getCompletedAt()).isNotNull();
            verify(orderMetrics).orderConfirmed();
        }

        @Test
        @DisplayName("failure → should compensate stock and cancel")
        void failureShouldCompensateStock() throws Exception {
            UUID sagaId = UUID.randomUUID();
            String orderId = UUID.randomUUID().toString();
            OrderSagaData data = createTestSagaData(orderId).withStockReserved(true);

            SagaInstance saga = SagaInstance.builder()
                    .id(sagaId)
                    .orderId(orderId)
                    .currentStep("PROCESSING_PAYMENT")
                    .payload(objectMapper.writeValueAsString(data))
                    .build();

            when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));
            when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            PaymentProcessedReply reply = PaymentProcessedReply.builder()
                    .sagaId(sagaId.toString())
                    .orderId(orderId)
                    .success(false)
                    .failureReason("Card declined")
                    .build();

            orchestrator.onPaymentProcessedReply(reply);

            assertThat(saga.getCurrentStep()).isEqualTo("COMPENSATING_STOCK");
            verify(kafkaTemplate).send(
                    eq(SagaTopics.RELEASE_STOCK_CMD),
                    eq(orderId),
                    any(ReleaseStockCommand.class));
        }
    }
}
