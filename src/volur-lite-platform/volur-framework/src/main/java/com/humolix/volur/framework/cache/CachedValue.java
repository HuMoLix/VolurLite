package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record CachedValue(boolean nullValue, String type, String elementType, String json) {

    private static final Logger log = LoggerFactory.getLogger(CachedValue.class);
    private static final CachedValue NULL_PLACEHOLDER = new CachedValue(true, null, null, null);

    private static final String[] ALLOWED_PACKAGE_PREFIXES = {
            "com.humolix.volur.",
            "java.lang.",
            "java.time.",
            "java.util.",
            "java.math."
    };

    public CachedValue(boolean nullValue, String type, String json) {
        this(nullValue, type, null, json);
    }

    public static CachedValue of(Object value) {
        if (value == null) {
            return NULL_PLACEHOLDER;
        }
        return new CachedValue(false, normalizeType(value), resolveElementType(value), JsonUtils.toJsonString(value));
    }

    private static String resolveElementType(Object value) {
        if (value instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item != null) {
                    return item.getClass().getName();
                }
            }
        }
        return null;
    }

    private static String normalizeType(Object value) {
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return Object[].class.getName();
        }
        if (value instanceof Map) {
            return Map.class.getName();
        }
        if (value instanceof Set) {
            return Set.class.getName();
        }
        if (value instanceof List) {
            return List.class.getName();
        }
        if (value instanceof Collection) {
            return Collection.class.getName();
        }
        return type.getName();
    }

    public static CachedValue decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = JsonUtils.parseTree(text);
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }
            if (node.path("n").asBoolean(false)) {
                return NULL_PLACEHOLDER;
            }
            String t = node.hasNonNull("t") ? node.get("t").asText() : null;
            String e = node.hasNonNull("e") ? node.get("e").asText() : null;
            String v = node.hasNonNull("v") ? node.get("v").asText() : null;
            return new CachedValue(false, t, e, v);
        } catch (Exception ex) {
            log.warn("[volur-cache] L2 decode failed, treated as cache miss. raw={}", abbreviate(text), ex);
            return null;
        }
    }

    public String encode() {
        if (nullValue) {
            return "{\"n\":true}";
        }
        ObjectNode obj = JsonUtils.createObjectNode();
        obj.put("n", false);
        obj.put("t", type);
        if (elementType != null) {
            obj.put("e", elementType);
        }
        obj.put("v", json);
        return JsonUtils.toJsonString(obj);
    }

    public Object unwrap(Class<?> preferredType) {
        if (nullValue) {
            return null;
        }
        Class<?> target = preferredType != null ? preferredType : resolveType(type);
        if (elementType != null && Collection.class.isAssignableFrom(target)) {
            Class<?> elementTarget = resolveType(elementType);
            if (Set.class.isAssignableFrom(target)) {
                return JsonUtils.parseSet(json, elementTarget);
            }
            return JsonUtils.parseList(json, elementTarget);
        }
        return JsonUtils.parseObject(json, target);
    }

    public boolean isNullPlaceholder() {
        return nullValue;
    }

    private static Class<?> resolveType(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            throw new IllegalStateException("缓存值缺少类型信息，无法反序列化");
        }
        if (!isAllowedType(fqcn)) {
            throw new IllegalStateException("缓存值类型不在白名单内，拒绝反序列化: " + fqcn);
        }
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("缓存值类型无法加载（可能已改名或删除）: " + fqcn, ex);
        }
    }

    private static boolean isAllowedType(String fqcn) {
        if (fqcn.startsWith("[")) {
            String component = fqcn;
            while (component.startsWith("[")) {
                component = component.substring(1);
            }
            if (component.startsWith("L") && component.endsWith(";")) {
                return isAllowedType(component.substring(1, component.length() - 1));
            }
            return !component.isEmpty();
        }
        for (String prefix : ALLOWED_PACKAGE_PREFIXES) {
            if (fqcn.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String abbreviate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
