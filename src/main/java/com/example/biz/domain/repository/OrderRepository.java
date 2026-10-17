package com.example.biz.domain.repository;

import com.example.biz.domain.aggregate.Order;
import com.example.biz.domain.valueobject.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储接口：只定义"要什么数据"，不关心怎么存取；由 infrastructure 层依赖倒置实现。
 */
public interface OrderRepository {

    /** 新增订单（含明细）。主键由领域层生成，仓储不做任何回填 */
    void save(Order order);

    /**
     * 乐观更新订单状态（CAS）：期望聚合此前处于 expectStatus，迁移至聚合当前状态。
     * 并发冲突（状态已被其他实例改写）返回 false，调用方应转为业务冲突提示。
     */
    boolean updateStatus(Order order, OrderStatus expectStatus);

    /** 按主键加载订单聚合（含明细），不存在返回 empty */
    Optional<Order> find(String id);

    /** 条件分页查询订单聚合（含明细），按创建时间倒序 */
    List<Order> page(OrderQuery query);

    /** 条件计数 */
    long count(OrderQuery query);

    /** 订单查询条件（领域层查询对象） */
    record OrderQuery(Long userId, OrderStatus status, Integer pageNo, Integer pageSize) {
    }
}
