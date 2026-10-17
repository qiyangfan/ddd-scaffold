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
import java.util.UUID;

/**
 * 订单接口：只做参数校验、入参转换与返回体组装，禁止业务逻辑。
 * <p>路径上的主键用 {@link UUID} 声明：格式非法的 ID 在进入领域层之前就被转成 400，
 * 而不是带着任意字符串去查库（fail-closed，畸形入参不进业务链路）。</p>
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
    public R<OrderResponse> pay(@PathVariable("id") UUID id) {
        return R.ok(orderAssembler.toResponse(orderApplicationService.payOrder(id.toString())));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public R<OrderResponse> cancel(@PathVariable("id") UUID id) {
        return R.ok(orderAssembler.toResponse(orderApplicationService.cancelOrder(id.toString())));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderResponse> get(@PathVariable("id") UUID id) {
        return R.ok(orderAssembler.toResponse(orderApplicationService.getOrder(id.toString())));
    }

    @Operation(summary = "分页查询订单")
    @GetMapping
    public R<PageResult<OrderResponse>> page(@ParameterObject @Valid OrderPageRequest request) {
        OrderPageQuery query = new OrderPageQuery(request.userId(), request.status(), request.pageNo(), request.pageSize());
        return R.ok(orderAssembler.toResponsePage(orderApplicationService.pageOrders(query)));
    }
}
