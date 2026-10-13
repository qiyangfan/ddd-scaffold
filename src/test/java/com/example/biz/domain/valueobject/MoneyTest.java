package com.example.biz.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 值对象单测：金额归一化与运算契约，纯领域层零依赖。
 */
class MoneyTest {

    @Test
    void amountShouldBeNormalizedToTwoFractionDigits() {
        assertThat(Money.of("1.5").amount()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(Money.of(new BigDecimal("100"))).isEqualTo(Money.of("100.00"));
    }

    @Test
    void addShouldKeepScale() {
        Money sum = Money.of("0.01").add(Money.of("199.99"));
        assertThat(sum).isEqualTo(Money.of("200.00"));
    }

    @Test
    void multiplyShouldWorkWithQuantity() {
        Money line = Money.of("5999.00").multiply(2);
        assertThat(line).isEqualTo(Money.of("11998.00"));
    }

    @Test
    void equalsShouldIgnoreScaleDifference() {
        assertThat(Money.of("10.0")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of("10.00").hashCode()).isEqualTo(Money.of("10.0").hashCode());
    }

    @Test
    void compareToShouldSupportTierCheck() {
        assertThat(Money.of("500.00").compareTo(Money.of("500.00"))).isZero();
        assertThat(Money.of("500.01").compareTo(Money.of("500.00"))).isPositive();
        assertThat(Money.of("99.99").compareTo(Money.of("100.00"))).isNegative();
    }

    @Test
    void negativeAmountShouldBeDetected() {
        assertThat(Money.of("-0.01").isNegative()).isTrue();
        assertThat(Money.of("0.00").isNegative()).isFalse();
    }
}
