package com.example.biz.domain.gateway;

import com.example.biz.domain.valueobject.ProductInfo;

/**
 * 商品网关（外部系统端口）：对规范包结构的一点补充——domain/gateway 与 domain/repository 同属依赖倒置端口，
 * repository 端口面向持久化，gateway 端口面向外部系统（RPC/HTTP/MQ/缓存），实现都在 infrastructure/adapter。
 */
public interface ProductGateway {

    /** 查询商品；不存在抛 {@link com.example.biz.domain.exception.DomainErrorCode#PRODUCT_NOT_FOUND} */
    ProductInfo getProduct(Long productId);
}
