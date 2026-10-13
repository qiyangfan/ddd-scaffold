package com.example.biz.application.command;

import java.util.List;

/**
 * 创建订单命令：应用层入参模型，由 api 层从请求 DTO 转换而来。
 */
public record CreateOrderCommand(Long userId, List<Item> items, String remark) {

    /** 明细项购买意向 */
    public record Item(Long productId, Integer quantity) {
    }
}
