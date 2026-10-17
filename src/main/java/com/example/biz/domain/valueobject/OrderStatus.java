package com.example.biz.domain.valueobject;

import com.example.biz.domain.exception.DomainErrorCode;
import com.example.biz.domain.exception.DomainException;

/**
 * 订单状态值对象：状态机 UNPAID -&gt; PAID / CANCELLED，终态不可逆。
 * <p>持久化值为状态名字符串（{@code status VARCHAR + CHECK}），与数据库 CHECK 约束共用同一份清单——
 * 不再用 TINYINT 数字码，避免"1=正常 0=禁用"这类只能靠注释解读的脏数据（见 ADR-0007）。</p>
 */
public enum OrderStatus {

    /** 待支付 */
    UNPAID("UNPAID", "待支付"),
    /** 已支付 */
    PAID("PAID", "已支付"),
    /** 已取消 */
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 持久化编码：与 {@link #name()} 一致，数据库 CHECK 约束以此为准 */
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 由持久化编码还原枚举，未知编码视为脏数据（不静默兜底） */
    public static OrderStatus of(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new DomainException(DomainErrorCode.ORDER_STATUS_UNKNOWN);
    }
}
