package com.humolix.volur.framework.cache.invalidation;

import com.humolix.volur.framework.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

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
        ObjectNode obj = JsonUtils.createObjectNode();
        obj.put("cacheName", cacheName);
        obj.put("key", key);
        obj.put("clearAll", clearAll);
        obj.put("origin", origin);
        obj.put("timestamp", timestamp);
        return JsonUtils.toJsonString(obj);
    }

    public static CacheEvictMessage decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        JsonNode node = JsonUtils.parseTree(text);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new CacheEvictMessage(
                node.hasNonNull("cacheName") ? node.get("cacheName").asText() : null,
                node.hasNonNull("key") ? node.get("key").asText() : null,
                node.path("clearAll").asBoolean(false),
                node.hasNonNull("origin") ? node.get("origin").asText() : null,
                node.path("timestamp").asLong(Instant.now().toEpochMilli())
        );
    }
}
