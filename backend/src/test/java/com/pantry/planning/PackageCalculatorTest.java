package com.pantry.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.pantry.shared.Money;
import com.pantry.shared.Quantity;
import com.pantry.shared.Quantity.Unit;
import org.junit.jupiter.api.Test;

class PackageCalculatorTest {
    @Test
    void roundsUpWholePackagesAndReportsLeftover() {
        PackageCalculator.Result result = PackageCalculator.calculate(
                Quantity.of("800", Unit.GRAM), Quantity.of("500", Unit.GRAM), Money.tryAmount("33.25"));

        assertThat(result.packageCount()).isEqualTo(2);
        assertThat(result.supplied().value()).isEqualByComparingTo("1000");
        assertThat(result.leftover().value()).isEqualByComparingTo("200");
        assertThat(result.lineTotal().amount()).isEqualByComparingTo("66.50");
    }

    @Test
    void zeroDemandNeedsNoPackages() {
        PackageCalculator.Result result = PackageCalculator.calculate(
                Quantity.of("0", Unit.GRAM), Quantity.of("500", Unit.GRAM), Money.tryAmount("33.25"));
        assertThat(result.packageCount()).isZero();
        assertThat(result.lineTotal().amount()).isEqualByComparingTo("0.00");
    }
}
