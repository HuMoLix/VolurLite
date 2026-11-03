package com.humolix.volur.system.service;

import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import com.humolix.volur.system.mapper.SystemMapper;
import com.humolix.volur.system.repository.SystemSettingRepository;
import com.humolix.volur.system.service.impl.SystemSettingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceImplTest {

    @Mock
    private Environment environment;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private SystemMapper systemMapper;

    @Mock
    private VolurCacheTemplate cacheTemplate;

    @InjectMocks
    private SystemSettingServiceImpl systemSettingService;

    @Test
    @DisplayName("getPublicSettingsMap should extract public settings without recursive cache calls")
    void testGetPublicSettingsMapSuccess() throws Exception {
        SystemSettingEntity siteTitle = SystemSettingEntity.builder()
                .settingKey("system.site.title")
                .settingValue("Volur Lite Platform")
                .isPublic(true)
                .build();

        SystemSettingEntity siteLogo = SystemSettingEntity.builder()
                .settingKey("system.site.logo")
                .settingValue("")
                .isPublic(true)
                .build();

        SystemSettingEntity nonSystem = SystemSettingEntity.builder()
                .settingKey("custom.secret")
                .settingValue("hidden")
                .isPublic(true)
                .build();

        when(systemSettingRepository.findByIsPublicTrue()).thenReturn(List.of(siteTitle, siteLogo, nonSystem));
        when(environment.getProperty("volur.setting.system.site.logo")).thenReturn("/logo.png");

        when(cacheTemplate.getOrLoad(eq(CacheNames.SYSTEM_SETTING), eq("__PUBLIC_SETTINGS_MAP__"), any()))
                .thenAnswer(invocation -> {
                    Callable<?> loader = invocation.getArgument(2);
                    return loader.call();
                });

        Map<String, String> result = systemSettingService.getPublicSettingsMap();

        assertNotNull(result);
        assertEquals("Volur Lite Platform", result.get("system.site.title"));
        assertEquals("/logo.png", result.get("system.site.logo"));
        assertNull(result.get("custom.secret"));
        assertTrue(result.containsKey(SystemSettingKeys.MAINTENANCE_ENABLED));
        assertTrue(result.containsKey(SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE));

        verify(cacheTemplate, times(1)).getOrLoad(eq(CacheNames.SYSTEM_SETTING), eq("__PUBLIC_SETTINGS_MAP__"), any());
    }

}
