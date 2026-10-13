package com.example.biz.domain.valueobject;

import java.util.Objects;

/**
 * 商品信息值对象：领域层关心的商品外部契约。
 * <p>由 infrastructure 层的网关实现从外部系统获取并转换为本模型，外部结构不进入领域层。</p>
 */
public record ProductInfo(Long productId, String productName, Money price, boolean onSale) {

    public ProductInfo {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(price, "price must not be null");
    }
}
