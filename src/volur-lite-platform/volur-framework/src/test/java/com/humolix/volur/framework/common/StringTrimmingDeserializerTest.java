package com.humolix.volur.framework.common;

import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class StringTrimmingDeserializerTest {

    private final ObjectMapper objectMapper = new JsonMapper();

    @Data
    static class TestDTO {
        @Trimmed
        private String username;

        @Trimmed(toLowerCase = true)
        private String email;

        @Trimmed(emptyToNull = true)
        private String nickname;

        private String rawPassword;
    }

    @Test
    @DisplayName("测试 @Trimmed 注解自动去除首尾空格、转小写以及空串转 null")
    void testTrimmedDeserialization() throws Exception {
        String json = """
                {
                    "username": "   AliceBob   ",
                    "email": "   ALICE@EXAMPLE.COM   ",
                    "nickname": "    ",
                    "rawPassword": "  MySecretPassword  "
                }
                """;

        TestDTO dto = objectMapper.readValue(json, TestDTO.class);

        assertThat(dto.getUsername()).isEqualTo("AliceBob");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getNickname()).isNull();
        assertThat(dto.getRawPassword()).isEqualTo("  MySecretPassword  ");
    }
}
