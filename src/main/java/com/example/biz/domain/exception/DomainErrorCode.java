package com.example.biz.domain.exception;

/**
 * 领域错误码：code 沿用 A/B/C 分级思想（领域规则拒绝归入 B 系），message 为英文。
 */
public enum DomainErrorCode {

    /** 订单不存在 */
    ORDER_NOT_FOUND("B0201", "order not found"),
    /** 订单必须至少包含一条明细 */
    ORDER_EMPTY_ITEMS("B0202", "order must contain at least one item"),
    /** 订单应付金额非法 */
    ORDER_AMOUNT_ILLEGAL("B0203", "order pay amount must not be negative"),
    /** 当前订单状态不允许该操作 */
    ORDER_STATUS_ILLEGAL("B0204", "order status does not allow this operation"),
    /** 并发修改冲突（状态 CAS 失败），客户端可重试 */
    ORDER_CONCURRENT_MODIFY("B0208", "order state changed concurrently, please retry"),
    /** 明细数量非法 */
    ORDER_ITEM_QUANTITY_ILLEGAL("B0205", "item quantity must be positive"),
    /** 订单状态数据非法（脏数据修复场景） */
    ORDER_STATUS_UNKNOWN("B0206", "unknown order status code"),
    /** 创建订单参数非法 */
    ORDER_CREATE_ARGS_ILLEGAL("B0207", "order create arguments are illegal"),
    /** 商品不存在 */
    PRODUCT_NOT_FOUND("B0211", "product not found"),
    /** 商品已下架 */
    PRODUCT_OFF_SALE("B0212", "product is off sale");

    private final String code;
    private final String message;

    DomainErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
