package com.example.biz.infrastructure.repository;

import com.example.biz.domain.aggregate.Order;
import com.example.biz.domain.repository.OrderRepository;
import com.example.biz.domain.support.PageLimits;
import com.example.biz.domain.valueobject.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 订单仓储实现：领域定义接口，这里用 MyBatis 落地——依赖倒置的 infrastructure 侧。
 * <p>主键由领域层生成，插入后无需回填；分页上限在 api 层已强校验，这里再兜底钳制一次。</p>
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final OrderMapper orderMapper;

    @Override
    public void save(Order order) {
        orderMapper.insertOrder(OrderConverter.toDO(order));
        List<OrderItemDO> itemDOList = OrderConverter.toItemDOList(order);
        if (!itemDOList.isEmpty()) {
            orderMapper.insertItems(itemDOList);
        }
    }

    @Override
    public boolean updateStatus(Order order, OrderStatus expectStatus) {
        return orderMapper.updateOrderStatus(order.getId(), expectStatus.getCode(),
                order.getStatus().getCode(), order.getUpdatedAt()) > 0;
    }

    @Override
    public Optional<Order> find(String id) {
        OrderDO orderDO = orderMapper.selectOrderById(id);
        if (orderDO == null) {
            return Optional.empty();
        }
        List<OrderItemDO> itemDOList = orderMapper.listItemsByOrderId(id);
        return Optional.of(OrderConverter.toAggregate(orderDO, itemDOList));
    }

    @Override
    public List<Order> page(OrderQuery query) {
        int pageNo = query.pageNo() == null ? DEFAULT_PAGE_NO : query.pageNo();
        int pageSize = query.pageSize() == null ? DEFAULT_PAGE_SIZE
                : Math.min(query.pageSize(), PageLimits.MAX_PAGE_SIZE);
        // long 乘法防止极端 pageNo 下 int 溢出为负 offset
        long offset = (long) (pageNo - DEFAULT_PAGE_NO) * pageSize;

        List<OrderDO> orderDOList = orderMapper.selectPage(query.userId(), statusCodeOf(query.status()),
                offset, pageSize);
        if (orderDOList.isEmpty()) {
            return List.of();
        }
        // 明细批量装载，避免逐单查询的 N+1
        List<String> orderIds = orderDOList.stream().map(OrderDO::getId).toList();
        Map<String, List<OrderItemDO>> itemMap = orderMapper.listItemsByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemDO::getOrderId));

        List<Order> result = new ArrayList<>(orderDOList.size());
        for (OrderDO orderDO : orderDOList) {
            result.add(OrderConverter.toAggregate(orderDO, itemMap.getOrDefault(orderDO.getId(), List.of())));
        }
        return result;
    }

    @Override
    public long count(OrderQuery query) {
        return orderMapper.countOrder(query.userId(), statusCodeOf(query.status()));
    }

    /** 状态入参转为持久化编码，null 表示不过滤 */
    private String statusCodeOf(OrderStatus status) {
        return status == null ? null : status.getCode();
    }
}
