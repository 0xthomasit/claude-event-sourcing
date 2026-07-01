package com.example.order.domain.model;

import com.example.common.domain.exception.DomainException;
import com.example.common.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Factory: Money.of()")
    class FactoryTests {

        @Test
        @DisplayName("creates Money with valid amount and currency")
        void of_valid() {
            Money money = Money.of(new BigDecimal("100.00"), "VND");
            assertThat(money.getAmount()).isEqualByComparingTo("100.00");
            assertThat(money.getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("rejects null amount")
        void of_nullAmount_throws() {
            assertThatThrownBy(() -> Money.of(null, "VND"))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejects negative amount")
        void of_negativeAmount_throws() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-1"), "VND"))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejects null currency")
        void of_nullCurrency_throws() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejects blank currency")
        void of_blankCurrency_throws() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "  "))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("accepts zero amount")
        void of_zeroAmount_ok() {
            Money money = Money.of(BigDecimal.ZERO, "VND");
            assertThat(money.isZero()).isTrue();
        }
    }

    @Nested
    @DisplayName("Arithmetic operations")
    class ArithmeticTests {

        @Test
        @DisplayName("add() — same currency")
        void add_sameCurrency() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money b = Money.of(new BigDecimal("50"), "VND");
            Money result = a.add(b);
            assertThat(result.getAmount()).isEqualByComparingTo("150");
            assertThat(result.getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("add() — different currency throws")
        void add_differentCurrency_throws() {
            Money vnd = Money.of(new BigDecimal("100"), "VND");
            Money usd = Money.of(new BigDecimal("50"), "USD");
            assertThatThrownBy(() -> vnd.add(usd))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("subtract() — valid subtraction")
        void subtract_valid() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money b = Money.of(new BigDecimal("30"), "VND");
            Money result = a.subtract(b);
            assertThat(result.getAmount()).isEqualByComparingTo("70");
        }

        @Test
        @DisplayName("subtract() — would result in negative throws")
        void subtract_negative_throws() {
            Money a = Money.of(new BigDecimal("30"), "VND");
            Money b = Money.of(new BigDecimal("100"), "VND");
            assertThatThrownBy(() -> a.subtract(b))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("subtract() — exact zero is allowed")
        void subtract_exactZero() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money result = a.subtract(a);
            assertThat(result.isZero()).isTrue();
        }

        @Test
        @DisplayName("multiply() — positive factor")
        void multiply_positive() {
            Money money = Money.of(new BigDecimal("250"), "VND");
            Money result = money.multiply(3);
            assertThat(result.getAmount()).isEqualByComparingTo("750");
        }

        @Test
        @DisplayName("divide() — even division")
        void divide_even() {
            Money money = Money.of(new BigDecimal("100"), "VND");
            Money result = money.divide(4);
            assertThat(result.getAmount()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("divide() — rounds HALF_UP")
        void divide_rounds() {
            Money money = Money.of(new BigDecimal("100"), "VND");
            Money result = money.divide(3);
            assertThat(result.getAmount()).isEqualByComparingTo("33.33");
        }

        @Test
        @DisplayName("divide() — by zero throws")
        void divide_byZero_throws() {
            Money money = Money.of(new BigDecimal("100"), "VND");
            assertThatThrownBy(() -> money.divide(0))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("zero");
        }

        @Test
        @DisplayName("add() — null other throws")
        void add_nullOther_throws() {
            Money money = Money.of(BigDecimal.TEN, "VND");
            assertThatThrownBy(() -> money.add(null))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Comparison / utility methods")
    class ComparisonTests {

        @Test
        @DisplayName("isZero() — true for zero")
        void isZero_true() {
            assertThat(Money.of(BigDecimal.ZERO, "VND").isZero()).isTrue();
        }

        @Test
        @DisplayName("isZero() — false for non-zero")
        void isZero_false() {
            assertThat(Money.of(BigDecimal.ONE, "VND").isZero()).isFalse();
        }

        @Test
        @DisplayName("isPositive() — true for positive")
        void isPositive_true() {
            assertThat(Money.of(BigDecimal.ONE, "VND").isPositive()).isTrue();
        }

        @Test
        @DisplayName("isPositive() — false for zero")
        void isPositive_false_zero() {
            assertThat(Money.of(BigDecimal.ZERO, "VND").isPositive()).isFalse();
        }

        @Test
        @DisplayName("isGreaterThan() — true when greater")
        void isGreaterThan_true() {
            Money a = Money.of(new BigDecimal("200"), "VND");
            Money b = Money.of(new BigDecimal("100"), "VND");
            assertThat(a.isGreaterThan(b)).isTrue();
        }

        @Test
        @DisplayName("isGreaterThan() — false when equal")
        void isGreaterThan_false_equal() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money b = Money.of(new BigDecimal("100"), "VND");
            assertThat(a.isGreaterThan(b)).isFalse();
        }

        @Test
        @DisplayName("isGreaterThan() — different currency throws")
        void isGreaterThan_differentCurrency_throws() {
            Money vnd = Money.of(new BigDecimal("200"), "VND");
            Money usd = Money.of(new BigDecimal("100"), "USD");
            assertThatThrownBy(() -> vnd.isGreaterThan(usd))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("equal when same amount and currency")
        void equals_same() {
            Money a = Money.of(new BigDecimal("100.00"), "VND");
            Money b = Money.of(new BigDecimal("100.00"), "VND");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("equal despite different BigDecimal scales")
        void equals_differentScale() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money b = Money.of(new BigDecimal("100.00"), "VND");
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("not equal when different amount")
        void notEquals_differentAmount() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money b = Money.of(new BigDecimal("200"), "VND");
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("not equal when different currency")
        void notEquals_differentCurrency() {
            Money a = Money.of(new BigDecimal("100"), "VND");
            Money b = Money.of(new BigDecimal("100"), "USD");
            assertThat(a).isNotEqualTo(b);
        }
    }

    @Test
    @DisplayName("toString() — includes amount and currency")
    void toString_format() {
        Money money = Money.of(new BigDecimal("25000000"), "VND");
        assertThat(money.toString()).isEqualTo("25000000 VND");
    }
}
