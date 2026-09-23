package com.pantry.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void addsUsingDecimalArithmetic() {
        Money result = Money.tryAmount("10.10").add(Money.tryAmount("2.25"));
        assertThat(result.amount()).isEqualByComparingTo("12.35");
    }

    @Test
    void rejectsCurrencyMismatch() {
        Money euros = new Money(new BigDecimal("1"), Currency.getInstance("EUR"));
        assertThatThrownBy(() -> Money.tryAmount("1").add(euros))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency mismatch");
    }
}
