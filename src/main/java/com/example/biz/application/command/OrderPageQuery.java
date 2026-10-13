package com.example.biz.application.command;

/**
 * 订单分页查询命令：api 层从请求参数转换而来，pageNo/pageSize 为空时由仓储实现取默认值。
 */
public record OrderPageQuery(Long userId, Integer status, Integer pageNo, Integer pageSize) {
}
