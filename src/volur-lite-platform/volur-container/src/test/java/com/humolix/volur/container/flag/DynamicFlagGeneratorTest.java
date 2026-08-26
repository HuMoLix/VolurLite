package com.humolix.volur.container.flag;

import com.humolix.volur.container.flag.resolver.IdentityPlaceholderResolver;
import com.humolix.volur.container.flag.resolver.RandomPlaceholderResolver;
import com.humolix.volur.container.flag.resolver.SecurityPlaceholderResolver;
import com.humolix.volur.system.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

class DynamicFlagGeneratorTest {

    private SystemSettingService systemSettingService;
    private DynamicFlagGenerator dynamicFlagGenerator;

    @BeforeEach
    void setUp() {
        systemSettingService = Mockito.mock(SystemSettingService.class);
        IdentityPlaceholderResolver identityResolver = new IdentityPlaceholderResolver();
        RandomPlaceholderResolver randomResolver = new RandomPlaceholderResolver();
        SecurityPlaceholderResolver securityResolver = new SecurityPlaceholderResolver();

        FlagTemplateParser flagTemplateParser = new FlagTemplateParser(List.of(identityResolver, randomResolver, securityResolver));
        dynamicFlagGenerator = new DynamicFlagGenerator(systemSettingService, flagTemplateParser);
    }

    @Test
    @DisplayName("测试自定义模版 VolurCTF{{UUID}} 生成")
    void testVolurCtfUuidTemplate() {
        when(systemSettingService.getSettingValue(eq("DYNAMIC_FLAG_TEMPLATE"), isNull()))
                .thenReturn("VolurCTF{{UUID}}");

        DynamicFlagContext context = DynamicFlagContext.builder()
                .userId(1001L)
                .challengeId(2001L)
                .build();

        String flag = dynamicFlagGenerator.generateFlag(context);

        assertNotNull(flag);
        assertTrue(flag.startsWith("VolurCTF{"));
        assertTrue(flag.endsWith("}"));
        // Check standard UUID format inside: 8-4-4-4-12
        String content = flag.substring("VolurCTF{".length(), flag.length() - 1);
        assertEquals(36, content.length());
        assertTrue(content.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
    }

    @Test
    @DisplayName("测试嵌套前缀模版 flag{flag-{UUID}} 生成")
    void testNestedFlagTemplate() {
        when(systemSettingService.getSettingValue(eq("DYNAMIC_FLAG_TEMPLATE"), isNull()))
                .thenReturn("flag{flag-{UUID}}");

        DynamicFlagContext context = DynamicFlagContext.builder()
                .userId(1001L)
                .challengeId(2001L)
                .build();

        String flag = dynamicFlagGenerator.generateFlag(context);

        assertNotNull(flag);
        assertTrue(flag.startsWith("flag{flag-"));
        assertTrue(flag.endsWith("}"));
        String innerUuid = flag.substring("flag{flag-".length(), flag.length() - 1);
        assertEquals(36, innerUuid.length());
    }

    @Test
    @DisplayName("测试队伍 Token 与多种占位符扩展 VolurCTF{team_{TEAM_TOKEN}_{USER_ID}_{UUID_SIMPLE}}")
    void testTeamTokenAndMultiPlaceholders() {
        when(systemSettingService.getSettingValue(eq("DYNAMIC_FLAG_TEMPLATE"), isNull()))
                .thenReturn("VolurCTF{team_{TEAM_TOKEN}_{USER_ID}_{UUID_SIMPLE}}");

        DynamicFlagContext context = DynamicFlagContext.builder()
                .userId(8888L)
                .challengeId(101L)
                .stageId(2L)
                .teamId(501L)
                .teamToken("ALPHA_SEC_TOKEN")
                .build();

        String flag = dynamicFlagGenerator.generateFlag(context);

        assertNotNull(flag);
        assertTrue(flag.startsWith("VolurCTF{team_ALPHA_SEC_TOKEN_8888_"));
        assertTrue(flag.endsWith("}"));
        String simpleUuid = flag.substring("VolurCTF{team_ALPHA_SEC_TOKEN_8888_".length(), flag.length() - 1);
        assertEquals(32, simpleUuid.length());
    }

    @Test
    @DisplayName("测试动态参数化占位符 RANDOM:20 与 HMAC 防作弊水印 SIGN")
    void testDynamicLengthAndWatermark() {
        when(systemSettingService.getSettingValue(eq("DYNAMIC_FLAG_TEMPLATE"), isNull()))
                .thenReturn("flag{u_{USER_ID}_r_{RANDOM:20}_w_{SIGN}}");

        DynamicFlagContext context = DynamicFlagContext.builder()
                .userId(9999L)
                .challengeId(777L)
                .teamId(333L)
                .build();

        String flag = dynamicFlagGenerator.generateFlag(context);

        assertNotNull(flag);
        assertTrue(flag.startsWith("flag{u_9999_r_"));
        assertTrue(flag.endsWith("}"));
        assertTrue(flag.contains("_w_"));
    }

    @Test
    @DisplayName("测试模版合法性校验与占位符提取")
    void testTemplateValidation() {
        String validTemplate = "flag{u_{USER_ID}_{UUID}_{TEAM_TOKEN}_{HEX:16}}";
        List<String> unsupported = dynamicFlagGenerator.validateTemplate(validTemplate);
        assertTrue(unsupported.isEmpty(), "所有合法占位符应均被支持");

        String invalidTemplate = "flag{u_{USER_ID}_{UNKNOWN_TEST_PLACEHOLDER}}";
        List<String> invalidList = dynamicFlagGenerator.validateTemplate(invalidTemplate);
        assertEquals(1, invalidList.size());
        assertEquals("UNKNOWN_TEST_PLACEHOLDER", invalidList.getFirst());
    }

    @Test
    @DisplayName("测试默认回退模版 flag{...}")
    void testDefaultFallback() {
        when(systemSettingService.getSettingValue(eq("DYNAMIC_FLAG_TEMPLATE"), isNull()))
                .thenReturn(null);
        when(systemSettingService.getSettingValue(eq("FLAG_TEMPLATE"), isNull()))
                .thenReturn(null);
        when(systemSettingService.getSettingValue(eq("FLAG_PREFIX"), eq("flag{")))
                .thenReturn("flag{");
        when(systemSettingService.getSettingValue(eq("FLAG_SUFFIX"), eq("}")))
                .thenReturn("}");

        DynamicFlagContext context = DynamicFlagContext.builder()
                .userId(1001L)
                .build();

        String flag = dynamicFlagGenerator.generateFlag(context);

        assertNotNull(flag);
        assertTrue(flag.startsWith("flag{"));
        assertTrue(flag.endsWith("}"));
    }
}
