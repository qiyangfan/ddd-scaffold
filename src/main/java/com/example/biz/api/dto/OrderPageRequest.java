package com.example.biz.api.dto;

import com.example.biz.domain.valueobject.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 订单分页查询请求：pageSize 设上限防恶意大分页，pageNo 设上界防 offset 溢出。
 */
@Schema(description = "订单分页查询入参")
public record OrderPageRequest(

        @Schema(description = "用户 ID，精确匹配", example = "1000")
        Long userId,

        @Schema(description = "状态：0 待支付 1 已支付 2 已取消", example = "0")
        @Min(value = OrderStatus.MIN_CODE, message = "status must be 0, 1 or 2")
        @Max(value = OrderStatus.MAX_CODE, message = "status must be 0, 1 or 2")
        Integer status,

        @Schema(description = "页码，从 1 开始", example = "1")
        @Min(value = 1, message = "pageNo must be greater than or equal to 1")
        @Max(value = 10000, message = "pageNo must be less than or equal to 10000")
        Integer pageNo,

        @Schema(description = "每页条数，上限 200", example = "10")
        @Min(value = 1, message = "pageSize must be greater than or equal to 1")
        @Max(value = 200, message = "pageSize must be less than or equal to 200")
        Integer pageSize) {
}
