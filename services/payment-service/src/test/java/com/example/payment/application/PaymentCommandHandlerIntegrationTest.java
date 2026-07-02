package com.example.payment.application;

import com.example.payment.application.command.dto.CompletePaymentCommand;
import com.example.payment.application.command.dto.InitiatePaymentCommand;
import com.example.payment.application.command.handler.CompletePaymentHandler;
import com.example.payment.application.command.handler.InitiatePaymentHandler;
import com.example.payment.application.query.dto.PaymentResponse;
import com.example.payment.application.query.handler.PaymentQueryHandler;
import com.example.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Payment Command Handler Integration Tests")
class PaymentCommandHandlerIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.4-alpine")
                    .withDatabaseName("payment_events")
                    .withUsername("postgres")
                    .withPassword("secret");

    @Container
    static MongoDBContainer mongo =
            new MongoDBContainer("mongo:7");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.9.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired InitiatePaymentHandler initiatePaymentHandler;
    @Autowired CompletePaymentHandler  completePaymentHandler;
    @Autowired PaymentQueryHandler     queryHandler;

    @Test
    @DisplayName("initiatePayment — persists to Event Store and Read Model")
    void initiatePayment_persistsEventAndReadModel() {
        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .orderId(UUID.randomUUID().toString())
                .customerId("customer-001")
                .amount(new BigDecimal("250000"))
                .currency("VND")
                .paymentMethodType("BANKING")
                .maskedAccount("****5678")
                .build();

        UUID paymentId = initiatePaymentHandler.handle(cmd);

        assertThat(paymentId).isNotNull();

        Optional<PaymentResponse> response = queryHandler.findById(paymentId.toString());
        assertThat(response).isPresent();
        assertThat(response.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.get().getOrderId()).isEqualTo(cmd.getOrderId());
        assertThat(response.get().getAmount()).isEqualByComparingTo(cmd.getAmount());
    }

    @Test
    @DisplayName("completePayment — status transitions to COMPLETED")
    void completePayment_statusBecomesCompleted() {
        String orderId = UUID.randomUUID().toString();
        InitiatePaymentCommand initCmd = InitiatePaymentCommand.builder()
                .orderId(orderId)
                .customerId("customer-002")
                .amount(new BigDecimal("500000"))
                .currency("VND")
                .paymentMethodType("CARD")
                .maskedAccount("****9999")
                .build();

        UUID paymentId = initiatePaymentHandler.handle(initCmd);

        completePaymentHandler.handle(CompletePaymentCommand.builder()
                .paymentId(paymentId.toString())
                .transactionRef("TXN-" + UUID.randomUUID())
                .build());

        Optional<PaymentResponse> response = queryHandler.findById(paymentId.toString());
        assertThat(response).isPresent();
        assertThat(response.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
