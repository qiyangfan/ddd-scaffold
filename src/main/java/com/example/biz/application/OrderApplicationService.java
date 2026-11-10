package com.example.biz.application;

import com.example.biz.application.command.CreateOrderCommand;
import com.example.biz.application.command.OrderPageQuery;
import com.example.biz.domain.aggregate.Order;
import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.gateway.IdGenerator;
import com.example.biz.domain.gateway.ProductGateway;
import com.example.biz.domain.repository.OrderRepository;
import com.example.biz.domain.service.OrderPricingService;
import com.example.biz.domain.valueobject.OrderStatus;
import com.example.biz.domain.valueobject.ProductInfo;
import com.example.biz.domain.event.DomainEvent;
import com.example.biz.domain.event.OrderCancelledEvent;
import com.example.biz.domain.event.OrderCreatedEvent;
import com.example.biz.domain.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单应用服务：一个方法对应一个业务用例。负责事务、用例编排、领域事件发布，不写业务规则。
 * <p>返回领域聚合，响应 DTO 的组装在 api 层完成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    /** 业务单号前缀，属于订单格式语义；唯一性由 {@link IdGenerator} 端口提供 */
    private static final String ORDER_NO_PREFIX = "SO";

    private final OrderRepository orderRepository;
    private final ProductGateway productGateway;
    private final IdGenerator idGenerator;
    private final OrderPricingService orderPricingService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 用例：创建订单。
     * <p>注意：当前网关是内存演示实现，取数放在事务内无碍；替换为真实 RPC/HTTP 后，
     * 应把外部调用移到事务外（拆分前置取数或改用 TransactionTemplate），避免事务内长时间持有数据库连接。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderCommand command) {
        if (command.items() == null || command.items().isEmpty()) {
            throw new DomainException(DomainErrorCode.ORDER_EMPTY_ITEMS);
        }
        // 商品信息经网关（DIP 端口）获取，外部系统细节不进入领域层
        Map<Long, ProductInfo> productMap = new HashMap<>();
        for (CreateOrderCommand.Item item : command.items()) {
            productMap.computeIfAbsent(item.productId(), productGateway::getProduct);
        }
        List<OrderPricingService.OrderItemDraft> drafts = command.items().stream()
                .map(item -> new OrderPricingService.OrderItemDraft(item.productId(), item.quantity()))
                .toList();
        OrderPricingService.PricingResult pricing = orderPricingService.price(drafts, productMap);

        Order order = Order.create(nextOrderNo(), command.userId(), pricing, command.remark());
        orderRepository.save(order);

        publish(new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getUserId(),
                order.getPayAmount().amount(), Instant.now()));
        log.info("order created, orderId:{}, orderNo:{}, payAmount:{}",
                order.getId(), order.getOrderNo(), order.getPayAmount().amount());
        return order;
    }

    /** 用例：支付订单（DB 层状态 CAS 兜底并发，冲突转为业务码提示重试） */
    @Transactional(rollbackFor = Exception.class)
    public Order payOrder(String orderId) {
        Order order = requireOrder(orderId);
        OrderStatus expectStatus = order.getStatus();
        order.pay();
        casUpdate(order, expectStatus);
        publish(new OrderPaidEvent(order.getId(), order.getOrderNo(),
                order.getPayAmount().amount(), Instant.now()));
        log.info("order paid, orderId:{}, orderNo:{}", order.getId(), order.getOrderNo());
        return order;
    }

    /** 用例：取消订单（DB 层状态 CAS 兜底并发，冲突转为业务码提示重试） */
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(String orderId) {
        Order order = requireOrder(orderId);
        OrderStatus expectStatus = order.getStatus();
        order.cancel();
        casUpdate(order, expectStatus);
        publish(new OrderCancelledEvent(order.getId(), order.getOrderNo(), Instant.now()));
        log.info("order cancelled, orderId:{}, orderNo:{}", order.getId(), order.getOrderNo());
        return order;
    }

    /** 用例：查询订单详情 */
    public Order getOrder(String orderId) {
        return requireOrder(orderId);
    }

    /** 用例：分页查询订单 */
    public PageResult<Order> pageOrders(OrderPageQuery query) {
        OrderRepository.OrderQuery orderQuery =
                new OrderRepository.OrderQuery(query.userId(), query.status(), query.pageNo(), query.pageSize());
        List<Order> orders = orderRepository.page(orderQuery);
        long total = orderRepository.count(orderQuery);
        return PageResult.of(orders, total);
    }

    /** 状态落库 CAS：并发冲突时回滚事务并返回可重试的业务码 */
    private void casUpdate(Order order, OrderStatus expectStatus) {
        if (!orderRepository.updateStatus(order, expectStatus)) {
            throw new DomainException(DomainErrorCode.ORDER_CONCURRENT_MODIFY);
        }
    }

    private Order requireOrder(String orderId) {
        return orderRepository.find(orderId)
                .orElseThrow(() -> new DomainException(DomainErrorCode.ORDER_NOT_FOUND));
    }

    private void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    /** 订单号 = 业务前缀 + 唯一 ID；唯一性机制见 {@link IdGenerator} 实现，切换雪花/号段无需改用例 */
    private String nextOrderNo() {
        return ORDER_NO_PREFIX + idGenerator.nextId();
    }
}
