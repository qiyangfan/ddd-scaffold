package com.example.biz.domain.service;

import com.example.biz.domain.entity.OrderItem;
import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.ProductInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单定价领域服务：承载跨多明细的核心计价规则（行小计、总额、满减档位）。
 * <p>无状态纯领域逻辑，不依赖 Spring——由 infrastructure 层装配为 Bean 供应用层注入。</p>
 */
public class OrderPricingService {

    /** 满减档位与优惠金额：规则常量集中定义，禁止魔法值 */
    private static final Money TIER_DISCOUNT_THRESHOLD_HIGH = Money.of("500.00");
    private static final Money TIER_DISCOUNT_THRESHOLD_LOW = Money.of("100.00");
    private static final Money DISCOUNT_HIGH = Money.of("80.00");
    private static final Money DISCOUNT_LOW = Money.of("10.00");
    private static final Money ZERO_DISCOUNT = Money.of("0.00");

    /**
     * 定价：校验商品可售与数量合法，计算行小计与总额，套用满减档位。
     *
     * @param drafts     购买意向（商品 + 数量）
     * @param productMap 商品信息，键为商品 ID（由应用层经网关预先获取）
     */
    public PricingResult price(List<OrderItemDraft> drafts, Map<Long, ProductInfo> productMap) {
        if (drafts == null || drafts.isEmpty()) {
            throw new DomainException(DomainErrorCode.ORDER_EMPTY_ITEMS);
        }
        Money totalAmount = ZERO_DISCOUNT;
        List<OrderItem> items = new ArrayList<>(drafts.size());
        for (OrderItemDraft draft : drafts) {
            if (draft.quantity() == null || draft.quantity() <= 0) {
                throw new DomainException(DomainErrorCode.ORDER_ITEM_QUANTITY_ILLEGAL);
            }
            ProductInfo product = productMap.get(draft.productId());
            if (product == null) {
                throw new DomainException(DomainErrorCode.PRODUCT_NOT_FOUND);
            }
            if (!product.onSale()) {
                throw new DomainException(DomainErrorCode.PRODUCT_OFF_SALE);
            }
            Money lineAmount = product.price().multiply(draft.quantity());
            items.add(new OrderItem(product.productId(), product.productName(), product.price(), draft.quantity(), lineAmount));
            totalAmount = totalAmount.add(lineAmount);
        }
        Money discountAmount = calculateDiscount(totalAmount);
        Money payAmount = totalAmount.subtract(discountAmount);
        return new PricingResult(items, totalAmount, discountAmount, payAmount);
    }

    /** 满减规则：满 500 减 80，满 100 减 10，不满不减 */
    private Money calculateDiscount(Money totalAmount) {
        if (totalAmount.compareTo(TIER_DISCOUNT_THRESHOLD_HIGH) >= 0) {
            return DISCOUNT_HIGH;
        }
        if (totalAmount.compareTo(TIER_DISCOUNT_THRESHOLD_LOW) >= 0) {
            return DISCOUNT_LOW;
        }
        return ZERO_DISCOUNT;
    }

    /** 购买意向：进入领域服务前的最小入参模型 */
    public record OrderItemDraft(Long productId, Integer quantity) {
    }

    /** 定价结果：聚合工厂据此创建订单 */
    public record PricingResult(List<OrderItem> items, Money totalAmount, Money discountAmount, Money payAmount) {

        public PricingResult {
            items = List.copyOf(items);
        }
    }
}
