package com.humolix.volur.system.service.impl;

import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO;
import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import com.humolix.volur.system.domain.vo.AdminSystemOverviewVO;
import com.humolix.volur.system.domain.vo.SystemSettingVO;
import com.humolix.volur.system.mapper.SystemMapper;
import com.humolix.volur.system.repository.SystemSettingRepository;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    public static final String VREDIS_SYSTEM_SETTING_KEY_PREFIX = "vredis:system:setting:";
    public static final String VREDIS_SYSTEM_SETTING_TOPIC = "vredis:topic:system:setting";
    private static final String PUBLIC_SETTINGS_MAP_CACHE_KEY = "__PUBLIC_SETTINGS_MAP__";

    private final Environment environment;
    private final StringRedisTemplate redisTemplate;
    private final SystemSettingRepository systemSettingRepository;
    private final SystemMapper systemMapper;
    private final VolurCacheTemplate cacheTemplate;

    @Override
    public String getSettingValue(String key, String defaultValue) {
        if (!StringUtils.hasText(key)) {
            return defaultValue;
        }

        String cachedVal = cacheTemplate.getOrLoad(CacheNames.SYSTEM_SETTING, key, () -> {
            Optional<SystemSettingEntity> entityOpt = systemSettingRepository.findBySettingKey(key);
            if (entityOpt.isPresent() && StringUtils.hasText(entityOpt.get().getSettingValue())) {
                return entityOpt.get().getSettingValue();
            }
            String envVal = environment.getProperty("volur.setting." + key.toLowerCase().replace('_', '-'));
            return StringUtils.hasText(envVal) ? envVal : null;
        });

        return cachedVal != null ? cachedVal : defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getSettingValue(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String val = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer for setting key {}: '{}', using default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String val = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long for setting key {}: '{}', using default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public Map<String, String> getPublicSettingsMap() {
        @SuppressWarnings("unchecked")
        Map<String, String> cachedMap = cacheTemplate.getOrLoad(CacheNames.SYSTEM_SETTING, PUBLIC_SETTINGS_MAP_CACHE_KEY, () -> {
            return systemSettingRepository.findByIsPublicTrue().stream()
                    .filter(setting -> setting.getSettingKey().startsWith("system."))
                    .collect(Collectors.toMap(
                            SystemSettingEntity::getSettingKey,
                            setting -> getSettingValue(setting.getSettingKey(), setting.getSettingValue()),
                            (existing, replacing) -> replacing
                    ));
        });

        Map<String, String> map = new HashMap<>(cachedMap != null ? cachedMap : Map.of());
        String maintenanceEnabled = String.valueOf(getBoolean(com.humolix.volur.system.constant.SystemSettingKeys.MAINTENANCE_ENABLED, false));
        String maintenanceNotice = getSettingValue(com.humolix.volur.system.constant.SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE, "系统正在全站维护升级中，暂不可用，请稍后访问。");
        map.put(com.humolix.volur.system.constant.SystemSettingKeys.MAINTENANCE_ENABLED, maintenanceEnabled);
        map.put(com.humolix.volur.system.constant.SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE, maintenanceNotice);

        return map;
    }

    @Override
    public List<SystemSettingVO> getAllSettings() {
        return systemMapper.toSettingVOList(systemSettingRepository.findAll());
    }

    @Override
    public AdminSystemOverviewVO getSystemOverview() {
        String springBootVer = SpringBootVersion.getVersion();
        String javaSpecVer = System.getProperty("java.specification.version", "25");
        String javaVendor = System.getProperty("java.vendor", "Oracle Corporation");
        String javaFullVer = System.getProperty("java.version", "25");
        String osName = System.getProperty("os.name", "Windows 11");
        String osArch = System.getProperty("os.arch", "amd64");
        String osVersion = System.getProperty("os.version", "");

        String redisStatus = "DISCONNECTED";
        double redisLatencyMs = -1.0;
        try {
            long start = System.nanoTime();
            String pong = null;
            if (redisTemplate.getConnectionFactory() != null) {
                pong = redisTemplate.getConnectionFactory().getConnection().ping();
            }
            long costNs = System.nanoTime() - start;
            if ("PONG".equalsIgnoreCase(pong)) {
                redisStatus = "CONNECTED";
                redisLatencyMs = Math.round((costNs / 1_000_000.0) * 100.0) / 100.0;
            }
        } catch (Exception e) {
            log.warn("[SystemOverview] Redis 连通性探测异常: {}", e.getMessage());
            redisStatus = "DISCONNECTED";
        }

        long uptimeSeconds = 0;
        try {
            uptimeSeconds = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        } catch (Exception ignored) {
        }

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        return AdminSystemOverviewVO.builder()
                .kernelVersion("Volur Lite v1.0.0-SNAPSHOT")
                .springBootVersion(springBootVer)
                .javaVersion("JDK " + javaFullVer + " (" + javaVendor + ")")
                .runtimeEnv("Spring Boot " + springBootVer + " / JDK " + javaSpecVer)
                .osInfo(osName + " " + osVersion + " (" + osArch + ")")
                .redisStatus(redisStatus)
                .redisLatencyMs(redisLatencyMs)
                .sseStatus("ONLINE")
                .uptimeSeconds(uptimeSeconds)
                .heapMemoryUsedMb(usedMemoryMb)
                .heapMemoryMaxMb(maxMemoryMb)
                .build();
    }

    @Override
    @Transactional
    public void updateSetting(SystemSettingUpdateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getKey())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "配置项键名不能为空");
        }

        boolean isPublic = dto.getKey().startsWith("system.general.")
                || dto.getKey().startsWith("system.maintenance.")
                || com.humolix.volur.system.constant.SystemSettingKeys.CAPTCHA_ENABLED.equals(dto.getKey())
                || com.humolix.volur.system.constant.SystemSettingKeys.AUTH_REGISTRATION_ENABLED.equals(dto.getKey())
                || com.humolix.volur.system.constant.SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION.equals(dto.getKey());

        SystemSettingEntity entity = systemSettingRepository.findBySettingKey(dto.getKey())
                .orElseGet(() -> SystemSettingEntity.builder()
                        .settingKey(dto.getKey())
                        .settingGroup("GENERAL")
                        .valueType("STRING")
                        .isPublic(isPublic)
                        .build());

        entity.setSettingValue(dto.getValue() != null ? dto.getValue() : "");
        entity.setIsPublic(isPublic);
        systemSettingRepository.save(entity);

        evictCache(dto.getKey());
    }

    @Override
    @Transactional
    public void updateSettings(List<SystemSettingUpdateDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }
        for (SystemSettingUpdateDTO dto : dtoList) {
            updateSetting(dto);
        }
    }

    @Override
    public void evictCache(String key) {
        if ("*".equals(key)) {
            cacheTemplate.evictAll(CacheNames.SYSTEM_SETTING);
        } else {
            cacheTemplate.evict(CacheNames.SYSTEM_SETTING, key);
            cacheTemplate.evict(CacheNames.SYSTEM_SETTING, PUBLIC_SETTINGS_MAP_CACHE_KEY);
        }
    }

    @Override
    public void evictLocalCache(String key) {
        // 由 VolurCache 与 CacheEvictListener 自动在集群范围内单向监听广播并清理本机 L1
        evictCache(key);
    }

}
