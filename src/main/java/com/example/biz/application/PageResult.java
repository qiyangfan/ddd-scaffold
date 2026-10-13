package com.example.biz.application;

import java.util.List;

/**
 * 应用层分页返回容器：record 自动实现 equals/hashCode，列表不可变。
 */
public record PageResult<T>(long total, List<T> list) {

    public PageResult {
        list = list == null ? List.of() : List.copyOf(list);
    }

    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(total, list);
    }
}
