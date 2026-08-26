package com.humolix.volur.framework.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageTokenUtilTest {

    @Test
    @DisplayName("Test encode and decode cursor tokens")
    void testEncodeAndDecodeToken() {
        Long sortVal = 1724600000000L;
        String uniqueId = "user_1001";

        String token = PageTokenUtil.encodeToken(sortVal, uniqueId);
        assertNotNull(token);
        assertFalse(token.isBlank());

        PageTokenUtil.CursorPayload payload = PageTokenUtil.decodeToken(token);
        assertNotNull(payload);
        assertEquals(uniqueId, payload.getUniqueId());
        assertNotNull(payload.getSortValue());
    }

    @Test
    @DisplayName("Test null and malformed tokens")
    void testNullAndMalformedTokens() {
        assertNull(PageTokenUtil.encodeToken(null, "123"));
        assertNull(PageTokenUtil.encodeToken(123, null));
        assertNull(PageTokenUtil.encodeToken(123, "   "));

        assertNull(PageTokenUtil.decodeToken(null));
        assertNull(PageTokenUtil.decodeToken(""));
        assertNull(PageTokenUtil.decodeToken("invalid-base64-payload!!!"));
    }

}
