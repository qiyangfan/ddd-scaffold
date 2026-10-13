package com.example.biz.domain.service;

import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.ProductInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 领域服务单测：定价规则（行小计 / 满减档位 / 商品校验），无需启动 Spring。
 */
class OrderPricingServiceTest {

    private final OrderPricingService pricingService = new OrderPricingService();

    private final Map<Long, ProductInfo> products = Map.of(
            1L, new ProductInfo(1L, "Mechanical Keyboard", Money.of("199.00"), true),
            2L, new ProductInfo(2L, "Laptop Pro 14", Money.of("5999.00"), true),
            3L, new ProductInfo(3L, "Wireless Mouse", Money.of("99.00"), true),
            4L, new ProductInfo(4L, "Discontinued Dock", Money.of("399.00"), false));

    @Test
    void highTierDiscountShouldApplyAbove500() {
        OrderPricingService.PricingResult result = pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(2L, 1)), products);

        assertThat(result.totalAmount()).isEqualTo(Money.of("5999.00"));
        assertThat(result.discountAmount()).isEqualTo(Money.of("80.00"));
        assertThat(result.payAmount()).isEqualTo(Money.of("5919.00"));
    }

    @Test
    void lowTierDiscountShouldApplyAbove100() {
        OrderPricingService.PricingResult result = pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(1L, 1)), products);

        assertThat(result.totalAmount()).isEqualTo(Money.of("199.00"));
        assertThat(result.discountAmount()).isEqualTo(Money.of("10.00"));
        assertThat(result.payAmount()).isEqualTo(Money.of("189.00"));
    }

    @Test
    void noDiscountShouldApplyBelow100() {
        OrderPricingService.PricingResult result = pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(3L, 1)), products);

        assertThat(result.discountAmount()).isEqualTo(Money.of("0.00"));
        assertThat(result.payAmount()).isEqualTo(Money.of("99.00"));
    }

    @Test
    void lineAmountShouldSnapshotUnitPrice() {
        OrderPricingService.PricingResult result = pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(2L, 2)), products);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("Laptop Pro 14");
        assertThat(result.items().get(0).lineAmount()).isEqualTo(Money.of("11998.00"));
    }

    @Test
    void offSaleProductShouldBeRejected() {
        assertThatThrownBy(() -> pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(4L, 1)), products))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.PRODUCT_OFF_SALE));
    }

    @Test
    void missingProductShouldBeRejected() {
        assertThatThrownBy(() -> pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(99L, 1)), products))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    void nonPositiveQuantityShouldBeRejected() {
        assertThatThrownBy(() -> pricingService.price(
                List.of(new OrderPricingService.OrderItemDraft(1L, 0)), products))
                .isInstanceOfSatisfying(DomainException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(DomainErrorCode.ORDER_ITEM_QUANTITY_ILLEGAL));
    }
}
