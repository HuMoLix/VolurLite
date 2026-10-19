package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster;
import com.humolix.volur.framework.cache.redis.RedisOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link VolurCache} 的三级缓存行为契约。
 *
 * <p><b>为什么用 Mockito 而不是内嵌 Redis？</b>
 * 本项目的架构评估结论之一就是"零集成测试、零 {@code @SpringBootTest}"。
 * 但引入 testcontainers/内嵌 Redis 会让 CI 依赖 Docker 或额外的二进制。
 * 这里选择用 Mockito 精确控制 L2 的每一个返回（命中、未命中、抛异常、返回脏数据），
 * 这些场景用真实 Redis <b>反而很难构造</b>（尤其是"值存在但类型不兼容"）。
 * 需要真实 Redis 语义的部分（Lua 脚本、SCAN）单独用集成测试覆盖。
 *
 * <p>覆盖的关键不变量：
 * <ol>
 *   <li>L1 命中不得访问 L2；L2 命中必须回填 L1；</li>
 *   <li>空值必须被缓存，否则防穿透失效；</li>
 *   <li>L2 任何故障都必须降级回源，绝不向上抛异常；</li>
 *   <li>同一 key 并发回源只能有一次（防击穿）；</li>
 *   <li>无法还原的缓存值必须被丢弃并回源，而不是把 null 当结果返回。</li>
 * </ol>
 */
class VolurCacheTest {

    private static final String CACHE = "user_details";
    private static final String L2_KEY_1 = "vcache:user_details:1";

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private RedisOps redisOps;
    private CacheEvictBroadcaster broadcaster;
    private VolurCacheProperties props;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        redisOps = mock(RedisOps.class);
        broadcaster = mock(CacheEvictBroadcaster.class);
        when(broadcaster.broadcastEvict(anyString(), anyString())).thenReturn(true);
        when(broadcaster.broadcastClear(anyString())).thenReturn(true);

