package com.example.biz.infrastructure.gateway;

import com.example.biz.domain.gateway.IdGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 唯一 ID 演示实现：时间戳 + 4 位随机段，开箱可用。
 * <p>多实例并发下存在碰撞风险；生产环境用雪花算法或号段模式实现 {@link IdGenerator} 并替换本类（移除 {@code @Component}）。</p>
 */
@Component
public class DemoIdGenerator implements IdGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RANDOM_BOUND = 10000;

    @Override
    public String nextId() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(RANDOM_BOUND));
    }
}
