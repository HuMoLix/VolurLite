package com.humolix.volur.framework.cache.invalidation;

import com.humolix.volur.framework.cache.VolurCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

/**
 * 缓存失效广播发送方。
 *
 * <p><b>设计取舍：广播失败不阻断主流程。</b>
 * 失效广播是"降低脏数据窗口"的优化手段，而不是正确性保证——正确性由 L1 的 TTL 兜底。
 * 因此发送失败只记 WARN，不抛异常。反过来若把广播做成强依赖，
 * Redis Pub/Sub 抖动就会让业务写操作失败，得不偿失。
 */
public class CacheEvictBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictBroadcaster.class);

    private final StringRedisTemplate redis;
    private final VolurCacheProperties properties;

    /** 本实例标识，仅用于日志区分消息来源。 */
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    public CacheEvictBroadcaster(StringRedisTemplate redis, VolurCacheProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /**
     * 广播单个 key 的失效。
     *
     * @return {@code true} 表示已投递（或因开关关闭而按预期跳过）；
     *         {@code false} 表示投递抛异常。调用方据此累计
     *         {@code volur.cache.invalidation.failed} 指标——把"广播失败"变成可观测信号，
     *         否则它会安静地退化成一个 TTL 长度的脏数据窗口而无人知晓。
     */
    public boolean broadcastEvict(String cacheName, String key) {
        return publish(CacheEvictMessage.evict(cacheName, key, instanceId));
    }

    /** 广播整个缓存的失效。返回值语义同 {@link #broadcastEvict}。 */
    public boolean broadcastClear(String cacheName) {
        return publish(CacheEvictMessage.clear(cacheName, instanceId));
    }

    public String instanceId() {
        return instanceId;
    }

    private boolean publish(CacheEvictMessage message) {
        if (!properties.getInvalidation().isEnabled()) {
            // 刻意返回 true：开关关闭是运维的主动决策，不是故障，
            // 若计入失败率会让告警在"已关闭广播"的正常状态下持续误报。
            return true;
        }
        try {
            redis.convertAndSend(properties.getInvalidation().getTopic(), message.encode());
            return true;
        } catch (Exception ex) {
            // L1 TTL 会兜底，这里不阻断业务
            log.warn("[volur-cache] 失效广播发送失败（L1 TTL 将兜底）。cache={}, key={}",
                    message.cacheName(), message.key(), ex);
            return false;
        }
    }
}
