package com.example.biz.infrastructure.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 DO：与 t_order 表结构一一对应，仅存在于 infrastructure 层。
 * <p>主键为 UUID v7 字符串（应用侧生成，不使用数据库自增），状态为编码字符串（见 ADR-0006 / ADR-0007）。</p>
 */
@Data
public class OrderDO {

    /** 主键：UUID v7 字符串 */
    private String id;
    /** 订单号 */
    private String orderNo;
    /** 用户 ID */
    private Long userId;
    /** 商品总额 */
    private BigDecimal totalAmount;
    /** 优惠金额 */
    private BigDecimal discountAmount;
    /** 应付金额 */
    private BigDecimal payAmount;
    /** 状态编码：UNPAID / PAID / CANCELLED，库表 CHECK 约束同一份清单 */
    private String status;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime gmtCreate;
    /** 修改时间 */
    private LocalDateTime gmtModified;
}
