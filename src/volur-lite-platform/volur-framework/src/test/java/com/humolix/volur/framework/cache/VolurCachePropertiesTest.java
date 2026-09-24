package com.humolix.volur.framework.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VolurCacheProperties} 的默认值合并与 TTL 抖动。
 *
 * <p>抖动是防雪崩的唯一手段，但"抖动"很容易被实现成"无效抖动"（例如所有 key 拿到同一个值，
 * 或抖动幅度越界），因此这里用统计方式验证而不是只看单个值。
 */
class VolurCachePropertiesTest {

    @Test
    @DisplayName("未配置覆盖项时，全部继承全局默认值")
    void inheritsGlobalDefaults() {
        VolurCacheProperties props = new VolurCacheProperties();

        VolurCacheProperties.Resolved spec = props.resolve("any_cache");

        assertThat(spec.l1Enabled()).isTrue();
        assertThat(spec.l1MaxSize()).isEqualTo(10_000L);
        assertThat(spec.l1Ttl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(spec.l2Enabled()).isTrue();
        assertThat(spec.l2Ttl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(spec.l2NullTtl()).isEqualTo(Duration.ofSeconds(60));
        assertThat(spec.nullCaching()).isTrue();
        // 分布式锁默认关闭：Caffeine 单飞已解决单实例击穿，开锁只增加尾延迟
        assertThat(spec.distributedLock()).isFalse();
    }

    @Test
    @DisplayName("全局 L1 关闭时，单缓存无法把它打开（避免配置自相矛盾）")
    void globalL1SwitchWinsOverPerCacheOverride() {
        VolurCacheProperties props = new VolurCacheProperties();
        props.getL1().setEnabled(false);

        VolurCacheProperties.CacheSpec spec = new VolurCacheProperties.CacheSpec();
        spec.setL1Enabled(true);
        props.getCaches().put("c", spec);

        // 全局是"总闸"，单缓存只能在其基础上收窄，不能反向放大
        assertThat(props.resolve("c").l1Enabled()).isFalse();
    }

    @Test
    @DisplayName("单缓存覆盖项优先于全局默认值")
    void perCacheOverrideWins() {
        VolurCacheProperties props = new VolurCacheProperties();
        VolurCacheProperties.CacheSpec spec = new VolurCacheProperties.CacheSpec();
        spec.setL1Enabled(false);
        spec.setL2Ttl(Duration.ofSeconds(10));
        spec.setL2NullTtl(Duration.ofSeconds(3));
        spec.setNullCaching(false);
        spec.setDistributedLock(true);
        props.getCaches().put("competition_status", spec);

        VolurCacheProperties.Resolved resolved = props.resolve("competition_status");

        assertThat(resolved.l1Enabled()).isFalse();
        assertThat(resolved.l2Ttl()).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.l2NullTtl()).isEqualTo(Duration.ofSeconds(3));
        assertThat(resolved.nullCaching()).isFalse();
        assertThat(resolved.distributedLock()).isTrue();
        // 未覆盖的项仍继承全局值
        assertThat(resolved.l1MaxSize()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("key 命名空间符合 vcache:<cache>:<key> 约定")
    void keyNaming() {
        VolurCacheProperties props = new VolurCacheProperties();
        VolurCacheProperties.Resolved spec = props.resolve("user_details");

        assertThat(spec.l2Key(123)).isEqualTo("vcache:user_details:123");
        assertThat(spec.l2KeyPattern()).isEqualTo("vcache:user_details:*");
        assertThat(spec.lockKey(123)).isEqualTo("vcache:lock:user_details:123");
    }

    @Test
    @DisplayName("TTL 抖动：全部落在 ±10% 内，且确实分散（不是无效抖动）")
    void ttlJitterIsBoundedAndActuallyVaries() {
        VolurCacheProperties props = new VolurCacheProperties();
        props.getL2().setTtlJitterRatio(0.1D);
        VolurCacheProperties.Resolved spec = props.resolve("c");

        Duration base = spec.l2Ttl();
        long baseMs = base.toMillis();
        long lower = baseMs - (long) (baseMs * 0.1D);
        long upper = baseMs + (long) (baseMs * 0.1D);

        Set<Long> observed = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            long ms = spec.effectiveL2Ttl(false).toMillis();
            assertThat(ms).as("抖动越界").isBetween(lower, upper);
            observed.add(ms);
        }

        // 若抖动实现退化成"返回原值"，observed 只会有一个元素，防雪崩就失效了
        assertThat(observed).as("抖动样本数（应远大于 1）").hasSizeGreaterThan(50);
    }

    @Test
    @DisplayName("TTL 抖动：null 占位走独立且更短的 base，不与正常值混用")
    void nullTtlUsesItsOwnBase() {
        VolurCacheProperties props = new VolurCacheProperties();
        VolurCacheProperties.Resolved spec = props.resolve("c");

        long nullTtlMs = spec.l2NullTtl().toMillis();
        for (int i = 0; i < 200; i++) {
            long ms = spec.effectiveL2Ttl(true).toMillis();
            assertThat(ms).isBetween(nullTtlMs - nullTtlMs / 10, nullTtlMs + nullTtlMs / 10);
        }
    }

    @Test
    @DisplayName("抖动比例为 0 或负时返回原值，不做无意义计算")
    void zeroJitterReturnsBase() {
        VolurCacheProperties props = new VolurCacheProperties();
        props.getL2().setTtlJitterRatio(0D);
        VolurCacheProperties.Resolved spec = props.resolve("c");

        assertThat(spec.effectiveL2Ttl(false)).isEqualTo(spec.l2Ttl());
        assertThat(spec.effectiveL2Ttl(true)).isEqualTo(spec.l2NullTtl());
    }

    @Test
    @DisplayName("抖动不会把 TTL 算成 0 或负数")
    void jitterNeverProducesNonPositiveTtl() {
        VolurCacheProperties props = new VolurCacheProperties();
        // 极短 TTL + 大抖动比例：容易在朴素实现里算出 0 或负数
        props.getL2().setDefaultTtl(Duration.ofMillis(5));
        props.getL2().setTtlJitterRatio(0.9D);
        VolurCacheProperties.Resolved spec = props.resolve("c");

        for (int i = 0; i < 300; i++) {
            assertThat(spec.effectiveL2Ttl(false).toMillis()).isPositive();
        }
    }
}
