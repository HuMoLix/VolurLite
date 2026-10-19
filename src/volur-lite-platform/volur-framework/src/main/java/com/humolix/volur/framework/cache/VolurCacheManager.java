package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster;
import com.humolix.volur.framework.cache.redis.RedisOps;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局缓存管理器：统一生产并持有所有 {@link VolurCache} 实例。
 *
 * <p>业务模块只需注入本类（或直接使用 {@code @Cacheable}），不应再自行创建
 * Caffeine / Redis 缓存实例。收敛的收益：
 * <ul>
 *   <li>统一的 key 命名空间与 TTL 策略，运维可在 Redis 里按前缀批量排查；</li>
 *   <li><b>单一的失效广播通道</b>——避免每个模块各建一个
 *       {@code RedisMessageListenerContainer} 造成连接浪费；</li>
 *   <li>统一的指标口径与启动预热。</li>
 * </ul>
 *
 * <p><b>与 {@link RedisOps} 的边界</b>：本类只负责"整体读、整体写的对象缓存"。
 * 排序（ZSET）、原子计数（限流）、互斥（分布式锁）属于数据结构操作，
 * 走 {@link RedisOps}，不要试图往本类里加方法。
 */
public class VolurCacheManager implements CacheManager {

    private static final Logger log = LoggerFactory.getLogger(VolurCacheManager.class);

    private final VolurCacheProperties properties;
    private final StringRedisTemplate redis;
    private final RedisOps redisOps;
    private final CacheEvictBroadcaster broadcaster;
    private final Map<String, VolurCache> caches = new ConcurrentHashMap<>();

    public VolurCacheManager(VolurCacheProperties properties,
                             StringRedisTemplate redis,
                             RedisOps redisOps,
                             CacheEvictBroadcaster broadcaster) {
        this.properties = properties;
        this.redis = redis;
        this.redisOps = redisOps;
        this.broadcaster = broadcaster;
    }

    /**
     * 启动时预创建 {@link CacheNames#ALL} 中声明的全部缓存。
     *
     * <p><b>为什么必须预热？</b> 若全部依赖懒创建，启动瞬间
     * {@link #getCacheNames()} 返回空集合，Micrometer 注册不到任何缓存指标，
     * Prometheus 里查不到命中率——等到第一个请求进来才创建，指标是断层的。
     */
    @PostConstruct
    public void preCreateDeclaredCaches() {
        for (String name : CacheNames.ALL) {
            getCache(name);
        }
        log.info("[volur-cache] 预创建缓存完成，共 {} 个: {}",
                caches.size(), String.join(", ", caches.keySet()));
    }

    /**
     * 获取缓存实例。
     *
     * <p><b>实现契约</b>：Spring 的 {@code CacheAspectSupport} 依赖本方法返回非 null，
     * 因此这里对未声明的缓存名也会动态创建（而不是返回 null）。这样业务方新增缓存时
     * 忘记登记 {@link CacheNames} 只是失去"预热与指标"，不会直接抛异常。
     */
    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(caches.keySet()));
    }

    /**
     * 供 {@code CacheEvictListener} 调用：仅清理本机 L1。
     *
     * <p>刻意不碰 L2——L2 由广播发起方负责删除。若订阅方也删 L2 并再次广播，
     * 会形成 A → B → A 的无限回环。
     *
     * @param cacheName 缓存名
     * @param key       目标 key；{@code clearAll} 为 true 时忽略
     * @param clearAll  是否清空整个缓存
     */
    public void invalidateLocal(String cacheName, String key, boolean clearAll) {
        VolurCache cache = caches.get(cacheName);
        if (cache == null) {
            // 本实例尚未创建过该缓存，说明它也没缓存过任何数据，无需清理
            return;
        }
        if (clearAll) {
            cache.invalidateLocalAll();
        } else if (key != null) {
            cache.invalidateLocalOnly(key);
        }
    }

    /** 获取强类型的 {@link VolurCache}，用于读取 L1 统计信息。 */
    public Optional<VolurCache> volurCache(String name) {
        Cache cache = caches.get(name);
        return cache instanceof VolurCache vc ? Optional.of(vc) : Optional.empty();
    }

    /** 清理所有缓存的 L1 过期条目，建议由低频后台任务调用。 */
    public void cleanUpAll() {
        caches.values().forEach(VolurCache::l1CleanUp);
    }

    private VolurCache createCache(String name) {
        VolurCacheProperties.Resolved spec = properties.resolve(name);
        VolurCache cache = new VolurCache(name, spec, redis, redisOps, broadcaster);
        log.info("[volur-cache] 创建缓存 cache={} | L1={} (ttl={}, maxSize={}) | L2={} (ttl={}, nullTtl={}) | 分布式锁={}",
                name,
                spec.l1Enabled(), spec.l1Ttl(), spec.l1MaxSize(),
                spec.l2Enabled(), spec.l2Ttl(), spec.l2NullTtl(),
                spec.distributedLock());
        return cache;
    }
}
