package com.example.biz.infrastructure.gateway;

import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.gateway.ProductGateway;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.ProductInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商品网关演示实现：内存数据模拟商品服务。
 * <p>生产实现替换要点：改用 RPC/HTTP 客户端 + 显式超时；把外部响应模型转换成 ProductInfo；
 * 外部异常统一翻译为 DomainException / 业务异常，禁止外泄第三方类型。ID 为演示数据，非魔法值。</p>
 */
@Component
public class DemoProductGateway implements ProductGateway {

    private static final Map<Long, ProductInfo> PRODUCTS = Map.of(
            1L, new ProductInfo(1L, "Mechanical Keyboard", Money.of("199.00"), true),
            2L, new ProductInfo(2L, "Laptop Pro 14", Money.of("5999.00"), true),
            3L, new ProductInfo(3L, "Wireless Mouse", Money.of("99.00"), true),
            4L, new ProductInfo(4L, "Discontinued Dock", Money.of("399.00"), false));

    @Override
    public ProductInfo getProduct(Long productId) {
        ProductInfo productInfo = PRODUCTS.get(productId);
        if (productInfo == null) {
            throw new DomainException(DomainErrorCode.PRODUCT_NOT_FOUND);
        }
        return productInfo;
    }
}
