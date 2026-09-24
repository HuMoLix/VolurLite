package com.humolix.volur.framework.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.Cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 缓存编程式门面。
 *
 * <p>{@code @Cacheable} 覆盖不了所有场景，以下情况必须用编程式 API：
 * <ul>
 *   <li>key 需要运行时动态拼装（多参数组合、按角色分桶）</li>
 *   <li>需要在同一方法内读多个不同缓存</li>
 *   <li>需要条件化缓存（例如仅对"进行中"的竞赛启用）</li>
 *   <li>需要在缓存之外附加埋点或降级逻辑</li>
 * </ul>
 *
 * <p>它同时把 {@code Cache.ValueRetrievalException} 还原为原始业务异常，
 * 避免调用方被迫处理 Spring 的包装异常。
 */
public class VolurCacheTemplate {

    private final VolurCacheManager cacheManager;

    public VolurCacheTemplate(VolurCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 读取缓存，未命中则回源并回填。
     *
     * <pre>{@code
     * UserDetails ud = cacheTemplate.getOrLoad(
     *         CacheNames.USER_DETAILS, username, () -> loadFromDb(username));
     * }</pre>
     *
     * @param cacheName 缓存名，见 {@link CacheNames}
     * @param key       缓存 key
     * @param loader    回源逻辑，可能为 null 返回值（会被缓存为防穿透占位）
     */
    public <T> T getOrLoad(String cacheName, Object key, Callable<T> loader) {
        Cache cache = cacheManager.getCache(cacheName);
        try {
            return cache.get(key, loader);
        } catch (Cache.ValueRetrievalException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("缓存回源失败: " + cacheName + ":" + key, cause);
        }
    }

    /**
     * 纯读取，不回源。未命中或缓存的是 null 时返回 {@code null}。
     *
     * @param type 期望类型，类型不匹配时返回 {@code null}（不会抛 ClassCastException）
     */
    public <T> T get(String cacheName, Object key, Class<T> type) {
        return cacheManager.getCache(cacheName).get(key, type);
    }

    /** 写入缓存。 */
    public void put(String cacheName, Object key, Object value) {
        cacheManager.getCache(cacheName).put(key, value);
    }

    /**
     * 主动失效单个 key，并广播通知其他实例清理各自的 L1。
     *
     * <p><b>写操作必须调用本方法</b>，否则其他实例的 L1 会一直脏到 TTL 到期。
     */
    public void evict(String cacheName, Object key) {
        cacheManager.getCache(cacheName).evict(key);
    }

    /** 清空整个缓存（L1 + L2 + 广播）。慎用，可能引发瞬时回源洪峰。 */
    public void evictAll(String cacheName) {
        cacheManager.getCache(cacheName).clear();
    }

    /**
     * 采集 L1 统计快照，用于 Micrometer 指标或管理端展示。
     *
     * @return 缓存名 → 统计信息；未启用 L1 的缓存不在其中
     */
    public Map<String, CacheStats> l1StatsSnapshot() {
        Map<String, CacheStats> snapshot = new LinkedHashMap<>();
        for (String name : cacheManager.getCacheNames()) {
            cacheManager.volurCache(name)
                    .flatMap(VolurCache::l1Stats)
                    .ifPresent(stats -> snapshot.put(name, stats));
        }
        return snapshot;
    }

    /**
     * L1 当前条目数与命中率摘要，可直接渲染到管理端。
     */
    public Map<String, String> l1Summary() {
        Map<String, String> summary = new LinkedHashMap<>();
        for (String name : cacheManager.getCacheNames()) {
            cacheManager.volurCache(name).ifPresent(vc -> {
                long size = vc.l1EstimatedSize();
                String hitRate = vc.l1Stats()
                        .map(s -> String.format("%.2f%%", s.hitRate() * 100))
                        .orElse("L1 未启用");
                summary.put(name, "size=" + size + ", hitRate=" + hitRate);
            });
        }
        return summary;
    }
}
