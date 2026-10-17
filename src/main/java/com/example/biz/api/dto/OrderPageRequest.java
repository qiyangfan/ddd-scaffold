package com.example.biz.api.dto;

import com.example.biz.domain.support.PageLimits;
import com.example.biz.domain.valueobject.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 订单分页查询请求：pageSize 设上限防恶意大分页，pageNo 设上界防 offset 溢出。
 * <p>上限值取自领域侧单一事实源 {@link PageLimits}，与仓储层兜底钳制同源，避免改一处漏一处。</p>
 */
@Schema(description = "订单分页查询入参")
public record OrderPageRequest(

        @Schema(description = "用户 ID，精确匹配", example = "1000")
        Long userId,

        @Schema(description = "状态编码，不传则不过滤", example = "UNPAID",
                allowableValues = {"UNPAID", "PAID", "CANCELLED"})
        OrderStatus status,

        @Schema(description = "页码，从 1 开始", example = "1")
        @Min(value = 1, message = "pageNo must be greater than or equal to 1")
        @Max(value = PageLimits.MAX_PAGE_NO, message = "pageNo is out of supported range")
        Integer pageNo,

        @Schema(description = "每页条数，上限 200", example = "10")
        @Min(value = 1, message = "pageSize must be greater than or equal to 1")
        @Max(value = PageLimits.MAX_PAGE_SIZE, message = "pageSize must be less than or equal to 200")
        Integer pageSize) {
}
