package com.example.biz.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额值对象：不可变、统一保留 2 位小数（HALF_UP），相等性基于归一化后的值。
 * <p> record 自动按字段生成 equals/hashCode，满足"覆写 equals 必须覆写 hashCode"契约。
 * 单币种模型；多币种场景在此扩展 currency 字段与换算规则。</p>
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(Integer quantity) {
        Objects.requireNonNull(quantity, "quantity must not be null");
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}
