package com.example.biz.infrastructure.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细 DO：与 t_order_item 表结构一一对应，仅存在于 infrastructure 层。
 */
@Data
public class OrderItemDO {

    /** 主键 */
    private Long id;
    /** 订单 ID */
    private Long orderId;
    /** 商品 ID */
    private Long productId;
    /** 商品名（下单时刻快照） */
    private String productName;
    /** 单价 */
    private BigDecimal unitPrice;
    /** 数量 */
    private Integer quantity;
    /** 行小计 */
    private BigDecimal lineAmount;
    /** 创建时间（与订单一致） */
    private LocalDateTime gmtCreate;
    /** 修改时间（与订单一致） */
    private LocalDateTime gmtModified;
}
