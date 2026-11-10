package com.example.biz.domain.gateway;

/**
 * 唯一 ID 端口：生成跨实例唯一的业务 ID（雪花算法、号段模式等）。
 * <p>只抽象唯一性机制，不含业务前缀等格式语义，格式由用例层拼装；
 * 演示实现为 DemoIdGenerator，生产环境替换为雪花算法或分布式号段实现。</p>
 */
@FunctionalInterface
public interface IdGenerator {

    /** 生成一个全局唯一的业务 ID 字符串 */
    String nextId();
}
