package com.pantry.planning;

import com.pantry.shared.Money;
import com.pantry.shared.Quantity;

public final class PackageCalculator {
    private PackageCalculator() {}

    public static Result calculate(Quantity required, Quantity packageSize, Money packagePrice) {
        int count = required.packagesNeeded(packageSize);
        Quantity supplied = packageSize.multiply(count);
        Quantity leftover = supplied.subtractFloorZero(required);
        return new Result(count, supplied, leftover, packagePrice.multiply(count));
    }

    public record Result(int packageCount, Quantity supplied, Quantity leftover, Money lineTotal) {}
}
