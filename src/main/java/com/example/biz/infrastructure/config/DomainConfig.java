package com.example.biz.infrastructure.config;

import com.example.biz.domain.service.OrderPricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 领域服务装配：领域层保持零 Spring 依赖，无状态领域服务在此声明为 Bean 供应用层注入。
 */
@Configuration
public class DomainConfig {

    @Bean
    public OrderPricingService orderPricingService() {
        return new OrderPricingService();
    }

    /** 业务时钟：生产取系统时钟，测试可替换为固定时钟，业务代码一律经注入 Clock 取时刻 */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
