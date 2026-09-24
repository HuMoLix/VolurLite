package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster;
import com.humolix.volur.framework.cache.redis.RedisOps;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link VolurCacheMetricsBinder} 的指标注册契约。
 *
 * <p>用 {@link SimpleMeterRegistry}（Micrometer 自带的内存实现）验证：
 * 声明的指标名是否真的被注册、标签是否正确、以及<b>数值是否随业务活动变化</b>。
 * 最后一点尤其重要——{@code FunctionCounter} 的经典错误是绑定到一个永远返回 0 的函数上，
 * 那样指标"存在但永远是 0"，比没有指标更误导人。
 */
class VolurCacheMetricsBinderTest {

    private VolurCacheManager cacheManager;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        CacheEvictBroadcaster broadcaster = mock(CacheEvictBroadcaster.class);
        when(broadcaster.broadcastEvict(anyString(), anyString())).thenReturn(true);
        when(broadcaster.broadcastClear(anyString())).thenReturn(true);

        cacheManager = new VolurCacheManager(new VolurCacheProperties(), redis,
                mock(RedisOps.class), broadcaster);
        // @PostConstruct 方法，此处显式调用以模拟容器行为
        cacheManager.preCreateDeclaredCaches();
    }

    private MeterRegistry bind() {
        MeterRegistry registry = new SimpleMeterRegistry();
        new VolurCacheMetricsBinder(cacheManager).bindTo(registry);
        return registry;
    }

    @Test
    @DisplayName("每个声明的缓存都注册了完整的指标集合")
    void registersFullMeterSetPerDeclaredCache() {
        MeterRegistry registry = bind();

        assertThat(CacheNames.ALL).isNotEmpty();
        for (String name : CacheNames.ALL) {
            assertThat(registry.find("volur.cache.l1.size").tag("cache", name).gauge())
                    .as("%s: l1.size", name).isNotNull();
            assertThat(registry.find("volur.cache.l1.gets").tag("cache", name).tag("result", "hit")
                    .functionCounter()).as("%s: l1.gets hit", name).isNotNull();
            assertThat(registry.find("volur.cache.l1.gets").tag("cache", name).tag("result", "miss")
                    .functionCounter()).as("%s: l1.gets miss", name).isNotNull();
            assertThat(registry.find("volur.cache.l2.gets").tag("cache", name).tag("result", "hit")
                    .functionCounter()).as("%s: l2.gets hit", name).isNotNull();
            assertThat(registry.find("volur.cache.l2.gets").tag("cache", name).tag("result", "miss")
                    .functionCounter()).as("%s: l2.gets miss", name).isNotNull();
            assertThat(registry.find("volur.cache.l2.writes").tag("cache", name).functionCounter())
                    .as("%s: l2.writes", name).isNotNull();
            assertThat(registry.find("volur.cache.l2.errors").tag("cache", name).functionCounter())
                    .as("%s: l2.errors", name).isNotNull();
            assertThat(registry.find("volur.cache.loader.calls").tag("cache", name).functionCounter())
                    .as("%s: loader.calls", name).isNotNull();
            assertThat(registry.find("volur.cache.loader.duration").tag("cache", name).functionCounter())
                    .as("%s: loader.duration", name).isNotNull();
            assertThat(registry.find("volur.cache.puts").tag("cache", name).functionCounter())
                    .as("%s: puts", name).isNotNull();
            assertThat(registry.find("volur.cache.null.placeholders").tag("cache", name).functionCounter())
                    .as("%s: null.placeholders", name).isNotNull();
            assertThat(registry.find("volur.cache.invalidation.failed").tag("cache", name).functionCounter())
                    .as("%s: invalidation.failed", name).isNotNull();
            assertThat(registry.find("volur.cache.decode.failures").tag("cache", name).functionCounter())
                    .as("%s: decode.failures", name).isNotNull();
        }
    }

    @Test
    @DisplayName("指标数值随真实缓存活动变化（不是恒为 0 的死指标）")
    void countersReflectRealActivity() {
        MeterRegistry registry = bind();
        String cache = CacheNames.USER_DETAILS;

        VolurCache target = cacheManager.volurCache(cache).orElseThrow();

        when(valueOps.get(anyString())).thenReturn(null);
        target.get("1", () -> "alice");
        target.get("2", () -> "bob");

        assertThat(registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count()).isEqualTo(2.0D);
        assertThat(registry.get("volur.cache.l2.writes").tag("cache", cache)
                .functionCounter().count()).isEqualTo(2.0D);
        assertThat(registry.get("volur.cache.l2.gets").tag("cache", cache).tag("result", "miss")
                .functionCounter().count()).isEqualTo(2.0D);

        // 回源累计耗时必须为正（证明 finally 里的计时真的生效了）
        assertThat(registry.get("volur.cache.loader.duration").tag("cache", cache)
                .functionCounter().count()).isPositive();
    }

    @Test
    @DisplayName("L1 命中会体现在 l1.gets 上，且 loader 不再增长")
    void l1HitsAreVisible() {
        MeterRegistry registry = bind();
        String cache = CacheNames.USER_DETAILS;
        VolurCache target = cacheManager.volurCache(cache).orElseThrow();

        when(valueOps.get(anyString())).thenReturn(null);
        target.get("1", () -> "alice");
        target.get("1", () -> "alice");
        target.get("1", () -> "alice");

        assertThat(registry.get("volur.cache.l1.gets").tag("cache", cache).tag("result", "hit")
                .functionCounter().count()).isEqualTo(2.0D);
        assertThat(registry.get("volur.cache.l1.gets").tag("cache", cache).tag("result", "miss")
                .functionCounter().count()).isEqualTo(1.0D);
        assertThat(registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count()).isEqualTo(1.0D);
        assertThat(registry.get("volur.cache.l1.size").tag("cache", cache).gauge().value())
                .isEqualTo(1.0D);
    }

    @Test
    @DisplayName("L2 故障会体现在 l2.errors 上——把'静默降级'变成可观测信号")
    void l2ErrorsAreVisible() {
        MeterRegistry registry = bind();
        String cache = CacheNames.USER_DETAILS;
        VolurCache target = cacheManager.volurCache(cache).orElseThrow();

        when(valueOps.get(anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("down"));

        assertThat(target.get("1", () -> "fallback")).isEqualTo("fallback");
        assertThat(registry.get("volur.cache.l2.errors").tag("cache", cache)
                .functionCounter().count()).isPositive();
    }

    @Test
    @DisplayName("未声明但被动态创建的缓存不会导致绑定失败")
    void dynamicallyCreatedCacheDoesNotBreakBinding() {
        cacheManager.getCache("some_ad_hoc_cache");

        MeterRegistry registry = bind();

        assertThat(registry.find("volur.cache.l1.size").tag("cache", "some_ad_hoc_cache").gauge())
                .isNotNull();
    }

    @Test
    @DisplayName("声明式缓存路径的 puts 指标可见（loader.calls 在该路径下不增长）")
    void putsAreVisibleForDeclarativePath() {
        MeterRegistry registry = bind();
        String cache = CacheNames.USER_DETAILS;
        VolurCache target = cacheManager.volurCache(cache).orElseThrow();

        // 模拟 @Cacheable(sync=false)：get 未命中后由 Spring 调用 put
        target.get("1");
        target.put("1", "alice");

        assertThat(registry.get("volur.cache.puts").tag("cache", cache)
                .functionCounter().count()).isEqualTo(1.0D);
        assertThat(registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count()).isZero();
    }

    @Test
    @DisplayName("未创建的缓存不会被注册指标")
    void doesNotRegisterMetricsForUnknownCaches() {
        MeterRegistry registry = bind();

        assertThat(registry.find("volur.cache.loader.calls").tag("cache", "never_created")
                .functionCounter()).isNull();
    }
}
