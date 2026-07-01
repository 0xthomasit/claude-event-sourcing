package com.example.common.domain.model;

import com.example.common.domain.ValueObject;
import com.example.common.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable monetary value object.
 */
public final class Money implements ValueObject {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new DomainException("Money amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Money amount must be greater than or equal to zero");
        }
        if (currency == null || currency.isBlank()) {
            throw new DomainException("Money currency must not be null or blank");
        }
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Subtraction would result in negative money");
        }
        return new Money(result, this.currency);
    }

    public Money divide(int divisor) {
        if (divisor == 0) {
            throw new DomainException("Cannot divide money by zero");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), 2,
                java.math.RoundingMode.HALF_UP), this.currency);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        if (other == null) {
            throw new DomainException("Cannot operate on null Money");
        }
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
