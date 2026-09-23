package com.pantry.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    public static Money tryAmount(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("TRY"));
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtractFloorZero(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount).max(BigDecimal.ZERO), currency);
    }

    public Money multiply(int count) {
        if (count < 0) throw new IllegalArgumentException("count cannot be negative");
        return new Money(amount.multiply(BigDecimal.valueOf(count)), currency);
    }

    public Money divide(int divisor) {
        if (divisor <= 0) throw new IllegalArgumentException("divisor must be positive");
        return new Money(amount.divide(BigDecimal.valueOf(divisor), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }
}
