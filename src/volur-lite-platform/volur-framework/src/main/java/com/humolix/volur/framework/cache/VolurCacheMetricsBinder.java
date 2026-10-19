package com.humolix.volur.framework.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 把 {@link VolurCache} 的运行期计数翻译成 Micrometer 指标。
 *
 * <p><b>为什么单独一个类？</b> 让 {@code volur-framework} 的缓存核心不依赖 Micrometer。
 * 本类是<b>可选装配</b>的：只有当 {@code micrometer-core} 在类路径上（即下游引入了
 * {@code spring-boot-starter-actuator}）才会被创建，见
 * {@code VolurCacheAutoConfiguration#volurCacheMetricsBinder}。
 *
 * <p><b>与 Spring Boot 内建缓存指标的分工</b>
 * <ul>
 *   <li>Boot 的 {@code CacheMetricsAutoConfiguration}（位于 {@code spring-boot-cache} 模块）
 *       能识别 {@code getNativeCache()} 返回的 Caffeine 实例，自动产出
 *       {@code cache.gets{cache=..., result=hit|miss}} 一类的 L1 指标。</li>
 *   <li>但它<b>看不见 L2（Redis）与回源（loader）</b>——那是本框架独有的三级结构。
 *       因此本类的重点指标是 {@code volur.cache.l2.*} 与 {@code volur.cache.loader.*}，
 *       同时也补一份 {@code volur.cache.l1.*} 以便用统一前缀做看板。</li>
 * </ul>
 *
 * <p><b>指标清单</b>
 * <table border="1">
 *   <caption>暴露的指标</caption>
 *   <tr><th>指标名</th><th>类型</th><th>标签</th><th>含义</th></tr>
 *   <tr><td>volur.cache.l1.size</td><td>Gauge</td><td>cache</td><td>L1 当前条目数</td></tr>
 *   <tr><td>volur.cache.l1.gets</td><td>Counter</td><td>cache, result</td><td>L1 命中/未命中次数</td></tr>
 *   <tr><td>volur.cache.l1.evictions</td><td>Counter</td><td>cache</td><td>L1 淘汰条目数</td></tr>
 *   <tr><td>volur.cache.l2.gets</td><td>Counter</td><td>cache, result</td><td>L2 命中/未命中次数</td></tr>
 *   <tr><td>volur.cache.l2.writes</td><td>Counter</td><td>cache</td><td>L2 写入成功次数</td></tr>
 *   <tr><td>volur.cache.l2.errors</td><td>Counter</td><td>cache</td><td>L2 读写异常次数（已降级）</td></tr>
 *   <tr><td>volur.cache.loader.calls</td><td>Counter</td><td>cache</td><td>回源次数（仅 sync=true / 编程式路径）</td></tr>
 *   <tr><td>volur.cache.loader.duration</td><td>Counter</td><td>cache</td><td>回源累计耗时（秒，同上路径）</td></tr>
 *   <tr><td>volur.cache.puts</td><td>Counter</td><td>cache</td><td>写入次数；默认 @Cacheable 的回源次数看这里</td></tr>
 *   <tr><td>volur.cache.null.placeholders</td><td>Counter</td><td>cache</td><td>空值占位写入次数</td></tr>
 *   <tr><td>volur.cache.evictions</td><td>Counter</td><td>cache</td><td>显式 evict 调用次数</td></tr>
 *   <tr><td>volur.cache.invalidation.failed</td><td>Counter</td><td>cache</td><td>失效广播发送失败次数</td></tr>
 *   <tr><td>volur.cache.decode.failures</td><td>Counter</td><td>cache</td><td>值反序列化失败次数</td></tr>
 * </table>
 *
 * <p><b>关键用法（PromQL）</b>
 * <pre>
 *   // 声明式缓存的整体命中率（默认 @Cacheable）：
 *   // 每次读都会经过 cache.get(key) → 计入 l1.gets；未命中会触发一次 put。
 *   1 - sum(rate(volur_cache_puts_total[5m]))
 *       / sum(rate(volur_cache_l1_gets_total[5m]))
 *
 *   // sync=true / 编程式路径的回源平均耗时
 *   rate(volur_cache_loader_duration_seconds_total[5m])
 *     / rate(volur_cache_loader_calls_total[5m])
 *
 *   // 最需要关注的告警：L2 错误率
 *   rate(volur_cache_l2_errors_total[5m]) > 1
 * </pre>
 *
 * <p><b>⚠️ 易踩的坑：{@code loader.calls} 对默认 {@code @Cacheable} 恒为 0。</b>
 * 因为 {@code @Cacheable} 默认 {@code sync=false}，Spring 走的是
 * {@code cache.get(key)} + {@code cache.put(key, value)}，压根不经过
 * {@code Cache#get(Object, Callable)}，因此 {@code loadThrough}/{@code loadAndPopulate}
 * 不会被调用。真正需要"回源次数"时请用 {@code volur.cache.puts}，
 * 或把注解写成 {@code @Cacheable(sync = true)}。
 *
 * <p><b>为什么用 {@code FunctionCounter} 而不是 {@code Counter}？</b>
 * {@code FunctionCounter} 在抓取时调用一个函数读取外部状态，而不是由本类主动
 * {@code increment()}。这样计数只有一份（在 {@link CacheCounters} 里），指标只是它的投影，
 * 不会出现"业务计数与指标计数不一致"这类经典问题。
 */
public class VolurCacheMetricsBinder implements MeterBinder {

    private static final Logger log = LoggerFactory.getLogger(VolurCacheMetricsBinder.class);

    /** 指标统一前缀。 */
    private static final String PREFIX = "volur.cache.";

    private final CacheManager cacheManager;

    /**
     * 刻意依赖 {@link CacheManager} <b>接口</b>而不是 {@link VolurCacheManager} 具体类。
     *
     * <p>好处是"最坏情况可退化"：若哪天自动配置排序被别的 CacheManager 抢走，
     * 本绑定器会遍历到非 {@link VolurCache} 的缓存并<b>安静地跳过</b>，
     * 结果是"没有指标"；而依赖具体类则会在启动时抛
     * "No qualifying bean of type VolurCacheManager"，把监控问题升级成可用性故障。
     */
    public VolurCacheMetricsBinder(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Set<String> names = new LinkedHashSet<>(CacheNames.ALL);
        names.addAll(cacheManager.getCacheNames());

        int bound = 0;
        for (String name : names) {
            if (cacheManager.getCache(name) instanceof VolurCache cache) {
                bindCache(registry, name, cache);
                bound++;
            }
        }
        log.info("[volur-cache] 已注册 {} 个缓存的指标到 MeterRegistry（共声明 {} 个）",
                bound, names.size());
    }

    private void bindCache(MeterRegistry registry, String name, VolurCache cache) {
        // ---------- L1：直接读 Caffeine 统计 ----------
        Gauge.builder(PREFIX + "l1.size", cache, VolurCache::l1EstimatedSize)
                .description("L1 (Caffeine) 当前条目数")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "l1.gets", cache, c -> l1Stat(c, CacheStats::hitCount))
                .description("L1 命中次数")
                .tag("cache", name).tag("result", "hit")
                .register(registry);

        FunctionCounter.builder(PREFIX + "l1.gets", cache, c -> l1Stat(c, CacheStats::missCount))
                .description("L1 未命中次数")
                .tag("cache", name).tag("result", "miss")
                .register(registry);

        FunctionCounter.builder(PREFIX + "l1.evictions", cache, c -> l1Stat(c, CacheStats::evictionCount))
                .description("L1 淘汰条目数")
                .tag("cache", name)
                .register(registry);

        // ---------- L2：Redis 读写 ----------
        FunctionCounter.builder(PREFIX + "l2.gets", cache, c -> c.counters().l2Hits())
                .description("L2 (Redis) 命中次数")
                .tag("cache", name).tag("result", "hit")
                .register(registry);

        FunctionCounter.builder(PREFIX + "l2.gets", cache, c -> c.counters().l2Misses())
                .description("L2 (Redis) 未命中次数（按查询次数计，非唯一 key 数）")
                .tag("cache", name).tag("result", "miss")
                .register(registry);

        FunctionCounter.builder(PREFIX + "l2.writes", cache, c -> c.counters().l2Writes())
                .description("L2 (Redis) 写入成功次数")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "l2.errors", cache, c -> c.counters().l2Errors())
                .description("L2 (Redis) 读写异常次数（已降级回源，业务未失败）")
                .tag("cache", name)
                .register(registry);

        // ---------- L3：回源 ----------
        // 注意：loader.* 只覆盖 @Cacheable(sync = true) 与编程式 getOrLoad 路径。
        // 默认的 @Cacheable(sync = false) 走的是 get + put，其"回源次数"应看 puts。
        FunctionCounter.builder(PREFIX + "loader.calls", cache, c -> c.counters().loaderCalls())
                .description("回源次数（仅 sync=true 的 @Cacheable 与编程式 getOrLoad 路径）")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "loader.duration", cache,
                        c -> c.counters().loaderNanos() / 1_000_000_000.0D)
                .description("回源累计耗时（秒，仅 sync=true 与编程式路径）")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "puts", cache, c -> c.counters().puts())
                .description("写入次数。默认 @Cacheable 的回源次数看这个值（未命中→执行原方法→put）")
                .tag("cache", name)
                .register(registry);

        // ---------- 其他 ----------
        FunctionCounter.builder(PREFIX + "null.placeholders", cache, c -> c.counters().nullPlaceholders())
                .description("空值占位写入次数（防穿透生效次数）")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "evictions", cache, c -> c.counters().explicitEvictions())
                .description("显式 evict() 调用次数")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "invalidation.failed", cache, c -> c.counters().broadcastFailures())
                .description("失效广播发送失败次数，>0 表示其他实例的 L1 要靠 TTL 兜底")
                .tag("cache", name)
                .register(registry);

        FunctionCounter.builder(PREFIX + "decode.failures", cache, c -> c.counters().decodeFailures())
                .description("值反序列化失败次数，持续 >0 通常意味着类型不兼容或脏数据")
                .tag("cache", name)
                .register(registry);
    }

    private static double l1Stat(VolurCache cache, java.util.function.ToLongFunction<CacheStats> extractor) {
        return cache.l1Stats().map(extractor::applyAsLong).orElse(0L);
    }
}
