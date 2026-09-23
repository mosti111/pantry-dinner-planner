package com.pantry.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Quantity(BigDecimal value, Unit unit) {

    public Quantity {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
        if (value.signum() < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        value = value.stripTrailingZeros();
    }

    public static Quantity of(String value, Unit unit) {
        return new Quantity(new BigDecimal(value), unit);
    }

    public int packagesNeeded(Quantity packageSize) {
        requireSameUnit(packageSize);
        if (packageSize.value.signum() <= 0) throw new IllegalArgumentException("Package size must be positive");
        if (value.signum() == 0) return 0;
        return value.divide(packageSize.value, 0, RoundingMode.CEILING).intValueExact();
    }

    public Quantity subtractFloorZero(Quantity other) {
        requireSameUnit(other);
        return new Quantity(value.subtract(other.value).max(BigDecimal.ZERO), unit);
    }

    public Quantity multiply(int count) {
        if (count < 0) throw new IllegalArgumentException("count cannot be negative");
        return new Quantity(value.multiply(BigDecimal.valueOf(count)), unit);
    }

    private void requireSameUnit(Quantity other) {
        if (unit != other.unit) throw new IllegalArgumentException("Unit mismatch");
    }

    public enum Unit { GRAM, MILLILITER, EACH }
}
