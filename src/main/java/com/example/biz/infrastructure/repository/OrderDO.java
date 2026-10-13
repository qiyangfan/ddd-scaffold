package com.example.biz.infrastructure.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 DO：与 t_order 表结构一一对应，仅存在于 infrastructure 层。
 */
@Data
public class OrderDO {

    /** 主键 */
    private Long id;
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
    /** 状态：0 待支付 1 已支付 2 已取消 */
    private Integer status;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime gmtCreate;
    /** 修改时间 */
    private LocalDateTime gmtModified;
}
