package com.humolix.volur.framework.cache.invalidation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.time.Instant;

/**
 * 跨实例缓存失效广播消息体。
 *
 * <p>刻意保持极简（< 200 字节）：广播走的是 Redis Pub/Sub，消息只承载"哪个缓存的哪个 key 需要失效"
 * 这一最小信息，<b>不承载数据本身</b>。让订阅方自己去 L2/DB 重新加载，
 * 避免"广播大对象"导致 Redis 输出缓冲膨胀。
 *
 * @param cacheName 缓存名，对应 {@code CacheNames}
 * @param key       需要失效的 key；{@code clearAll} 为 true 时可为 null
 * @param clearAll  是否清空整个缓存
 * @param origin    发送方实例标识，仅用于日志排查（不用于过滤，见 {@code CacheEvictListener}）
 * @param timestamp 发送时间戳（毫秒）
 */
public record CacheEvictMessage(
        String cacheName,
        String key,
        boolean clearAll,
        String origin,
        long timestamp
) {

    public static CacheEvictMessage evict(String cacheName, String key, String origin) {
        return new CacheEvictMessage(cacheName, key, false, origin, Instant.now().toEpochMilli());
    }

    public static CacheEvictMessage clear(String cacheName, String origin) {
        return new CacheEvictMessage(cacheName, null, true, origin, Instant.now().toEpochMilli());
    }

    public String encode() {
        JSONObject obj = new JSONObject(5);
        obj.put("cacheName", cacheName);
        obj.put("key", key);
        obj.put("clearAll", clearAll);
        obj.put("origin", origin);
        obj.put("timestamp", timestamp);
        return obj.toJSONString();
    }

    /**
     * 显式解码。
     *
     * <p>不依赖 JSON 库对 record 的自动映射，避免不同版本行为差异导致静默解析失败
     * （失效链路一旦静默失败，L1 会一直脏到 TTL 到期，很难排查）。
     */
    public static CacheEvictMessage decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        JSONObject obj = JSON.parseObject(text);
        if (obj == null) {
            return null;
        }
        return new CacheEvictMessage(
                obj.getString("cacheName"),
                obj.getString("key"),
                Boolean.TRUE.equals(obj.getBoolean("clearAll")),
                obj.getString("origin"),
                obj.getLongValue("timestamp", Instant.now().toEpochMilli())
        );
    }
}
