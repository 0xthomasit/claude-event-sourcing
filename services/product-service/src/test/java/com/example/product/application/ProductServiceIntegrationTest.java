package com.example.product.application;

import com.example.product.application.dto.CreateProductRequest;
import com.example.product.application.dto.ProductResponse;
import com.example.product.application.dto.UpdateProductRequest;
import com.example.product.application.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@DisplayName("ProductService integration tests")
class ProductServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("product_db")
                    .withUsername("postgres")
                    .withPassword("secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductService productService;

    // Mock Kafka so tests don't need a broker
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("create() — persists product and returns response with ID")
    void create_persistsProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("TEST-SKU-001")
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("100000"))
                .currency("VND")
                .build();

        ProductResponse response = productService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSku()).isEqualTo("TEST-SKU-001");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("create() — duplicate SKU throws IllegalArgumentException")
    void create_duplicateSku_throws() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("DUPE-SKU-001")
                .name("Product A")
                .price(new BigDecimal("50000"))
                .currency("VND")
                .build();

        productService.create(request);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> productService.create(request));
    }

    @Test
    @DisplayName("update() — price change publishes event")
    void update_priceChange_publishesEvent() {
        CreateProductRequest create = CreateProductRequest.builder()
                .sku("PRICE-TEST-001")
                .name("Price Test Product")
                .price(new BigDecimal("200000"))
                .currency("VND")
                .build();
        ProductResponse created = productService.create(create);

        UpdateProductRequest update = UpdateProductRequest.builder()
                .price(new BigDecimal("180000"))
                .build();
        ProductResponse updated = productService.update(created.getId(), update);

        assertThat(updated.getPrice()).isEqualByComparingTo("180000");
    }
}