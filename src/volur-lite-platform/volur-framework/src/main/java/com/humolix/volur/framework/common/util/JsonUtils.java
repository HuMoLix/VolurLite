package com.humolix.volur.framework.common.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class JsonUtils {

    private static final JsonMapper DEFAULT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();

    private JsonUtils() {
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = SpringContextHolder.getBean(ObjectMapper.class);
        return mapper != null ? mapper : DEFAULT_MAPPER;
    }

    public static String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return getObjectMapper().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] toJsonBytes(Object value) {
        if (value == null) {
            return new byte[0];
        }
        try {
            return getObjectMapper().writeValueAsBytes(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> List<T> parseList(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = getObjectMapper();
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> Set<T> parseSet(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) {
            return Collections.emptySet();
        }
        try {
            ObjectMapper mapper = getObjectMapper();
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(Set.class, elementClass));
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode parseTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return getObjectMapper().readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ObjectNode createObjectNode() {
        return getObjectMapper().createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return getObjectMapper().createArrayNode();
    }
}
