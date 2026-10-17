package com.example.biz.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/** 订单已创建事件 */
public record OrderCreatedEvent(String orderId, String orderNo, Long userId, BigDecimal payAmount, Instant occurredAt)
        implements DomainEvent {
}
