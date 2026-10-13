package com.example.biz.domain.aggregate;

import com.example.biz.domain.entity.OrderItem;
import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.service.OrderPricingService;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合根单测：不变量与状态机，纯领域层零依赖。
 */
class OrderTest {

    private OrderPricingService.PricingResult pricing() {
        return new OrderPricingService.PricingResult(
                List.of(new OrderItem(1L, "Mechanical Keyboard", Money.of("199.00"), 1, Money.of("199.00"))),
                Money.of("199.00"), Money.of("10.00"), Money.of("189.00"));
    }

    @Test
    void createShouldStartFromUnpaid() {
        Order order = Order.create("SO20260921000001", 1000L, pricing(), "note");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.UNPAID);
        assertThat(order.getPayAmount()).isEqualTo(Money.of("189.00"));
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void createShouldRejectEmptyItems() {
        OrderPricingService.PricingResult emptyResult = new OrderPricingService.PricingResult(
                List.of(), Money.of("0.00"), Money.of("0.00"), Money.of("0.00"));

        assertThatThrownBy(() -> Order.create("SO20260921000002", 1000L, emptyResult, null))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_EMPTY_ITEMS));
    }

    @Test
    void createShouldRejectNegativePayAmount() {
        OrderPricingService.PricingResult negativeResult = new OrderPricingService.PricingResult(
                List.of(new OrderItem(1L, "Keyboard", Money.of("10.00"), 1, Money.of("10.00"))),
                Money.of("10.00"), Money.of("80.00"), Money.of("-70.00"));

        assertThatThrownBy(() -> Order.create("SO20260921000003", 1000L, negativeResult, null))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_AMOUNT_ILLEGAL));
    }

    @Test
    void payShouldTransitionUnpaidToPaid() {
        Order order = Order.create("SO20260921000004", 1000L, pricing(), null);
        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void payShouldRejectAlreadyPaidOrder() {
        Order order = Order.create("SO20260921000005", 1000L, pricing(), null);
        order.pay();

        assertThatThrownBy(order::pay)
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_STATUS_ILLEGAL));
    }

    @Test
    void cancelShouldRejectPaidOrder() {
        Order order = Order.create("SO20260921000006", 1000L, pricing(), null);
        order.pay();

        assertThatThrownBy(order::cancel)
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_STATUS_ILLEGAL));
    }

    @Test
    void cancelShouldTransitionUnpaidToCancelled() {
        Order order = Order.create("SO20260921000007", 1000L, pricing(), null);
        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void itemsShouldBeUnmodifiableView() {
        Order order = Order.create("SO20260921000008", 1000L, pricing(), null);

        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
