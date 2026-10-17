package com.example.biz.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/** 订单已支付事件 */
public record OrderPaidEvent(String orderId, String orderNo, BigDecimal payAmount, Instant occurredAt)
        implements DomainEvent {
}
