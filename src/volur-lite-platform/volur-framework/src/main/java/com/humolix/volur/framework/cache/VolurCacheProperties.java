package com.humolix.volur.framework.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 全局缓存配置。
 *
 * <p>三级结构：{@code volur.cache.l1.*}（进程内）/ {@code volur.cache.l2.*}（Redis）/
 * {@code volur.cache.caches.<cacheName>.*}（按缓存名精细覆盖）。
 *
 * <p><b>设计要点</b>：按缓存名的覆盖项全部使用包装类型（{@code Boolean} / {@code Duration}），
 * 未配置时保持 {@code null} 表示"继承默认值"。这样配置文件中只需写与默认值不同的项。
 */
@ConfigurationProperties(prefix = "volur.cache")
public class VolurCacheProperties {

    /** 总开关。关闭后所有缓存退化为直接回源，用于故障时紧急降级。 */
    private boolean enabled = true;

    private L1 l1 = new L1();

    private L2 l2 = new L2();

    private Invalidation invalidation = new Invalidation();

    /** 按缓存名覆盖，key 为 {@link CacheNames} 中声明的缓存名。 */
    private Map<String, CacheSpec> caches = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // L1：进程内 Caffeine
    // ------------------------------------------------------------------

    public static class L1 {

        /** 是否启用进程内一级缓存。 */
        private boolean enabled = true;

        /** 单实例内每个缓存的最大条目数，防止无界增长导致 OOM。 */
        private long maxSize = 10_000L;

        /**
         * 默认 L1 TTL。
         *
         * <p><b>刻意设得短</b>：Redis Pub/Sub 不持久化，订阅者重连窗口内的失效广播会丢失。
         * L1 TTL 是脏数据窗口的<b>兜底保证</b>，广播只是把窗口从 TTL 缩短到毫秒级，不能替代 TTL。
         */
        private Duration defaultTtl = Duration.ofSeconds(30);

        /** 是否开启命中率统计（供 Micrometer 采集）。 */
        private boolean recordStats = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public boolean isRecordStats() {
            return recordStats;
        }

        public void setRecordStats(boolean recordStats) {
            this.recordStats = recordStats;
        }
    }

    // ------------------------------------------------------------------
    // L2：Redis
    // ------------------------------------------------------------------

    public static class L2 {

        /** 是否启用 Redis 二级缓存。 */
        private boolean enabled = true;

        /** Redis key 前缀，统一命名空间便于运维排查与批量清理。 */
        private String keyPrefix = "vcache:";

        /** 默认 L2 TTL。 */
        private Duration defaultTtl = Duration.ofMinutes(10);

        /** 空值占位 TTL，应显著短于正常 TTL。 */
        private Duration nullTtl = Duration.ofSeconds(60);

        /** TTL 随机抖动比例（0.1 表示 ±10%），用于防雪崩。 */
        private double ttlJitterRatio = 0.1D;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getNullTtl() {
            return nullTtl;
        }

        public void setNullTtl(Duration nullTtl) {
            this.nullTtl = nullTtl;
        }

        public double getTtlJitterRatio() {
            return ttlJitterRatio;
        }

        public void setTtlJitterRatio(double ttlJitterRatio) {
            this.ttlJitterRatio = ttlJitterRatio;
        }
    }

    // ------------------------------------------------------------------
    // 广播失效
    // ------------------------------------------------------------------

    public static class Invalidation {

        /** 是否启用跨实例 L1 失效广播。关闭后仅依赖 L1 TTL 保证最终一致。 */
        private boolean enabled = true;

        /** 广播频道。与 {@code vredis:topic:*} 命名规范保持一致。 */
        private String topic = "vredis:topic:cache:evict";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    // ------------------------------------------------------------------
    // 单缓存覆盖项
    // ------------------------------------------------------------------

    public static class CacheSpec {

        /** 是否启用 L1。强一致数据（竞赛状态、可见性）必须显式设为 false。 */
        private Boolean l1Enabled;

        private Duration l1Ttl;

        private Duration l2Ttl;

        private Duration l2NullTtl;

        /** 是否缓存 null 值（防穿透）。对"大量查询不存在的 key"场景才需要。 */
        private Boolean nullCaching;

        /**
         * 是否启用 Redis 分布式锁防跨实例击穿。
         *
         * <p><b>默认关闭</b>：Caffeine 的 {@code get(key, fn)} 已解决单实例击穿，
         * 分布式锁会引入额外一次 SETNX + DEL，并让未抢到锁的线程自旋等待，尾延迟变差。
         * 仅对"回源极慢 + 极热"的缓存开启。
         */
        private Boolean distributedLock;

        /** 分布式锁租约，应大于回源 P99 耗时。 */
        private Duration lockLease;

        /** 未抢到锁时的自旋间隔。 */
        private Duration lockSpinInterval;

        /** 未抢到锁时的最大自旋次数，超时后兜底直接回源（宁可多打一次源，也不阻塞请求）。 */
        private Integer lockSpinAttempts;

        /** 缓存值的具体类型，用于泛型/集合类型的安全反序列化。 */
        private Class<?> valueType;

        public Boolean getL1Enabled() {
            return l1Enabled;
        }

        public void setL1Enabled(Boolean l1Enabled) {
            this.l1Enabled = l1Enabled;
        }

        public Duration getL1Ttl() {
            return l1Ttl;
        }

        public void setL1Ttl(Duration l1Ttl) {
            this.l1Ttl = l1Ttl;
        }

        public Duration getL2Ttl() {
            return l2Ttl;
        }

        public void setL2Ttl(Duration l2Ttl) {
            this.l2Ttl = l2Ttl;
        }

