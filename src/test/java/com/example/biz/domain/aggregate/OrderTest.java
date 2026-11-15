package com.example.biz.domain.aggregate;

import com.example.biz.domain.entity.OrderItem;
import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.service.OrderPricingService;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合根单测：不变量与状态机，纯领域层零依赖。
 */
class OrderTest {

    /** 固定时钟：时间语义确定性可测 */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T08:00:00Z"), ZoneOffset.UTC);

    private OrderPricingService.PricingResult pricing() {
        return new OrderPricingService.PricingResult(
                List.of(new OrderItem(1L, "Mechanical Keyboard", Money.of("199.00"), 1, Money.of("199.00"))),
                Money.of("199.00"), Money.of("10.00"), Money.of("189.00"));
    }

    @Test
    void createShouldStartFromUnpaid() {
        Order order = Order.create("SO20260921000001", 1000L, pricing(), "note", CLOCK);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.UNPAID);
        assertThat(order.getPayAmount()).isEqualTo(Money.of("189.00"));
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void createShouldRejectEmptyItems() {
        OrderPricingService.PricingResult emptyResult = new OrderPricingService.PricingResult(
                List.of(), Money.of("0.00"), Money.of("0.00"), Money.of("0.00"));

        assertThatThrownBy(() -> Order.create("SO20260921000002", 1000L, emptyResult, null, CLOCK))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_EMPTY_ITEMS));
    }

    @Test
    void createShouldRejectNegativePayAmount() {
        OrderPricingService.PricingResult negativeResult = new OrderPricingService.PricingResult(
                List.of(new OrderItem(1L, "Keyboard", Money.of("10.00"), 1, Money.of("10.00"))),
                Money.of("10.00"), Money.of("80.00"), Money.of("-70.00"));

        assertThatThrownBy(() -> Order.create("SO20260921000003", 1000L, negativeResult, null, CLOCK))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_AMOUNT_ILLEGAL));
    }

    /** 时间语义：创建与状态跃迁的时间戳都来自注入时钟，而非墙钟 */
    @Test
    void timestampsShouldComeFromInjectedClock() {
        Order order = Order.create("SO20260921000009", 1000L, pricing(), null, CLOCK);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.parse("2026-11-15T08:00:00Z"), ZoneOffset.UTC);

        assertThat(order.getCreatedAt()).isEqualTo(expected);
        assertThat(order.getUpdatedAt()).isEqualTo(expected);

        Clock later = Clock.fixed(Instant.parse("2026-11-15T09:30:00Z"), ZoneOffset.UTC);
        order.pay(later);

        assertThat(order.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(Instant.parse("2026-11-15T09:30:00Z"),
                ZoneOffset.UTC));
    }

    @Test
    void payShouldTransitionUnpaidToPaid() {
        Order order = Order.create("SO20260921000004", 1000L, pricing(), null, CLOCK);
        order.pay(CLOCK);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void payShouldRejectAlreadyPaidOrder() {
        Order order = Order.create("SO20260921000005", 1000L, pricing(), null, CLOCK);
        order.pay(CLOCK);

        assertThatThrownBy(() -> order.pay(CLOCK))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_STATUS_ILLEGAL));
    }

    @Test
    void cancelShouldRejectPaidOrder() {
        Order order = Order.create("SO20260921000006", 1000L, pricing(), null, CLOCK);
        order.pay(CLOCK);

        assertThatThrownBy(() -> order.cancel(CLOCK))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_STATUS_ILLEGAL));
    }

    @Test
    void cancelShouldTransitionUnpaidToCancelled() {
        Order order = Order.create("SO20260921000007", 1000L, pricing(), null, CLOCK);
        order.cancel(CLOCK);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void itemsShouldBeUnmodifiableView() {
        Order order = Order.create("SO20260921000008", 1000L, pricing(), null, CLOCK);

        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
