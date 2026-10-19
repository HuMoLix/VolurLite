package com.humolix.volur.framework.common.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PageTokenUtil {

    private PageTokenUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CursorPayload {
        private Object sortValue;
        private String uniqueId;
    }

    public static String encodeToken(Object sortValue, String uniqueId) {
        if (sortValue == null || !StringUtils.hasText(uniqueId)) {
            return null;
        }
        try {
            CursorPayload payload = new CursorPayload(sortValue, uniqueId);
            String json = OBJECT_MAPPER.writeValueAsString(payload);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    public static CursorPayload decodeToken(String pageToken) {
        if (!StringUtils.hasText(pageToken)) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(pageToken);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(json, CursorPayload.class);
        } catch (Exception e) {
            return null;
        }
    }

}
