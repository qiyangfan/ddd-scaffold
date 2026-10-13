package com.example.biz.domain.valueobject;

import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;

import java.util.Objects;

/**
 * 订单状态值对象：状态机 UNPAID -&gt; PAID / CANCELLED，终态不可逆。
 */
public enum OrderStatus {

    /** 待支付 */
    UNPAID(0, "待支付"),
    /** 已支付 */
    PAID(1, "已支付"),
    /** 已取消 */
    CANCELLED(2, "已取消");

    /** 状态码下界，用于入参快速校验 */
    public static final int MIN_CODE = 0;
    /** 状态码上界 */
    public static final int MAX_CODE = 2;

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 由持久化状态码还原枚举，未知编码视为脏数据 */
    public static OrderStatus of(Integer code) {
        for (OrderStatus status : values()) {
            if (Objects.equals(status.code, code)) {
                return status;
            }
        }
        throw new DomainException(DomainErrorCode.ORDER_STATUS_UNKNOWN);
    }
}
