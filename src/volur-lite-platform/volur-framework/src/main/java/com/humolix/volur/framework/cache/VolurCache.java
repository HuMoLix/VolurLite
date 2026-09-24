package com.humolix.volur.framework.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster;
import com.humolix.volur.framework.cache.redis.RedisOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 三级缓存实现：L1 Caffeine（进程内） → L2 Redis（String + JSON） → L3 回源 loader。
 *
 * <p><b>读取路径</b>
 * <pre>
 *   get(key, loader)
 *     ├─ L1 命中 ──────────────────────────────► 返回
 *     └─ L1 miss（Caffeine 对同 key 原子，天然单飞）
 *          ├─ L2 命中 ──► 回填 L1 ─────────────► 返回
 *          └─ L2 miss
 *               ├─ [可选] 抢 Redis 分布式锁 → 双检 L2 → 回源 → 写 L2
 *               └─ 未抢到锁 → 自旋等 L2 填充 → 超时兜底直接回源
 * </pre>
 *
 * <p><b>失效路径</b>：清本机 L1 → 删 L2 → 广播（其他实例收到后<b>只清各自 L1</b>）。
 *
 * <p><b>三条硬约束</b>（实现上不可妥协）：
 * <ol>
 *   <li><b>L2 故障一律降级</b>：所有 Redis 读写都被 try/catch 包裹，失败仅告警并回源，
 *       绝不向上抛异常——Redis 抖动不能演变成全站 500。</li>
 *   <li><b>TTL 必须带抖动</b>：批量写入的 key 若同时过期会引发雪崩，见
 *       {@link VolurCacheProperties.Resolved#effectiveL2Ttl(boolean)}。</li>
 *   <li><b>clear() 必须用 SCAN</b>：{@code KEYS} 会阻塞 Redis 单线程导致全站超时。</li>
 * </ol>
 *
 * <p>本类线程安全，实例由 {@link VolurCacheManager} 缓存复用。
 */
public class VolurCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(VolurCache.class);

    /**
     * "缓存的 null"对应的 ValueWrapper。
     *
     * <p>Spring 的 {@code CacheAspectSupport} 在拿到非 null 的 wrapper 时会直接返回
     * {@code wrapper.get()} 而<b>不再调用目标方法</b>——这正是空值缓存防穿透的机制。
     */
    private static final ValueWrapper NULL_HIT = new ValueWrapper() {
        @Override
        public Object get() {
            return null;
        }

        @Override
        public String toString() {
            return "VolurCacheNullHit";
        }
    };

    private final String name;
    private final VolurCacheProperties.Resolved spec;
    private final com.github.benmanes.caffeine.cache.Cache<Object, CachedValue> l1;
    private final StringRedisTemplate redis;
    private final RedisOps redisOps;
    private final CacheEvictBroadcaster broadcaster;

    /**
     * 运行期计数。刻意用零依赖的 {@link CacheCounters}，Micrometer 只在
     * {@link VolurCacheMetricsBinder} 里出现——这样本类不因埋点而被绑定到监控实现。
     */
    private final CacheCounters counters = new CacheCounters();

    VolurCache(String name,
               VolurCacheProperties.Resolved spec,
               StringRedisTemplate redis,
               RedisOps redisOps,
               CacheEvictBroadcaster broadcaster) {
        this.name = name;
        this.spec = spec;
        this.redis = redis;
        this.redisOps = redisOps;
        this.broadcaster = broadcaster;
        this.l1 = spec.l1Enabled() ? buildL1(spec) : null;
    }

    private static com.github.benmanes.caffeine.cache.Cache<Object, CachedValue> buildL1(
            VolurCacheProperties.Resolved spec) {
        // 仅在 L1 启用时才会被调用，因此无条件开启统计。
        // 注意必须显式指定泛型实参：Caffeine.newBuilder() 返回 Caffeine<Object, Object>，
        // 直接链式 build() 会得到 Cache<Object, Object> 而无法赋值给 Cache<Object, CachedValue>。
        return Caffeine.<Object, CachedValue>newBuilder()
                .maximumSize(spec.l1MaxSize())
                .expireAfterWrite(spec.l1Ttl())
                .recordStats()
                .build();
    }

    // ==================================================================
    // Spring Cache SPI
    // ==================================================================

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return l1 != null ? l1 : redis;
    }

    /**
     * 无 loader 的读取。L1 → L2（命中 L2 时回填 L1）。
     *
     * <p>注意区分返回值语义：{@code null} 表示 miss（Spring 会继续走 loader），
     * {@link #NULL_HIT} 表示"确实缓存了 null"（Spring 直接返回 null，不再回源）。
     */
    @Override
    public ValueWrapper get(Object key) {
        CachedValue cv = doGet(key);
        if (cv == null) {
            return null;
        }
        if (cv.isNullPlaceholder()) {
            return NULL_HIT;
        }
        Object value = unwrapQuietly(key, cv);
        // 走到这里 unwrapQuietly 返回 null 只有一种可能：反序列化失败
        //（真正的 null 值已在上面按 NULL_HIT 处理掉了）。
        //
        // 此时必须返回 null 表示 miss，让 Spring 继续调用 loader；
        // 若错误地返回 NULL_HIT，Spring 会把它当成"已经缓存了 null"而直接返回 null，
        // 结果是这个 key 在 TTL 内**永远拿不到真实数据**——一个只会在脏数据/类改名时
        // 才暴露、且表现为"偶发查不到"的隐蔽故障。
        return value == null ? null : new SimpleValueWrapper(value);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        CachedValue cv = doGet(key);
        if (cv == null || cv.isNullPlaceholder()) {
            return null;
        }
        Object value = unwrapQuietly(key, cv);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * 带 loader 的读取——{@code @Cacheable} 未命中时的实际入口。
     *
     * <p>关键点：loader 在 {@code Caffeine.get(key, fn)} 内部执行，因此<b>同一 key 的并发请求
     * 只有一个会真正回源</b>，其余阻塞等待结果。这是防击穿的第一道防线，且不需要分布式锁。
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (l1 == null) {
            try {
                return unwrapToType(loadThrough(key, valueLoader));
            } catch (Exception ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }
        }

        CachedValue cv;
        try {
            cv = l1.get(key, k -> loadThroughUnchecked(k, valueLoader));
        } catch (LoaderFailure failure) {
            throw new ValueRetrievalException(key, valueLoader, failure.getCause());
        }

        T value = unwrapToType(cv);
        if (value == null && cv != null && !cv.isNullPlaceholder()) {
            // 命中了一个"解不开"的值（L2 里的类型与配置不符、类被改名、脏数据……）。
            // 关键：不能就这样把 null 返回给调用方——那等于用缓存里的坏数据
            // 顶替了数据库里完好的数据，而且这个 key 会一直坏到 TTL 到期。
            // 必须丢弃它并真正回源一次。
            cv = reloadDiscardingUnusable(key, valueLoader);
            value = unwrapToType(cv);
        }
        return value;
    }

    /**
     * 丢弃无法还原的缓存值并强制回源。
     *
     * <p>先清 L1 与 L2 再回源，避免 {@code loadThrough} 又从 L2 读回同一个坏值。
     * 这里刻意不广播：坏值通常只源于本实例读取到的存量数据，
     * 批量出现时广播会引发消息风暴；让各实例在自己的 TTL 周期内自愈即可。
     */
    private <T> CachedValue reloadDiscardingUnusable(Object key, Callable<T> valueLoader) {
        log.warn("[volur-cache] 缓存值无法还原为配置类型，丢弃并回源。cache={}, key={}", name, key);
        if (l1 != null) {
            l1.invalidate(key);
        }
        discardL2(key);
        // 直接回源，刻意不再经过 loadThrough：那条路径会先读 L2，
        // 一旦删除因故未生效就会把同一个坏值再读回来，陷入"失败 → 重读 → 再失败"。
        CachedValue fresh = loadFromSourceUnchecked(key, valueLoader);
        if (l1 != null && fresh != null) {
            l1.put(key, fresh);
        }
        return fresh;
    }

    /** 跳过 L2 直接回源并写回 L2，用于已确认缓存值不可用的恢复路径。 */
    private CachedValue loadFromSourceUnchecked(Object key, Callable<?> loader) {
        try {
            return loadAndPopulate(key, loader);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LoaderFailure(ex);
        }
    }

    /** 删除 L2 中的单个 key，失败不抛（TTL 会兜底）。 */
    private void discardL2(Object key) {
        if (!spec.l2Enabled()) {
            return;
        }
        try {
            redis.delete(spec.l2Key(key));
        } catch (Exception ignored) {
            // 清不掉也无所谓，TTL 会兜底
        }
    }

    /**
     * 显式写入。
     *
     * <p><b>这是声明式缓存的主路径。</b> {@code @Cacheable} 默认（{@code sync=false}）的行为是：
     * <pre>
     *   get(key) 未命中 → 执行原方法 → put(key, 返回值)
     * </pre>
     * 也就是说它<b>不使用</b> {@link #get(Object, Callable)}，因此：
     * <ul>
     *   <li>不会走 {@code loadThrough}，也就<b>没有 Caffeine 单飞保护</b>——并发未命中会各自回源。
     *       需要防击穿时必须写 {@code @Cacheable(sync = true)}；</li>
     *   <li>回源耗时无法在此处统计（方法体不在我们手里），因此
     *       {@code volur.cache.loader.*} 指标对声明式缓存恒为 0，
     *       应改看 {@code volur.cache.puts}。</li>
     * </ul>
     */
    @Override
    public void put(Object key, Object value) {
        CachedValue cv;
        try {
            cv = CachedValue.of(value);
        } catch (Exception ex) {
            log.error("[volur-cache] 序列化缓存值失败，跳过写入。cache={}, key={}", name, key, ex);
            return;
        }
        if (cv.isNullPlaceholder() && !spec.nullCaching()) {
            // 与 loadAndPopulate 保持一致：关闭空值缓存时 L1 也不能存。
            // 声明式缓存同样会走到这里（@Cacheable 命中空结果后调用 put(key, null)），
            // 若只在 writeL2 里挡，L1 仍会缓存空值，"不缓存 null"的配置就是假的。
            return;
        }
        counters.put();
        if (l1 != null) {
            l1.put(key, cv);
        }
        writeL2(key, cv);
    }

    /**
     * 非原子的 putIfAbsent（与 Spring 默认契约一致）。
     *
     * <p>若业务需要严格的原子性，应改用 {@link RedisOps#tryLock} 或
     * {@code SET NX} 语义，不要依赖此方法。
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    /**
     * 主动失效：清本机 L1 → 删 L2 → 广播通知其他实例清各自的 L1。
     *
     * <p><b>只有发起方删 L2。</b>订阅方只清 L1——否则 A 删 L2 触发广播、
     * B 收到后再删 L2 又触发广播，形成无限回环。
     */
    @Override
    public void evict(Object key) {
        counters.explicitEviction();
        if (l1 != null) {
            l1.invalidate(key);
        }
        if (spec.l2Enabled()) {
            try {
                redis.delete(spec.l2Key(key));
            } catch (Exception ex) {
                log.warn("[volur-cache] L2 删除失败。cache={}, key={}", name, key, ex);
            }
        }
        if (!broadcaster.broadcastEvict(name, String.valueOf(key))) {
            counters.broadcastFailure();
        }
    }

    @Override
    public void clear() {
        if (l1 != null) {
            l1.invalidateAll();
        }
        if (spec.l2Enabled()) {
            // 必须 SCAN，不能用 KEYS
            redisOps.deleteByPattern(spec.l2KeyPattern());
        }
        if (!broadcaster.broadcastClear(name)) {
            counters.broadcastFailure();
        }
    }

    // ==================================================================
    // 供 CacheEvictListener 调用：只清本机 L1
    // ==================================================================

    /** 清本机 L1 中的单个 key（收到其他实例的失效广播时调用）。 */
    void invalidateLocalOnly(Object key) {
        if (l1 != null) {
            l1.invalidate(key);
        }
    }

    /** 清空本机 L1。 */
    void invalidateLocalAll() {
        if (l1 != null) {
            l1.invalidateAll();
        }
    }

    // ==================================================================
    // 内部实现
    // ==================================================================

    private CachedValue doGet(Object key) {
        CachedValue fromL1 = l1 != null ? l1.getIfPresent(key) : null;
        if (fromL1 != null) {
            return fromL1;
        }
        CachedValue fromL2 = readL2(key);
        if (fromL2 != null && l1 != null) {
            l1.put(key, fromL2);
        }
        return fromL2;
    }

    private CachedValue loadThroughUnchecked(Object key, Callable<?> loader) {
        try {
            return loadThrough(key, loader);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LoaderFailure(ex);
        }
    }

    private CachedValue loadThrough(Object key, Callable<?> loader) throws Exception {
        CachedValue fromL2 = readL2(key);
        if (fromL2 != null) {
            return fromL2;
        }

        if (!spec.distributedLock()) {
            return loadAndPopulate(key, loader);
        }

        String lockKey = spec.lockKey(key);
        Optional<RedisOps.LockHandle> handle = redisOps.tryLock(lockKey, spec.lockLease());
        if (handle.isPresent()) {
            try {
                // 双检：持锁期间可能已被其他实例填充
                CachedValue recheck = readL2(key);
                if (recheck != null) {
                    return recheck;
                }
                return loadAndPopulate(key, loader);
            } finally {
                redisOps.unlock(handle.get());
            }
        }

        // 未抢到锁：短暂自旋等待持锁者填充 L2
        for (int i = 0; i < spec.lockSpinAttempts(); i++) {
            sleepQuietly(spec.lockSpinInterval());
            CachedValue filled = readL2(key);
            if (filled != null) {
                return filled;
            }
        }

        // 兜底：宁可多打一次源，也不让请求无限阻塞
        log.debug("[volur-cache] 分布式锁自旋超时，兜底直接回源。cache={}, key={}", name, key);
        return loadAndPopulate(key, loader);
    }

    private CachedValue loadAndPopulate(Object key, Callable<?> loader) throws Exception {
        counters.loaderCall();
        long start = System.nanoTime();
        Object value;
        try {
            value = loader.call();
        } finally {
            // 无论回源成功还是抛异常都要记耗时——回源变慢/失败的耗时同样是关键信号
            counters.loaderNanos(System.nanoTime() - start);
        }
        CachedValue cv;
        try {
            cv = CachedValue.of(value);
        } catch (Exception ex) {
            log.error("[volur-cache] 序列化回源值失败，跳过写入缓存。cache={}, key={}", name, key, ex);
            return null;
        }
        if (cv.isNullPlaceholder() && !spec.nullCaching()) {
            // 关闭空值缓存时必须让 L1 也遵守，否则语义不一致：
            // 只在 writeL2 里挡，L1 仍会缓存空值占位，结果是"配了不缓存 null，
            // 但同一个实例内依然不再回源"——配置与行为不符。
            //
            // Caffeine 的 get(key, fn) 约定"映射函数返回 null 则不写入缓存"，
            // 所以返回 null 即可同时跳过 L1 与 L2，无需额外分支。
            return null;
        }
        writeL2(key, cv);
        return cv;
    }

    private CachedValue readL2(Object key) {
        if (!spec.l2Enabled()) {
            return null;
        }
        try {
            String raw = redis.opsForValue().get(spec.l2Key(key));
            if (raw == null) {
                counters.l2Miss();
                return null;
            }
            CachedValue cv = CachedValue.decode(raw);
            if (cv == null) {
                // 存量值无法解析（版本升级导致的格式变更等）：按 miss 处理并留痕
                counters.l2Error();
                counters.decodeFailure();
                return null;
            }
            if (cv.isNullPlaceholder() && !spec.nullCaching()) {
                counters.l2Miss();
                return null;
            }
            counters.l2Hit();
            return cv;
        } catch (Exception ex) {
            // 硬约束：L2 故障降级回源，绝不向上抛
            counters.l2Error();
            log.warn("[volur-cache] L2 读取失败，降级回源。cache={}, key={}", name, key, ex);
            return null;
        }
    }

    private void writeL2(Object key, CachedValue cv) {
        if (!spec.l2Enabled()) {
            return;
        }
        if (cv.isNullPlaceholder() && !spec.nullCaching()) {
            return;
        }
        Duration ttl = spec.effectiveL2Ttl(cv.isNullPlaceholder());
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redis.opsForValue().set(spec.l2Key(key), cv.encode(), ttl);
            counters.l2Write();
            if (cv.isNullPlaceholder()) {
                counters.nullPlaceholder();
            }
        } catch (Exception ex) {
            counters.l2Error();
            log.warn("[volur-cache] L2 写入失败，忽略。cache={}, key={}", name, key, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T unwrapToType(CachedValue cv) {
        if (cv == null || cv.isNullPlaceholder()) {
            return null;
        }
        try {
            return (T) cv.unwrap(spec.valueType());
        } catch (RuntimeException ex) {
            counters.decodeFailure();
            log.warn("[volur-cache] 值反序列化失败，按 miss 处理。cache={}", name, ex);
            return null;
        }
    }

    private Object unwrapQuietly(Object key, CachedValue cv) {
        try {
            return cv.unwrap(spec.valueType());
        } catch (RuntimeException ex) {
            // 脏值/类型不兼容：清掉本地与 L2 以免反复失败。
            // 刻意不广播——解码失败通常只影响本实例已读取的数据，
            // 广播会在批量脏值时引发消息风暴。
            log.warn("[volur-cache] 值反序列化失败，清除该 key。cache={}, key={}", name, key, ex);
            counters.decodeFailure();
            if (l1 != null) {
                l1.invalidate(key);
            }
            discardL2(key);
            return null;
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================================================================
    // 监控
    // ==================================================================

    /** L1 命中统计，供 Micrometer 采集。L1 未启用时返回空。 */
    public Optional<CacheStats> l1Stats() {
        return l1 == null ? Optional.empty() : Optional.of(l1.stats());
    }

    /** 运行期计数快照，供 {@link VolurCacheMetricsBinder} 采集。 */
    public CacheCounters.Snapshot counters() {
        return counters.snapshot();
    }

    /** L1 当前条目数。 */
    public long l1EstimatedSize() {
        return l1 == null ? 0L : l1.estimatedSize();
    }

    /** 手动触发 L1 清理（由后台任务周期性调用，回收过期条目）。 */
    public void l1CleanUp() {
        if (l1 != null) {
            l1.cleanUp();
        }
    }

    public VolurCacheProperties.Resolved spec() {
        return spec;
    }

    /** loader 抛出的受检异常包装，用于穿过 Caffeine 的 Function 签名。 */
    private static final class LoaderFailure extends RuntimeException {
        LoaderFailure(Throwable cause) {
            super(cause);
        }
    }
}
