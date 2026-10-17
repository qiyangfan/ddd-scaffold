package com.example.biz.infrastructure.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper：方法命名遵循手册 get/list/count/insert/update 规约；只服务仓储实现，禁止上层直接调用。
 * <p>主键由应用侧生成，插入不使用 useGeneratedKeys（见 ADR-0006）。</p>
 */
@Mapper
public interface OrderMapper {

    /** 新增订单，主键已由领域层生成 */
    int insertOrder(OrderDO orderDO);

    /** 批量新增订单明细 */
    int insertItems(@Param("items") List<OrderItemDO> items);

    /** 按主键查询订单 */
    OrderDO selectOrderById(@Param("id") String id);

    /** 查询单个订单的明细 */
    List<OrderItemDO> listItemsByOrderId(@Param("orderId") String orderId);

    /** 批量查询多个订单的明细（分页组装用，避免 N+1） */
    List<OrderItemDO> listItemsByOrderIds(@Param("orderIds") List<String> orderIds);

    /** 乐观更新订单状态（CAS：仅当当前状态等于 fromStatus 时生效），返回受影响行数 */
    int updateOrderStatus(@Param("id") String id, @Param("fromStatus") String fromStatus,
                          @Param("toStatus") String toStatus, @Param("gmtModified") LocalDateTime gmtModified);

    /** 条件分页查询，物理分页 LIMIT */
    List<OrderDO> selectPage(@Param("userId") Long userId, @Param("status") String status,
                             @Param("offset") long offset, @Param("size") int size);

    /** 条件计数 */
    long countOrder(@Param("userId") Long userId, @Param("status") String status);
}
