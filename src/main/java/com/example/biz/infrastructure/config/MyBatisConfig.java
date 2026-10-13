package com.example.biz.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置：Mapper 接口只存在于 infrastructure 层，扫描范围与其对齐。
 */
@Configuration
@MapperScan("com.example.biz.infrastructure.repository")
public class MyBatisConfig {
}