        public Duration getL2NullTtl() {
            return l2NullTtl;
        }

        public void setL2NullTtl(Duration l2NullTtl) {
            this.l2NullTtl = l2NullTtl;
        }

        public Boolean getNullCaching() {
            return nullCaching;
        }

        public void setNullCaching(Boolean nullCaching) {
            this.nullCaching = nullCaching;
        }

        public Boolean getDistributedLock() {
            return distributedLock;
        }

        public void setDistributedLock(Boolean distributedLock) {
            this.distributedLock = distributedLock;
        }

        public Duration getLockLease() {
            return lockLease;
        }

        public void setLockLease(Duration lockLease) {
            this.lockLease = lockLease;
        }

        public Duration getLockSpinInterval() {
            return lockSpinInterval;
        }

        public void setLockSpinInterval(Duration lockSpinInterval) {
            this.lockSpinInterval = lockSpinInterval;
        }

        public Integer getLockSpinAttempts() {
            return lockSpinAttempts;
        }

        public void setLockSpinAttempts(Integer lockSpinAttempts) {
            this.lockSpinAttempts = lockSpinAttempts;
        }

        public Class<?> getValueType() {
            return valueType;
        }

        public void setValueType(Class<?> valueType) {
            this.valueType = valueType;
        }
    }

    // ------------------------------------------------------------------
    // 解析：把"默认值 + 覆盖项"合并为不可变规格
    // ------------------------------------------------------------------

    /**
     * 解析指定缓存名的最终规格。
     *
     * @param cacheName 缓存名，见 {@link CacheNames}
     */
    public Resolved resolve(String cacheName) {
        CacheSpec spec = caches.getOrDefault(cacheName, new CacheSpec());

        boolean l1Enabled = l1.isEnabled()
                && (spec.getL1Enabled() == null ? true : spec.getL1Enabled());
        Duration l1Ttl = spec.getL1Ttl() != null ? spec.getL1Ttl() : l1.getDefaultTtl();
        long l1MaxSize = l1.getMaxSize();

        boolean l2Enabled = l2.isEnabled();
        Duration l2Ttl = spec.getL2Ttl() != null ? spec.getL2Ttl() : l2.getDefaultTtl();
        Duration l2NullTtl = spec.getL2NullTtl() != null ? spec.getL2NullTtl() : l2.getNullTtl();

        boolean nullCaching = spec.getNullCaching() == null || spec.getNullCaching();
        boolean distributedLock = spec.getDistributedLock() != null && spec.getDistributedLock();
        Duration lockLease = spec.getLockLease() != null ? spec.getLockLease() : Duration.ofSeconds(5);
        Duration lockSpinInterval = spec.getLockSpinInterval() != null
                ? spec.getLockSpinInterval() : Duration.ofMillis(50);
        int lockSpinAttempts = spec.getLockSpinAttempts() != null ? spec.getLockSpinAttempts() : 20;

        return new Resolved(
                cacheName,
                l1Enabled, l1MaxSize, l1Ttl,
                l2Enabled, l2.getKeyPrefix(), l2Ttl, l2NullTtl, l2.getTtlJitterRatio(),
                nullCaching,
                distributedLock, lockLease, lockSpinInterval, lockSpinAttempts,
                spec.getValueType()
        );
    }

    /**
     * 解析后的不可变缓存规格。构造后不再变化，可安全跨线程共享。
     */
    public record Resolved(
            String cacheName,
            boolean l1Enabled,
            long l1MaxSize,
            Duration l1Ttl,
            boolean l2Enabled,
            String l2KeyPrefix,
            Duration l2Ttl,
            Duration l2NullTtl,
            double ttlJitterRatio,
            boolean nullCaching,
            boolean distributedLock,
            Duration lockLease,
            Duration lockSpinInterval,
            int lockSpinAttempts,
            Class<?> valueType
    ) {
        /** L2 key：{@code vcache:<cacheName>:<key>} */
        public String l2Key(Object key) {
            return l2KeyPrefix + cacheName + ":" + key;
        }

        /** L2 扫描模式，用于 clear()。 */
        public String l2KeyPattern() {
            return l2KeyPrefix + cacheName + ":*";
        }

        /** 分布式锁 key。 */
        public String lockKey(Object key) {
            return l2KeyPrefix + "lock:" + cacheName + ":" + key;
        }

        /** 写入 L2 时使用的 TTL（含防雪崩抖动）。 */
        public Duration effectiveL2Ttl(boolean nullValue) {
            Duration base = nullValue ? l2NullTtl : l2Ttl;
            if (base == null || base.isZero() || base.isNegative() || ttlJitterRatio <= 0) {
                return base;
            }
            long jitter = (long) (base.toMillis() * ttlJitterRatio);
            if (jitter <= 0) {
                return base;
            }
            long delta = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
            long result = base.toMillis() + delta;
            return result > 0 ? Duration.ofMillis(result) : base;
        }
    }

    // ------------------------------------------------------------------
    // getters / setters
    // ------------------------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public L1 getL1() {
        return l1;
    }

    public void setL1(L1 l1) {
        this.l1 = l1;
    }

    public L2 getL2() {
        return l2;
    }

    public void setL2(L2 l2) {
        this.l2 = l2;
    }

    public Invalidation getInvalidation() {
        return invalidation;
    }

    public void setInvalidation(Invalidation invalidation) {
        this.invalidation = invalidation;
    }

    public Map<String, CacheSpec> getCaches() {
        return caches;
    }

    public void setCaches(Map<String, CacheSpec> caches) {
        this.caches = caches;
    }
}
