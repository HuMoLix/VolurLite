package com.humolix.volur.framework.cache.invalidation;

import com.humolix.volur.framework.cache.VolurCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * 缓存失效广播订阅方：收到其他实例的失效通知后，清理<b>本机 L1</b>。
 *
 * <p><b>这里有两个必须讲清楚的设计约束：</b>
 *
 * <h3>1. 只清 L1，不清 L2，也不再广播</h3>
 * 失效的完整链路是：
 * <pre>
 *   发起实例：清本机 L1 → 删 L2 → 广播
 *   订阅实例：清本机 L1            ← 到此为止
 * </pre>
 * 如果订阅方也删 L2 并再次广播，就会形成 A → B → A 的无限回环，
 * 在广播风暴下直接打满 Redis 和所有实例的 CPU。
 *
 * <h3>2. 不过滤自己发出的消息</h3>
 * Redis Pub/Sub 会把消息投递给包括发布者在内的所有订阅者，本实例也会收到自己发的消息。
 * 这里<b>刻意不做 origin 过滤</b>：重复执行 {@code invalidate} 是幂等的（本来就已清过），
 * 加过滤反而多一条出错路径（比如多实例时钟不一致导致误过滤）。
 * {@code origin} 字段只用于日志排查。
 */
public class CacheEvictListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictListener.class);

    private final VolurCacheManager cacheManager;

    public CacheEvictListener(VolurCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            CacheEvictMessage msg = CacheEvictMessage.decode(body);
            if (msg == null || msg.cacheName() == null) {
                log.warn("[volur-cache] 收到无法解析的失效广播，已忽略。raw={}", body);
                return;
            }

            cacheManager.invalidateLocal(msg.cacheName(), msg.key(), msg.clearAll());

            if (log.isDebugEnabled()) {
                log.debug("[volur-cache] 已清理本机 L1。cache={}, key={}, clearAll={}, from={}",
                        msg.cacheName(), msg.key(), msg.clearAll(), msg.origin());
            }
        } catch (Exception ex) {
            // 单条消息处理失败不能影响监听容器后续消息
            log.warn("[volur-cache] 处理失效广播异常，已忽略", ex);
        }
    }
}
