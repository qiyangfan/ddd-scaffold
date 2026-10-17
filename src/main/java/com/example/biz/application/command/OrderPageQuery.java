package com.example.biz.application.command;

import com.example.biz.domain.valueobject.OrderStatus;

/**
 * 订单分页查询命令：api 层从请求参数转换而来，pageNo/pageSize 为空时由仓储实现取默认值。
 * <p>status 直接用领域枚举：非法值在 api 层就地被 Spring 转换为 400，不会带着脏值进入领域层。</p>
 */
public record OrderPageQuery(Long userId, OrderStatus status, Integer pageNo, Integer pageSize) {
}
