package com.example.biz.domain.entity;

import com.example.biz.domain.valueobject.Money;

import java.util.Objects;

/**
 * 订单明细实体：Order 聚合内部的从属实体，无独立生命周期与全局标识（在聚合内以商品维度存在），
 * 因此采用不可变 record 按值语义建模，快照下单时刻的商品名与单价。
 */
public record OrderItem(Long productId, String productName, Money unitPrice, Integer quantity, Money lineAmount) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(lineAmount, "lineAmount must not be null");
    }
}
