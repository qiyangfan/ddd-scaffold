package com.example.biz.infrastructure.config;

import com.example.biz.domain.service.OrderPricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域服务装配：领域层保持零 Spring 依赖，无状态领域服务在此声明为 Bean 供应用层注入。
 */
@Configuration
public class DomainConfig {

    @Bean
    public OrderPricingService orderPricingService() {
        return new OrderPricingService();
    }
}
