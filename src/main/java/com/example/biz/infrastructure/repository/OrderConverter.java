package com.example.biz.infrastructure.repository;

import com.example.biz.domain.aggregate.Order;
import com.example.biz.domain.entity.OrderItem;
import com.example.biz.domain.support.Ids;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.OrderStatus;

import java.util.List;

/**
 * 订单持久化转换器：聚合 &lt;-&gt; DO 的唯一转换点，显式映射禁止反射拷贝。
 * <p>主键与明细主键均由 {@link Ids} 生成（应用侧身份），转换器不再依赖数据库自增回填。</p>
 */
public final class OrderConverter {

    private OrderConverter() {
    }

    /** 聚合 -&gt; 订单 DO */
    public static OrderDO toDO(Order order) {
        OrderDO orderDO = new OrderDO();
        orderDO.setId(order.getId());
        orderDO.setOrderNo(order.getOrderNo());
        orderDO.setUserId(order.getUserId());
        orderDO.setTotalAmount(order.getTotalAmount().amount());
        orderDO.setDiscountAmount(order.getDiscountAmount().amount());
        orderDO.setPayAmount(order.getPayAmount().amount());
        orderDO.setStatus(order.getStatus().getCode());
        orderDO.setRemark(order.getRemark());
        orderDO.setGmtCreate(order.getCreatedAt());
        orderDO.setGmtModified(order.getUpdatedAt());
        return orderDO;
    }

    /** 聚合 -&gt; 明细 DO 列表（时间字段与订单保持一致） */
    public static List<OrderItemDO> toItemDOList(Order order) {
        return order.getItems().stream()
                .map(item -> {
                    OrderItemDO itemDO = new OrderItemDO();
                    itemDO.setId(Ids.newId());
                    itemDO.setOrderId(order.getId());
                    itemDO.setProductId(item.productId());
                    itemDO.setProductName(item.productName());
                    itemDO.setUnitPrice(item.unitPrice().amount());
                    itemDO.setQuantity(item.quantity());
                    itemDO.setLineAmount(item.lineAmount().amount());
                    itemDO.setGmtCreate(order.getCreatedAt());
                    itemDO.setGmtModified(order.getUpdatedAt());
                    return itemDO;
                })
                .toList();
    }

    /** DO 列表 -&gt; 聚合（经 restore 工厂重建） */
    public static Order toAggregate(OrderDO orderDO, List<OrderItemDO> itemDOList) {
        List<OrderItem> items = itemDOList.stream()
                .map(itemDO -> new OrderItem(itemDO.getProductId(), itemDO.getProductName(),
                        Money.of(itemDO.getUnitPrice()), itemDO.getQuantity(), Money.of(itemDO.getLineAmount())))
                .toList();
        return Order.restore(orderDO.getId(), orderDO.getOrderNo(), orderDO.getUserId(),
                OrderStatus.of(orderDO.getStatus()),
                Money.of(orderDO.getTotalAmount()), Money.of(orderDO.getDiscountAmount()),
                Money.of(orderDO.getPayAmount()),
                orderDO.getRemark(), items, orderDO.getGmtCreate(), orderDO.getGmtModified());
    }
}
