package com.example.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 手搓轻量 DDD 架构脚手架启动类。
 * <p>分层与依赖规则：api -&gt; application -&gt; domain，infrastructure 依赖 domain（依赖倒置）。
 * domain 层零框架依赖，由 ArchUnit 架构守护测试强制约束。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BizApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizApplication.class, args);
    }
}
