package com.example.biz.api.controller;

import com.example.biz.api.assembler.OrderAssembler;
import com.example.biz.api.dto.OrderCreateRequest;
import com.example.biz.api.dto.OrderPageRequest;
import com.example.biz.api.dto.OrderResponse;
import com.example.biz.api.web.R;
import com.example.biz.application.OrderApplicationService;
import com.example.biz.application.PageResult;
import com.example.biz.application.command.CreateOrderCommand;
import com.example.biz.application.command.OrderPageQuery;
import com.example.biz.domain.aggregate.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口：只做参数校验、入参转换与返回体组装，禁止业务逻辑。
 */
@Tag(name = "订单管理")
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderApplicationService;
    private final OrderAssembler orderAssembler;

    @Operation(summary = "创建订单")
    @PostMapping
    public R<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        List<CreateOrderCommand.Item> items = request.items().stream()
                .map(item -> new CreateOrderCommand.Item(item.productId(), item.quantity()))
                .toList();
        Order order = orderApplicationService.createOrder(new CreateOrderCommand(request.userId(), items, request.remark()));
        return R.ok(orderAssembler.toResponse(order));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public R<OrderResponse> pay(@PathVariable("id") @Min(value = 1, message = "id must be greater than or equal to 1") Long id) {
        return R.ok(orderAssembler.toResponse(orderApplicationService.payOrder(id)));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public R<OrderResponse> cancel(@PathVariable("id") @Min(value = 1, message = "id must be greater than or equal to 1") Long id) {
        return R.ok(orderAssembler.toResponse(orderApplicationService.cancelOrder(id)));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderResponse> get(@PathVariable("id") @Min(value = 1, message = "id must be greater than or equal to 1") Long id) {
        return R.ok(orderAssembler.toResponse(orderApplicationService.getOrder(id)));
    }

    @Operation(summary = "分页查询订单")
    @GetMapping
    public R<PageResult<OrderResponse>> page(@ParameterObject @Valid OrderPageRequest request) {
        OrderPageQuery query = new OrderPageQuery(request.userId(), request.status(), request.pageNo(), request.pageSize());
        return R.ok(orderAssembler.toResponsePage(orderApplicationService.pageOrders(query)));
    }
}
