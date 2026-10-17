package com.example.biz.domain.event;

import java.time.Instant;

/** 订单已取消事件 */
public record OrderCancelledEvent(String orderId, String orderNo, Instant occurredAt) implements DomainEvent {
}
