package com.example.biz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建订单请求：校验 message 一律英文（国际化）。
 */
@Schema(description = "创建订单入参")
public record OrderCreateRequest(

        @Schema(description = "用户 ID", example = "1000")
        @NotNull(message = "userId must not be null")
        Long userId,

        @Schema(description = "订单明细")
        @NotEmpty(message = "items must not be empty")
        @Valid
        List<OrderItemRequest> items,

        @Schema(description = "备注", example = "deliver before Friday")
        @Size(max = 255, message = "remark length must be less than or equal to 255")
        String remark) {

    @Schema(description = "订单明细项")
    public record OrderItemRequest(

            @Schema(description = "商品 ID", example = "1")
            @NotNull(message = "productId must not be null")
            @Min(value = 1, message = "productId must be greater than or equal to 1")
            Long productId,

            @Schema(description = "数量", example = "2")
            @NotNull(message = "quantity must not be null")
            @Min(value = 1, message = "quantity must be greater than or equal to 1")
            Integer quantity) {
    }
}
