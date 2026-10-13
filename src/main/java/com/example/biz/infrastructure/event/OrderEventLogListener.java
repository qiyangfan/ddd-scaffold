package com.example.biz.infrastructure.event;

import com.example.biz.domain.event.OrderCancelledEvent;
import com.example.biz.domain.event.OrderCreatedEvent;
import com.example.biz.domain.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 领域事件消费演示：事务提交后（AFTER_COMMIT）才执行，避免读到未提交数据。
 * <p>事件目前是进程内的（Spring 事件总线）；多实例/跨服务场景把监听器替换为 MQ 生产者，
 * 或在应用层将 publish 通道换成事务消息，语义不变。</p>
 */
@Slf4j
@Component
public class OrderEventLogListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("order created event consumed, orderId:{}, orderNo:{}, payAmount:{}",
                event.orderId(), event.orderNo(), event.payAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("order paid event consumed, orderId:{}, orderNo:{}, payAmount:{}",
                event.orderId(), event.orderNo(), event.payAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("order cancelled event consumed, orderId:{}, orderNo:{}",
                event.orderId(), event.orderNo());
    }
}
