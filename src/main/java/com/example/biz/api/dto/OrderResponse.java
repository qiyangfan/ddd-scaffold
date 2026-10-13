package com.example.biz.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应：由 api 层装配器从聚合生成，domain 类型不外泄。
 */
@Schema(description = "订单详情")
public record OrderResponse(

        @Schema(description = "订单 ID", example = "1")
        Long id,

        @Schema(description = "订单号", example = "SO202609211200001234")
        String orderNo,

        @Schema(description = "用户 ID", example = "1000")
        Long userId,

        @Schema(description = "状态码名", example = "UNPAID")
        String status,

        @Schema(description = "状态描述", example = "待支付")
        String statusDesc,

        @Schema(description = "商品总额", example = "11998.00")
        BigDecimal totalAmount,

        @Schema(description = "优惠金额", example = "80.00")
        BigDecimal discountAmount,

        @Schema(description = "应付金额", example = "11918.00")
        BigDecimal payAmount,

        @Schema(description = "备注", example = "deliver before Friday")
        String remark,

        @Schema(description = "明细列表")
        List<OrderItemResponse> items,

        @Schema(description = "创建时间", example = "2026-09-21 12:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt) {

    @Schema(description = "订单明细项")
    public record OrderItemResponse(

            @Schema(description = "商品 ID", example = "2")
            Long productId,

            @Schema(description = "商品名（下单时刻快照）", example = "Laptop Pro 14")
            String productName,

            @Schema(description = "单价", example = "5999.00")
            BigDecimal unitPrice,

            @Schema(description = "数量", example = "2")
            Integer quantity,

            @Schema(description = "行小计", example = "11998.00")
            BigDecimal lineAmount) {
    }
}
