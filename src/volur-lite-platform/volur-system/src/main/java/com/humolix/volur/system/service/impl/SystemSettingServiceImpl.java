package com.humolix.volur.system.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO;
import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import com.humolix.volur.system.domain.vo.AdminSystemOverviewVO;
import com.humolix.volur.system.domain.vo.SystemSettingVO;
import com.humolix.volur.system.mapper.SystemMapper;
import com.humolix.volur.system.repository.SystemSettingRepository;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
    private static final String NULL_VALUE_PLACEHOLDER = "__NULL__";

    private static final Duration L2_CACHE_TTL = Duration.ofDays(7);
    private static final Duration L2_NULL_CACHE_TTL = Duration.ofMinutes(1);

    private final Environment environment;
    private final StringRedisTemplate redisTemplate;
    private final SystemSettingRepository systemSettingRepository;
    private final SystemMapper systemMapper;

    private Cache<String, String> localCache;

    @PostConstruct
    public void initLocalCache() {
        this.localCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    @Override
    public String getSettingValue(String key, String defaultValue) {
        if (!StringUtils.hasText(key)) {
            return defaultValue;
        }

        String cachedVal = localCache.get(key, this::loadFromL2OrDb);

        if (NULL_VALUE_PLACEHOLDER.equals(cachedVal) || cachedVal == null) {
            String envVal = environment.getProperty("volur.setting." + key.toLowerCase().replace('_', '-'));
            return StringUtils.hasText(envVal) ? envVal : defaultValue;
        }
        return cachedVal;
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

    private String loadFromL2OrDb(String key) {
        String redisKey = VREDIS_SYSTEM_SETTING_KEY_PREFIX + key;

        try {
            String redisVal = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.hasText(redisVal)) {
                return redisVal;
            }
        } catch (Exception ex) {
            log.error("Redis read error for key: {}", key, ex);
        }

        Optional<SystemSettingEntity> entityOpt = systemSettingRepository.findBySettingKey(key);
        if (entityOpt.isPresent() && StringUtils.hasText(entityOpt.get().getSettingValue())) {
            String dbVal = entityOpt.get().getSettingValue();
            try {
                redisTemplate.opsForValue().set(redisKey, dbVal, L2_CACHE_TTL);
            } catch (Exception ex) {
                log.error("Failed to write Redis cache for key: {}", key, ex);
            }
            return dbVal;
        }

        String envVal = environment.getProperty("volur.setting." + key.toLowerCase().replace('_', '-'));
        if (StringUtils.hasText(envVal)) {
            try {
                redisTemplate.opsForValue().set(redisKey, envVal, L2_CACHE_TTL);
            } catch (Exception ex) {
                log.error("Failed to write Redis cache for env key: {}", key, ex);
            }
            return envVal;
        }

        try {
            redisTemplate.opsForValue().set(redisKey, NULL_VALUE_PLACEHOLDER, L2_NULL_CACHE_TTL);
        } catch (Exception ignored) {
        }
        return NULL_VALUE_PLACEHOLDER;
    }

    @Override
    public Map<String, String> getPublicSettingsMap() {
        Map<String, String> map = systemSettingRepository.findByIsPublicTrue().stream()
                .filter(setting -> setting.getSettingKey().startsWith("system."))
                .collect(Collectors.toMap(
                        SystemSettingEntity::getSettingKey,
                        setting -> getSettingValue(setting.getSettingKey(), setting.getSettingValue()),
                        (existing, replacing) -> replacing
                ));

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
        String redisKey = VREDIS_SYSTEM_SETTING_KEY_PREFIX + key;
        try {
            redisTemplate.delete(redisKey);
            redisTemplate.convertAndSend(VREDIS_SYSTEM_SETTING_TOPIC, key);
        } catch (Exception ex) {
            log.error("Failed to evict Redis cache / publish topic for key: {}", key, ex);
        }
        evictLocalCache(key);
    }

    @Override
    public void evictLocalCache(String key) {
        if (localCache != null) {
            if ("*".equals(key)) {
                localCache.invalidateAll();
            } else {
                localCache.invalidate(key);
            }
        }
    }

}
