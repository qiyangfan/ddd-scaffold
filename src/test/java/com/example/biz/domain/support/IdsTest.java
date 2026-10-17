package com.example.biz.domain.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 身份生成器单测：UUID v7 的布局正确性、唯一性与同毫秒单调性（纯 JDK，不启 Spring）。
 */
class IdsTest {

    @Test
    void generatedIdShouldBeUuidV7() {
        UUID id = Ids.uuidV7();

        // version = 7
        assertThat(id.version()).isEqualTo(7);
        // variant = RFC 4122（10b）
        assertThat(id.variant()).isEqualTo(2);
        // 时间戳在高 48 位：与当前毫秒接近（允许时钟回拨之外的极小偏差）
        long timestamp = id.getMostSignificantBits() >>> 16;
        assertThat(timestamp).isCloseTo(System.currentTimeMillis(),
                org.assertj.core.data.Offset.offset(5_000L));
    }

    @Test
    void stringFormShouldBeLowercaseCanonicalUuid() {
        String id = Ids.newId();

        assertThat(id).hasSize(36);
        assertThat(id).isEqualTo(id.toLowerCase());
        assertThat(UUID.fromString(id)).isNotNull();
    }

    @Test
    void generatedIdsShouldBeUnique() {
        List<String> ids = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            ids.add(Ids.newId());
        }

        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void idsShouldBeMonotonicWithinSameMillisecond() {
        // 同一毫秒内连续生成：rand_a 递增保证字典序不回退（UUID v7 可排序的关键）
        String first = Ids.newId();
        String second = Ids.newId();
        String third = Ids.newId();

        assertThat(second.compareTo(first)).isPositive();
        assertThat(third.compareTo(second)).isPositive();
    }
}