        props = new VolurCacheProperties();
    }

    private VolurCache newCache() {
        return new VolurCache(CACHE, props.resolve(CACHE), redis, redisOps, broadcaster);
    }

    // ==================================================================
    // 读取路径
    // ==================================================================

    @Test
    @DisplayName("L1 命中时完全不访问 L2 与 loader")
    void l1HitShortCircuits() {
        VolurCache cache = newCache();
        cache.put("1", "alice");
        clearInvocations(valueOps);

        AtomicInteger loads = new AtomicInteger();
        String value = cache.get("1", () -> {
            loads.incrementAndGet();
            return "reloaded";
        });

        assertThat(value).isEqualTo("alice");
        assertThat(loads.get()).isZero();
        verify(valueOps, never()).get(anyString());
    }

    @Test
    @DisplayName("L2 命中时回填 L1，且后续请求不再访问 L2")
    void l2HitBackfillsL1() {
        VolurCache cache = newCache();
        when(valueOps.get(L2_KEY_1)).thenReturn(CachedValue.of("bob").encode());

        AtomicInteger loads = new AtomicInteger();
        String first = cache.get("1", () -> {
            loads.incrementAndGet();
            return "fromDb";
        });
        assertThat(first).isEqualTo("bob");
        assertThat(loads.get()).isZero();

        // 第二次必须由 L1 直接满足
        clearInvocations(valueOps);
        assertThat(cache.get("1", () -> "fromDb")).isEqualTo("bob");
        verify(valueOps, never()).get(anyString());
    }

    @Test
    @DisplayName("L2 未命中时回源并把结果写入 L2")
    void l2MissLoadsAndPopulatesL2() {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(cache.get("1", () -> "carol")).isEqualTo("carol");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq(L2_KEY_1), payload.capture(), ttl.capture());

        // 写入 L2 的必须是可解码的信封，而不是裸字符串
        CachedValue written = CachedValue.decode(payload.getValue());
        assertThat(written).isNotNull();
        assertThat(written.unwrap(null)).isEqualTo("carol");
        assertThat(ttl.getValue()).isPositive();

        assertThat(cache.counters().l2Misses()).isEqualTo(1);
        assertThat(cache.counters().l2Writes()).isEqualTo(1);
        assertThat(cache.counters().loaderCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("无 loader 的 get 在未命中时返回 null（miss 语义）")
    void plainGetReturnsNullOnMiss() {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(cache.get("absent")).isNull();
    }

    // ==================================================================
    // 空值防穿透
    // ==================================================================

    @Test
    @DisplayName("空值被缓存：第二次不再回源（防穿透核心）")
    void nullResultIsCachedSoLoaderRunsOnce() {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        AtomicInteger loads = new AtomicInteger();
        Callable<String> loader = () -> {
            loads.incrementAndGet();
            return null;
        };

        assertThat(cache.get("404", loader)).isNull();
        assertThat(cache.get("404", loader)).isNull();
        assertThat(cache.get("404", loader)).isNull();

        assertThat(loads.get()).as("空值必须只回源一次").isEqualTo(1);
        assertThat(cache.counters().nullPlaceholders()).isEqualTo(1);
    }

    @Test
    @DisplayName("空值走独立的、显著更短的 TTL，避免长期占用")
    void nullPlaceholderUsesShorterTtl() {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        cache.get("404", () -> null);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("vcache:user_details:404"), anyString(), ttl.capture());

        VolurCacheProperties.Resolved spec = props.resolve(CACHE);
        assertThat(ttl.getValue()).isLessThan(spec.l2Ttl());
        // 允许 ±10% 抖动
        long expected = spec.l2NullTtl().toMillis();
        assertThat(ttl.getValue().toMillis()).isBetween(expected * 9 / 10, expected * 11 / 10);
    }

    @Test
    @DisplayName("显式关闭空值缓存时不写入 L2，且每次都会回源")
    void nullCachingCanBeDisabled() {
        VolurCacheProperties.CacheSpec spec = new VolurCacheProperties.CacheSpec();
        spec.setNullCaching(false);
        props.getCaches().put(CACHE, spec);

        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        AtomicInteger loads = new AtomicInteger();
        Callable<String> loader = () -> {
            loads.incrementAndGet();
            return null;
        };
        cache.get("404", loader);
        cache.get("404", loader);

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        assertThat(loads.get()).isEqualTo(2);
        assertThat(cache.counters().nullPlaceholders()).isZero();
    }

    @Test
    @DisplayName("put(null) 在关闭空值缓存时同样不落 L1（这是声明式缓存的主路径）")
    void putRespectsNullCachingDisabled() {
        VolurCacheProperties.CacheSpec spec = new VolurCacheProperties.CacheSpec();
        spec.setNullCaching(false);
        props.getCaches().put(CACHE, spec);

        VolurCache cache = newCache();

        // @Cacheable 未命中后会执行原方法，若返回 null 则调用 put(key, null)。
        // 如果这里把占位写进 L1，"不缓存 null"的配置在声明式路径上就完全失效了。
        cache.put("404", null);

        assertThat(cache.get("404")).isNull();
        assertThat(cache.l1EstimatedSize()).isZero();
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        assertThat(cache.counters().nullPlaceholders()).isZero();
        assertThat(cache.counters().puts()).isZero();
    }

    @Test
    @DisplayName("put(null) 在开启空值缓存时写入占位，并计入 puts")
    void putNullCachesPlaceholderWhenEnabled() {
        VolurCache cache = newCache();

        cache.put("404", null);

        // 关键区别：返回的是"已缓存 null"的包装器（非 null），而不是 miss 的 null。
        // Spring 据此直接返回 null 而不再调用目标方法——这就是防穿透。
        var wrapper = cache.get("404");
        assertThat(wrapper).as("必须是非 null 的包装器，否则 Spring 会继续回源").isNotNull();
        assertThat(wrapper.get()).isNull();

        assertThat(cache.counters().puts()).isEqualTo(1);
        assertThat(cache.counters().nullPlaceholders()).isEqualTo(1);
    }

    // ==================================================================
    // 故障降级（缓存不能成为故障源）
    // ==================================================================

    @Test
    @DisplayName("L2 读写全部异常时仍能正常回源，绝不抛异常")
    void l2FailureDegradesToLoader() {
        VolurCache cache = newCache();
        RedisConnectionFailureException down = new RedisConnectionFailureException("redis down");
        when(valueOps.get(anyString())).thenThrow(down);
        doThrow(down).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        String value = cache.get("1", () -> "dave");

        assertThat(value).isEqualTo("dave");
        // 读失败 1 次 + 写失败 1 次
        assertThat(cache.counters().l2Errors()).isEqualTo(2);
        assertThat(cache.counters().loaderCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("L2 中存在畸形 JSON 时按 miss 处理并回源")
    void malformedL2ValueDegradesToLoader() {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn("###not-json###");

        assertThat(cache.get("1", () -> "heidi")).isEqualTo("heidi");
        assertThat(cache.counters().decodeFailures()).isEqualTo(1);
        assertThat(cache.counters().loaderCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("L2 值类型不在白名单时：丢弃该值并真正回源（不能把 null 当结果返回）")
    void unwrappableL2ValueIsDiscardedAndReloaded() {
        VolurCache cache = newCache();
        String tampered = "{\"n\":false,\"t\":\"javax.script.ScriptEngineManager\",\"v\":\"{}\"}";
        when(valueOps.get(anyString())).thenReturn(tampered);

        AtomicInteger loads = new AtomicInteger();
        String value = cache.get("1", () -> {
            loads.incrementAndGet();
            return "realValue";
        });

        // 这是本测试最重要的断言：必须拿到数据库的真实值，而不是 null
        assertThat(value).isEqualTo("realValue");
        assertThat(loads.get()).isEqualTo(1);
        // 坏值必须被从 L2 删除，否则每次请求都要重复走一遍失败路径
        verify(redis).delete(L2_KEY_1);
    }

    @Test
    @DisplayName("无 loader 的 get 遇到解不开的值时返回 miss（而非'已缓存 null'）")
    void plainGetTreatsUnwrappableValueAsMiss() {
        VolurCache cache = newCache();
        String tampered = "{\"n\":false,\"t\":\"javax.script.ScriptEngineManager\",\"v\":\"{}\"}";
        when(valueOps.get(anyString())).thenReturn(tampered);

        // 若这里返回了"已缓存 null"的包装，Spring 会直接返回 null 而不调用 loader，
        // 导致该 key 在 TTL 内永远拿不到数据。
        assertThat(cache.get("1")).isNull();
    }

    // ==================================================================
    // 防击穿：同 key 并发只回源一次
    // ==================================================================

    @Test
    @DisplayName("同一 key 并发 16 个请求，loader 只被调用一次")
    void concurrentSameKeyLoadsOnce() throws Exception {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        AtomicInteger loads = new AtomicInteger();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return cache.get("hot", () -> {
                        loads.incrementAndGet();
                        // 放大竞态窗口：若没有单飞保护，这里会挤进多个线程
                        Thread.sleep(30);
                        return "expensive";
                    });
                }));
            }
            start.countDown();

            for (Future<String> future : futures) {
                assertThat(future.get()).isEqualTo("expensive");
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(loads.get()).as("Caffeine get(key, fn) 的单飞语义").isEqualTo(1);
    }

    // ==================================================================
    // 失效路径
    // ==================================================================

    @Test
    @DisplayName("evict 同时清 L1、删 L2、广播，三件事缺一不可")
    void evictDoesAllThreeThings() {
        VolurCache cache = newCache();
        cache.put("1", "erin");
        clearInvocations(valueOps, broadcaster, redis);

        cache.evict("1");

        verify(redis).delete(L2_KEY_1);
        verify(broadcaster).broadcastEvict(CACHE, "1");

        // L1 已清：loader 必须被重新调用
        when(valueOps.get(anyString())).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();
        assertThat(cache.get("1", () -> {
            loads.incrementAndGet();
            return "fromDb";
        })).isEqualTo("fromDb");
        assertThat(loads.get()).isEqualTo(1);
        assertThat(cache.counters().explicitEvictions()).isEqualTo(1);
    }

    @Test
    @DisplayName("clear 用 SCAN 模式删除 L2，绝不使用 KEYS")
    void clearUsesScanPattern() {
        VolurCache cache = newCache();

        cache.clear();

        verify(redisOps).deleteByPattern("vcache:user_details:*");
        verify(broadcaster).broadcastClear(CACHE);
    }

    @Test
    @DisplayName("广播失败只计数，不影响业务（L1 TTL 兜底）")
    void broadcastFailureIsCountedNotThrown() {
        VolurCache cache = newCache();
        when(broadcaster.broadcastEvict(anyString(), anyString())).thenReturn(false);

        cache.evict("1");

        assertThat(cache.counters().broadcastFailures()).isEqualTo(1);
    }

    @Test
    @DisplayName("L2 删除失败不影响 evict 完成（广播仍会发出）")
    void l2DeleteFailureDoesNotAbortEvict() {
        VolurCache cache = newCache();
        when(redis.delete(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        cache.evict("1");

        verify(broadcaster).broadcastEvict(CACHE, "1");
    }

    @Test
    @DisplayName("invalidateLocalOnly 只清 L1，绝不碰 L2（避免广播回环）")
    void localInvalidationDoesNotTouchL2() {
        VolurCache cache = newCache();
        cache.put("1", "frank");
        clearInvocations(valueOps, redis, broadcaster);

        cache.invalidateLocalOnly("1");

        verify(redis, never()).delete(anyString());
        verify(broadcaster, never()).broadcastEvict(anyString(), anyString());
        // L1 已清 → 会去读 L2
        assertThat(cache.get("1")).isNull();
        verify(valueOps).get(L2_KEY_1);
    }

    // ==================================================================
    // L1 关闭时的降级形态
    // ==================================================================

    @Test
    @DisplayName("关闭 L1 后仍能正常读写 L2，且不报统计信息")
    void worksWithL1Disabled() {
        VolurCacheProperties.CacheSpec spec = new VolurCacheProperties.CacheSpec();
        spec.setL1Enabled(false);
        props.getCaches().put(CACHE, spec);

        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(cache.get("1", () -> "grace")).isEqualTo("grace");
        assertThat(cache.l1Stats()).isEmpty();
        assertThat(cache.l1EstimatedSize()).isZero();
        assertThat(cache.counters().l2Writes()).isEqualTo(1);
    }

    @Test
    @DisplayName("关闭 L2 后成为纯进程内缓存，不再产生任何 Redis 访问")
    void worksWithL2Disabled() {
        VolurCacheProperties.CacheSpec spec = new VolurCacheProperties.CacheSpec();
        spec.setL2Ttl(Duration.ZERO);
        props.getL2().setEnabled(false);
        props.getCaches().put(CACHE, spec);

        VolurCache cache = newCache();
        assertThat(cache.get("1", () -> "henry")).isEqualTo("henry");

        verify(valueOps, never()).get(anyString());
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        assertThat(cache.counters().l2Writes()).isZero();
    }

    @Test
    @DisplayName("loader 抛出的受检异常被包装为 Spring 的 ValueRetrievalException")
    void checkedLoaderExceptionIsWrapped() {
        VolurCache cache = newCache();
        when(valueOps.get(anyString())).thenReturn(null);

        Callable<String> failing = () -> {
            throw new java.io.IOException("db unavailable");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> cache.get("1", failing))
                .isInstanceOf(org.springframework.cache.Cache.ValueRetrievalException.class)
                .hasRootCauseInstanceOf(java.io.IOException.class);
    }
}
