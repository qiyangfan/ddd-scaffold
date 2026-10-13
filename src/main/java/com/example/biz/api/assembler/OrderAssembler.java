package com.example.biz.api.assembler;

import com.example.biz.api.dto.OrderResponse;
import com.example.biz.application.PageResult;
import com.example.biz.domain.aggregate.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单装配器（api 层）：聚合 -&gt; 响应 DTO 的唯一转换点，Money/枚举在此解包为标量，domain 类型不出 api 层。
 * <p>显式映射，禁止反射拷贝（手册：避免使用 BeanUtils.copyProperties）。</p>
 */
@Component
public class OrderAssembler {

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.productId(), item.productName(), item.unitPrice().amount(),
                        item.quantity(), item.lineAmount().amount()))
                .toList();
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getUserId(),
                order.getStatus().name(), order.getStatus().getDesc(),
                order.getTotalAmount().amount(), order.getDiscountAmount().amount(),
                order.getPayAmount().amount(), order.getRemark(), itemResponses, order.getCreatedAt());
    }

    public PageResult<OrderResponse> toResponsePage(PageResult<Order> orderPage) {
        List<OrderResponse> responses = orderPage.list().stream().map(this::toResponse).toList();
        return PageResult.of(responses, orderPage.total());
    }
}
