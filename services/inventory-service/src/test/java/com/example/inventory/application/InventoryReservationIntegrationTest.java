package com.example.inventory.application;

import com.example.inventory.application.command.dto.ReplenishStockCommand;
import com.example.inventory.application.command.dto.ReserveStockCommand;
import com.example.inventory.application.command.handler.ReleaseStockHandler;
import com.example.inventory.application.command.handler.ReplenishStockHandler;
import com.example.inventory.application.command.handler.ReserveStockHandler;
import com.example.inventory.application.query.dto.StockLevelResponse;
import com.example.inventory.application.query.handler.StockQueryHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("Inventory reservation integration tests")
class InventoryReservationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("inventory_events")
                    .withUsername("postgres")
                    .withPassword("secret");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired ReplenishStockHandler replenishStockHandler;
    @Autowired ReserveStockHandler reserveStockHandler;
    @Autowired ReleaseStockHandler releaseStockHandler;
    @Autowired StockQueryHandler stockQueryHandler;

    @Test
    @DisplayName("reserve then release by orderId updates read model correctly")
    void reserve_thenReleaseByOrderId_updatesReadModel() {
        String productId = "product-001";
        String orderId = "order-001";

        replenishStockHandler.handle(ReplenishStockCommand.builder()
                .productId(productId)
                .quantity(10)
                .reference("INIT")
                .build());

        reserveStockHandler.handle(ReserveStockCommand.builder()
                .productId(productId)
                .orderId(orderId)
                .quantity(3)
                .build());

        Optional<StockLevelResponse> afterReserve = stockQueryHandler.findByProductId(productId);
        assertThat(afterReserve).isPresent();
        assertThat(afterReserve.get().getAvailableQuantity()).isEqualTo(7);
        assertThat(afterReserve.get().getReservedQuantity()).isEqualTo(3);

        releaseStockHandler.handleByOrderId(orderId);

        Optional<StockLevelResponse> afterRelease = stockQueryHandler.findByProductId(productId);
        assertThat(afterRelease).isPresent();
        assertThat(afterRelease.get().getAvailableQuantity()).isEqualTo(10);
        assertThat(afterRelease.get().getReservedQuantity()).isEqualTo(0);
    }
}
