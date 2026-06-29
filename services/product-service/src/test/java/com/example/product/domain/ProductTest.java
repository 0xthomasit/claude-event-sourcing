package com.example.product.domain;

import com.example.product.domain.model.Product;
import com.example.product.domain.model.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product domain model")
class ProductTest {

    private Product buildProduct(BigDecimal price) {
        return Product.builder()
                .sku("SKU-001")
                .name("iPhone 15")
                .price(price)
                .currency("VND")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("isPriceChanged() — returns true when price differs")
    void isPriceChanged_differentPrice_returnsTrue() {
        Product p = buildProduct(new BigDecimal("25000000"));
        assertThat(p.isPriceChanged(new BigDecimal("24000000"))).isTrue();
    }

    @Test
    @DisplayName("isPriceChanged() — returns false when price is same")
    void isPriceChanged_samePrice_returnsFalse() {
        Product p = buildProduct(new BigDecimal("25000000"));
        assertThat(p.isPriceChanged(new BigDecimal("25000000"))).isFalse();
    }

    @Test
    @DisplayName("updatePrice() — updates price successfully")
    void updatePrice_validPrice_updatesSuccessfully() {
        Product p = buildProduct(new BigDecimal("25000000"));
        p.updatePrice(new BigDecimal("23000000"));
        assertThat(p.getPrice()).isEqualByComparingTo("23000000");
    }

    @Test
    @DisplayName("updatePrice() — zero price throws IllegalArgumentException")
    void updatePrice_zeroPrice_throws() {
        Product p = buildProduct(new BigDecimal("25000000"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> p.updatePrice(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("deactivate() — sets status to INACTIVE")
    void deactivate_setsStatusInactive() {
        Product p = buildProduct(new BigDecimal("25000000"));
        p.deactivate();
        assertThat(p.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("discontinue() — sets status to DISCONTINUED")
    void discontinue_setsStatusDiscontinued() {
        Product p = buildProduct(new BigDecimal("25000000"));
        p.discontinue();
        assertThat(p.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
    }
}