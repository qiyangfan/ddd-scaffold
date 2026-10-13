package com.example.biz.domain.aggregate;

import com.example.biz.domain.entity.OrderItem;
import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;
import com.example.biz.domain.service.OrderPricingService;
import com.example.biz.domain.valueobject.Money;
import com.example.biz.domain.valueobject.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单聚合根：一致性与状态机的边界，全部业务规则内聚在此，禁止外部直接改状态字段。
 * <p>状态机：UNPAID -&gt; PAID（支付）/ UNPAID -&gt; CANCELLED（取消），终态不可逆。</p>
 */
public final class Order {

    private Long id;
    private final String orderNo;
    private final Long userId;
    private OrderStatus status;
    private final Money totalAmount;
    private final Money discountAmount;
    private final Money payAmount;
    private final String remark;
    private final List<OrderItem> items;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order(Long id, String orderNo, Long userId, OrderStatus status, Money totalAmount,
                  Money discountAmount, Money payAmount, String remark, List<OrderItem> items,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.payAmount = payAmount;
        this.remark = remark;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建订单：领域工厂方法，入参为领域服务产出的定价结果，创建时即完成全部不变量校验。
     */
    public static Order create(String orderNo, Long userId, OrderPricingService.PricingResult pricing, String remark) {
        if (orderNo == null || orderNo.isBlank() || userId == null || pricing == null) {
            throw new DomainException(DomainErrorCode.ORDER_CREATE_ARGS_ILLEGAL);
        }
        if (pricing.items().isEmpty()) {
            throw new DomainException(DomainErrorCode.ORDER_EMPTY_ITEMS);
        }
        if (pricing.payAmount().isNegative()) {
            throw new DomainException(DomainErrorCode.ORDER_AMOUNT_ILLEGAL);
        }
        LocalDateTime now = LocalDateTime.now();
        return new Order(null, orderNo, userId, OrderStatus.UNPAID, pricing.totalAmount(),
                pricing.discountAmount(), pricing.payAmount(), remark, pricing.items(), now, now);
    }

    /**
     * 仅供仓储实现重建聚合，业务代码禁止调用。
     */
    public static Order restore(Long id, String orderNo, Long userId, OrderStatus status, Money totalAmount,
                                Money discountAmount, Money payAmount, String remark, List<OrderItem> items,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, orderNo, userId, status, totalAmount, discountAmount, payAmount, remark, items, createdAt, updatedAt);
    }

    /** 支付：仅待支付订单可支付 */
    public void pay() {
        ensureUnpaid("only unpaid order can be paid");
        this.status = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    /** 取消：仅待支付订单可取消，已支付订单走退款流程 */
    public void cancel() {
        ensureUnpaid("only unpaid order can be cancelled");
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 仅供仓储实现在插入后回填数据库自增主键，业务代码禁止调用。
     */
    public void attachId(Long id) {
        this.id = id;
    }

    private void ensureUnpaid(String message) {
        if (this.status != OrderStatus.UNPAID) {
            throw new DomainException(DomainErrorCode.ORDER_STATUS_ILLEGAL,
                    "order status is " + this.status.name() + ", " + message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getDiscountAmount() {
        return discountAmount;
    }

    public Money getPayAmount() {
        return payAmount;
    }

    public String getRemark() {
        return remark;
    }

    /** 返回不可变视图，防止外部绕过聚合修改明细 */
    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
