package com.example.biz.infrastructure.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细 DO：与 t_order_item 表结构一一对应，仅存在于 infrastructure 层。
 * <p>明细主键同样由应用侧生成（明细 ID 是持久化细节，由转换器在落库前统一分配）。</p>
 */
@Data
public class OrderItemDO {

    /** 主键：UUID v7 字符串 */
    private String id;
    /** 订单 ID */
    private String orderId;
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
