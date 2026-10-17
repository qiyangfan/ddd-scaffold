package com.example.biz.api.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 序列化配置（Boot 4 默认 Jackson 3，包名 tools.jackson）。
 * <p>注意与主键策略的关系：聚合主键已是 UUID v7 字符串（ADR-0006），不再依赖本配置兜底；
 * 这里仍然全局转字符串，是为了兜住剩余的 64 位长整型——用户 ID、汇总计数、以及未来引入的
 * 雪花/号段类业务编号。规则简单且不需要逐个字段判断类型。</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Long 序列化为字符串：JS Number 安全整数上限为 2^53-1，64 位长整型直接传给前端会丢精度。
     */
    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule longToStringModule = new SimpleModule("longToStringModule");
            longToStringModule.addSerializer(Long.class, ToStringSerializer.instance);
            longToStringModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.addModule(longToStringModule);
        };
    }
}
