package com.humolix.volur.verification.service.admin.impl;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.dto.AdminIpRuleCreateDTO;
import com.humolix.volur.verification.domain.dto.AdminIpRuleQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskPolicyDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskSettingsDTO;
import com.humolix.volur.verification.domain.entity.RiskIpRule;
import com.humolix.volur.verification.domain.enums.IpRuleSource;
import com.humolix.volur.verification.domain.enums.IpRuleStatus;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import com.humolix.volur.verification.domain.vo.*;
import com.humolix.volur.verification.mapper.RiskIpRuleMapper;
import com.humolix.volur.verification.repository.RiskIpRuleRepository;
import com.humolix.volur.verification.risk.event.RiskAutoBanEvent;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.admin.AdminRiskService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRiskServiceImpl implements AdminRiskService {

    private static final String VREDIS_VERIFICATION_RISK_BAN_IP_KEY_PREFIX = "vredis:verification:risk:ban:ip:";
    private static final String VREDIS_VERIFICATION_RISK_WHITELIST_IP_KEY_PREFIX = "vredis:verification:risk:whitelist:ip:";
    private static final String VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX = "vredis:verification:risk:failures:";
    private static final String VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX = "vredis:verification:risk:velocity:";

    private final RiskIpRuleRepository ipRuleRepository;
    private final RiskIpRuleMapper ipRuleMapper;
    private final SystemSettingService systemSettingService;
    private final StringRedisTemplate redisTemplate;
    private final RedisSlidingWindowRateLimiter rateLimiter;

    @Override
    public AdminRiskSettingsVO getSettings() {
        return AdminRiskSettingsVO.builder()
                .enabled(systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true))
                .ipFilterEnabled(systemSettingService.getBoolean(SystemSettingKeys.RISK_IP_FILTER_ENABLED, true))
                .defaultBanDurationMinutes(systemSettingService.getInt(SystemSettingKeys.RISK_DEFAULT_BAN_DURATION_MINUTES, 1440))
                .banNoticeMessage(systemSettingService.getSettingValue(SystemSettingKeys.RISK_BAN_NOTICE_MESSAGE, "您的 IP 存在异常访问行为，已被系统安全机制拦截。如需解封请联系平台管理员。"))
                .bypassLoopback(systemSettingService.getBoolean(SystemSettingKeys.RISK_BYPASS_LOOPBACK, true))
                .captchaEnabled(systemSettingService.getBoolean(SystemSettingKeys.CAPTCHA_ENABLED, true))
                .captchaTolerance(systemSettingService.getInt(SystemSettingKeys.CAPTCHA_TOLERANCE_PX, 8))
                .captchaTicketTtl(systemSettingService.getInt(SystemSettingKeys.CAPTCHA_TICKET_TTL_SECONDS, 300))
                .captchaMaxAttempts(systemSettingService.getInt(SystemSettingKeys.CAPTCHA_MAX_ATTEMPTS, 5))
                .forceCaptchaOnReset(systemSettingService.getBoolean(SystemSettingKeys.CAPTCHA_FORCE_RESET, true))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(AdminRiskSettingsDTO dto, Long operatorId) {
        saveSetting(SystemSettingKeys.RISK_ENABLED, String.valueOf(dto.getEnabled()));
        saveSetting(SystemSettingKeys.RISK_IP_FILTER_ENABLED, String.valueOf(dto.getIpFilterEnabled()));
        saveSetting(SystemSettingKeys.RISK_DEFAULT_BAN_DURATION_MINUTES, String.valueOf(dto.getDefaultBanDurationMinutes()));
        saveSetting(SystemSettingKeys.RISK_BAN_NOTICE_MESSAGE, dto.getBanNoticeMessage());
        saveSetting(SystemSettingKeys.RISK_BYPASS_LOOPBACK, String.valueOf(dto.getBypassLoopback()));
        saveSetting(SystemSettingKeys.CAPTCHA_ENABLED, String.valueOf(dto.getCaptchaEnabled()));
        saveSetting(SystemSettingKeys.CAPTCHA_TOLERANCE_PX, String.valueOf(dto.getCaptchaTolerance()));
        saveSetting(SystemSettingKeys.CAPTCHA_TICKET_TTL_SECONDS, String.valueOf(dto.getCaptchaTicketTtl()));
        saveSetting(SystemSettingKeys.CAPTCHA_MAX_ATTEMPTS, String.valueOf(dto.getCaptchaMaxAttempts()));
        saveSetting(SystemSettingKeys.CAPTCHA_FORCE_RESET, String.valueOf(dto.getForceCaptchaOnReset()));
        log.info("[AdminRiskService] 管理员 [{}] 更新了风控全局基础设置", operatorId);
    }

    @Override
    public AdminRiskPolicyVO getPolicy() {
        String whitelistStr = systemSettingService.getSettingValue(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, "");
        List<String> emailWhitelist = org.springframework.util.StringUtils.hasText(whitelistStr)
                ? java.util.Arrays.stream(whitelistStr.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(org.springframework.util.StringUtils::hasText)
                        .map(s -> s.startsWith("@") ? s.substring(1) : s)
                        .toList()
                : java.util.Collections.emptyList();

        return AdminRiskPolicyVO.builder()
                .emailWhitelistSuffixes(emailWhitelist)
                .rateLimitWindow(systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW, 60))
                .rateLimitMaxRequests(systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS, 120))
                .velocityMediumThreshold(systemSettingService.getInt(SystemSettingKeys.RISK_VELOCITY_MEDIUM_THRESHOLD, 12))
                .accountFailuresMediumThreshold(systemSettingService.getInt(SystemSettingKeys.RISK_ACCOUNT_FAILURES_MEDIUM_THRESHOLD, 4))
                .accountFailuresHighThreshold(systemSettingService.getInt(SystemSettingKeys.RISK_ACCOUNT_FAILURES_HIGH_THRESHOLD, 8))
                .failuresMediumThreshold(systemSettingService.getInt(SystemSettingKeys.RISK_FAILURES_MEDIUM_THRESHOLD, 5))
                .failuresHighThreshold(systemSettingService.getInt(SystemSettingKeys.RISK_FAILURES_HIGH_THRESHOLD, 10))
                .failuresBanThreshold(systemSettingService.getInt(SystemSettingKeys.RISK_FAILURES_BAN_THRESHOLD, 20))
                .recoveryStepCount(systemSettingService.getInt(SystemSettingKeys.RISK_RECOVERY_STEP_COUNT, 6))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePolicy(AdminRiskPolicyDTO dto, Long operatorId) {
        String whitelistStr = dto.getEmailWhitelistSuffixes() != null
                ? dto.getEmailWhitelistSuffixes().stream()
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(org.springframework.util.StringUtils::hasText)
                        .map(s -> s.startsWith("@") ? s.substring(1) : s)
                        .collect(java.util.stream.Collectors.joining(","))
                : "";
        saveSetting(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, whitelistStr);

        saveSetting(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW, String.valueOf(dto.getRateLimitWindow()));
        saveSetting(SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS, String.valueOf(dto.getRateLimitMaxRequests()));
        saveSetting(SystemSettingKeys.RISK_VELOCITY_MEDIUM_THRESHOLD, String.valueOf(dto.getVelocityMediumThreshold()));
        saveSetting(SystemSettingKeys.RISK_ACCOUNT_FAILURES_MEDIUM_THRESHOLD, String.valueOf(dto.getAccountFailuresMediumThreshold()));
        saveSetting(SystemSettingKeys.RISK_ACCOUNT_FAILURES_HIGH_THRESHOLD, String.valueOf(dto.getAccountFailuresHighThreshold()));
        saveSetting(SystemSettingKeys.RISK_FAILURES_MEDIUM_THRESHOLD, String.valueOf(dto.getFailuresMediumThreshold()));
        saveSetting(SystemSettingKeys.RISK_FAILURES_HIGH_THRESHOLD, String.valueOf(dto.getFailuresHighThreshold()));
        saveSetting(SystemSettingKeys.RISK_FAILURES_BAN_THRESHOLD, String.valueOf(dto.getFailuresBanThreshold()));
        saveSetting(SystemSettingKeys.RISK_RECOVERY_STEP_COUNT, String.valueOf(dto.getRecoveryStepCount()));
        log.info("[AdminRiskService] 管理员 [{}] 更新了风控策略配置 (邮件/拦截/封禁策略)", operatorId);
    }

    private void saveSetting(String key, String value) {
        systemSettingService.updateSetting(new com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO(key, value));
    }

    @Override
    public PageResponse<AdminIpRuleVO> listIpRules(AdminIpRuleQueryDTO queryDTO) {
        int page = queryDTO.getPage() != null && queryDTO.getPage() > 0 ? queryDTO.getPage() - 1 : 0;
        int size = queryDTO.getSize() != null && queryDTO.getSize() > 0 ? queryDTO.getSize() : 20;

        Sort sort = "asc".equalsIgnoreCase(queryDTO.getSortOrder())
                ? Sort.by(queryDTO.getSortBy() != null ? queryDTO.getSortBy() : "createdAt").ascending()
                : Sort.by(queryDTO.getSortBy() != null ? queryDTO.getSortBy() : "createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<RiskIpRule> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(queryDTO.getIp())) {
                predicates.add(cb.like(root.get("ip"), "%" + queryDTO.getIp() + "%"));
            }
            if (queryDTO.getRuleType() != null) {
                predicates.add(cb.equal(root.get("ruleType"), queryDTO.getRuleType()));
            }
            if (queryDTO.getSourceType() != null) {
                predicates.add(cb.equal(root.get("sourceType"), queryDTO.getSourceType()));
            }
            if (queryDTO.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), queryDTO.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RiskIpRule> resultPage = ipRuleRepository.findAll(spec, pageable);
        List<AdminIpRuleVO> voList = ipRuleMapper.toVOList(resultPage.getContent());
        return new PageResponse<>(voList, resultPage.getTotalElements(), page + 1, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createIpRule(AdminIpRuleCreateDTO dto, Long operatorId, String operatorName, String operatorIp) {
        String cleanIp = dto.getIp();
        Instant expiresAt = null;
        if (dto.getDurationMinutes() != null && dto.getDurationMinutes() > 0) {
            expiresAt = Instant.now().plus(Duration.ofMinutes(dto.getDurationMinutes()));
        }

        // 检查是否存在相同 IP 和规则类型的生效记录
        Optional<RiskIpRule> existingOpt = ipRuleRepository.findFirstByIpAndRuleTypeAndStatusOrderByCreatedAtDesc(
                cleanIp, dto.getRuleType(), IpRuleStatus.ACTIVE
        );

        RiskIpRule entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setReason(dto.getReason());
            entity.setExpiresAt(expiresAt);
            entity.setOperatorId(operatorId);
            entity.setOperatorName(operatorName);
            entity.setOperatorIp(operatorIp);
            entity.setStatus(IpRuleStatus.ACTIVE);
        } else {
            entity = RiskIpRule.builder()
                    .ip(cleanIp)
                    .ruleType(dto.getRuleType())
                    .sourceType(IpRuleSource.MANUAL)
                    .reason(dto.getReason())
                    .status(IpRuleStatus.ACTIVE)
                    .expiresAt(expiresAt)
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .operatorIp(operatorIp)
                    .build();
        }

        ipRuleRepository.save(entity);

        // 同步刷新 Redis 缓存
        syncRuleToRedis(entity);
        log.info("[AdminRiskService] 管理员 [{}] 手动添加/更新 IP 规则 [{} - {}], 到期时间: {}",
                operatorName, cleanIp, dto.getRuleType(), expiresAt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeIpRule(Long id, Long operatorId, String operatorName) {
        RiskIpRule entity = ipRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "IP 规则记录不存在"));

        entity.setStatus(IpRuleStatus.REVOKED);
        ipRuleRepository.save(entity);

        // 从 Redis 移除
        removeRuleFromRedis(entity.getIp(), entity.getRuleType());
        log.info("[AdminRiskService] 管理员 [{}] 撤销解封了 IP 规则 [{} - {}]", operatorName, entity.getIp(), entity.getRuleType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIpRule(Long id) {
        RiskIpRule entity = ipRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "IP 规则记录不存在"));

        removeRuleFromRedis(entity.getIp(), entity.getRuleType());
        ipRuleRepository.delete(entity);
        log.info("[AdminRiskService] 删除 IP 规则 [{} - {}]", entity.getIp(), entity.getRuleType());
    }

    @Override
    public AdminIpDiagnosisVO diagnoseIp(String ip) {
        String cleanIp = ip != null ? ip.trim() : "";
        boolean isBypassed = isBypassedIp(cleanIp);
        boolean isWhitelisted = isIpWhitelisted(cleanIp);
        boolean isBanned = isIpBanned(cleanIp);

        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + cleanIp;
        long recentFailures = rateLimiter.getCurrentCount(failureKey, Duration.ofMinutes(10));

        String velocityKey = VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX + cleanIp;
        long recentVelocity = rateLimiter.getCurrentCount(velocityKey, Duration.ofMinutes(1));

        String riskLevel = "LOW";
        if (isBanned) {
            riskLevel = "BANNED";
        } else if (recentFailures >= 10) {
            riskLevel = "HIGH";
        } else if (recentFailures >= 5 || recentVelocity >= 12) {
            riskLevel = "MEDIUM";
        }

        String banReason = null;
        Instant banExpiresAt = null;
        String banSource = null;

        if (isBanned) {
            Optional<RiskIpRule> bannedRuleOpt = ipRuleRepository.findFirstByIpAndRuleTypeAndStatusOrderByCreatedAtDesc(
                    cleanIp, IpRuleType.BLACKLIST, IpRuleStatus.ACTIVE
            );
            if (bannedRuleOpt.isPresent()) {
                RiskIpRule rule = bannedRuleOpt.get();
                banReason = rule.getReason();
                banExpiresAt = rule.getExpiresAt();
                banSource = rule.getSourceType().name();
            } else {
                banReason = redisTemplate.opsForValue().get(VREDIS_VERIFICATION_RISK_BAN_IP_KEY_PREFIX + cleanIp);
            }
        }

        String profileKey = "vredis:verification:risk:client_profile:" + cleanIp;
        String profileData = redisTemplate.opsForValue().get(profileKey);
        String deviceFp = null;
        String browserFamily = null;
        String tlsProtocol = null;
        String tlsCipher = null;
        String tlsAlpn = null;

        if (StringUtils.hasText(profileData)) {
            String[] parts = profileData.split(":::", -1);
            if (parts.length >= 5) {
                deviceFp = !"-".equals(parts[0]) ? parts[0] : null;
                browserFamily = !"-".equals(parts[1]) ? parts[1] : null;
                tlsProtocol = !"-".equals(parts[2]) ? parts[2] : null;
                tlsCipher = !"-".equals(parts[3]) ? parts[3] : null;
                tlsAlpn = !"-".equals(parts[4]) ? parts[4] : null;
            }
        }

        return AdminIpDiagnosisVO.builder()
                .ip(cleanIp)
                .isBanned(isBanned)
                .isWhitelisted(isWhitelisted)
                .isBypassed(isBypassed)
                .riskLevel(riskLevel)
                .recentFailures(recentFailures)
                .recentVelocity(recentVelocity)
                .banReason(banReason)
                .banExpiresAt(banExpiresAt)
                .banSource(banSource)
                .deviceFingerprint(deviceFp)
                .browserFamily(browserFamily)
                .tlsProtocol(tlsProtocol)
                .tlsCipher(tlsCipher)
                .tlsAlpn(tlsAlpn)
                .build();
    }

    @Override
    public AdminRiskStatisticsVO getStatistics() {
        long totalBanned = ipRuleRepository.countByRuleType(IpRuleType.BLACKLIST);
        long activeBanned = ipRuleRepository.countByRuleTypeAndStatus(IpRuleType.BLACKLIST, IpRuleStatus.ACTIVE);
        long totalWhitelisted = ipRuleRepository.countByRuleTypeAndStatus(IpRuleType.WHITELIST, IpRuleStatus.ACTIVE);

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long todayAutoBanned = ipRuleRepository.countBySourceTypeAndCreatedAtAfter(IpRuleSource.AUTO_TRIGGER, startOfToday);
        long todayManualBanned = ipRuleRepository.countBySourceTypeAndCreatedAtAfter(IpRuleSource.MANUAL, startOfToday);

        return AdminRiskStatisticsVO.builder()
                .totalBannedCount(totalBanned)
                .activeBannedCount(activeBanned)
                .totalWhitelistedCount(totalWhitelisted)
                .todayAutoBannedCount(todayAutoBanned)
                .todayManualBannedCount(todayManualBanned)
                .build();
    }

    @Override
    public boolean isIpBanned(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        String cleanIp = ip.trim();

        // 1. 本地回环与内网白名单放行
        boolean bypassLoopback = systemSettingService.getBoolean(SystemSettingKeys.RISK_BYPASS_LOOPBACK, true);
        if (bypassLoopback && isBypassedIp(cleanIp)) {
            return false;
        }

        // 2. 检查 IP 过滤拦截总开关
        boolean ipFilterEnabled = systemSettingService.getBoolean(SystemSettingKeys.RISK_IP_FILTER_ENABLED, true);
        if (!ipFilterEnabled) {
            return false;
        }

        // 3. 检查白名单
        if (isIpWhitelisted(cleanIp)) {
            return false;
        }

        // 4. 极速查询 Redis 黑名单缓存
        String banKey = VREDIS_VERIFICATION_RISK_BAN_IP_KEY_PREFIX + cleanIp;
        Boolean exists = redisTemplate.hasKey(banKey);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        // 5. 缓存未命中时回退探测数据库有效记录并同步缓存
        Optional<RiskIpRule> ruleOpt = ipRuleRepository.findFirstByIpAndRuleTypeAndStatusOrderByCreatedAtDesc(
                cleanIp, IpRuleType.BLACKLIST, IpRuleStatus.ACTIVE
        );
        if (ruleOpt.isPresent()) {
            RiskIpRule rule = ruleOpt.get();
            if (rule.isExpired()) {
                rule.setStatus(IpRuleStatus.EXPIRED);
                ipRuleRepository.save(rule);
                return false;
            }
            syncRuleToRedis(rule);
            return true;
        }

        return false;
    }

    @Override
    public boolean isIpWhitelisted(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        String cleanIp = ip.trim();
        String whitelistKey = VREDIS_VERIFICATION_RISK_WHITELIST_IP_KEY_PREFIX + cleanIp;
        Boolean exists = redisTemplate.hasKey(whitelistKey);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        Optional<RiskIpRule> ruleOpt = ipRuleRepository.findFirstByIpAndRuleTypeAndStatusOrderByCreatedAtDesc(
                cleanIp, IpRuleType.WHITELIST, IpRuleStatus.ACTIVE
        );
        if (ruleOpt.isPresent()) {
            RiskIpRule rule = ruleOpt.get();
            if (rule.isExpired()) {
                rule.setStatus(IpRuleStatus.EXPIRED);
                ipRuleRepository.save(rule);
                return false;
            }
            syncRuleToRedis(rule);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoBanIp(String ip, String reason) {
        if (!StringUtils.hasText(ip) || isBypassedIp(ip) || isIpWhitelisted(ip)) {
            return;
        }
        String cleanIp = ip.trim();

        // 若已处于封禁中，不重复触发
        if (isIpBanned(cleanIp)) {
            return;
        }

        int durationMinutes = systemSettingService.getInt(SystemSettingKeys.RISK_DEFAULT_BAN_DURATION_MINUTES, 1440);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(durationMinutes));

        RiskIpRule entity = RiskIpRule.builder()
                .ip(cleanIp)
                .ruleType(IpRuleType.BLACKLIST)
                .sourceType(IpRuleSource.AUTO_TRIGGER)
                .reason(StringUtils.hasText(reason) ? reason : "触发系统连续高频风险行为自动封禁")
                .status(IpRuleStatus.ACTIVE)
                .expiresAt(expiresAt)
                .operatorName("SYSTEM_AUTO")
                .operatorIp("127.0.0.1")
                .build();

        ipRuleRepository.save(entity);
        syncRuleToRedis(entity);
        log.warn("[AdminRiskService] 系统已自动封禁恶意 IP: [{}], 原因: [{}], 解封时间: {}", cleanIp, reason, expiresAt);
    }

    @EventListener
    public void onAutoBanEvent(RiskAutoBanEvent event) {
        autoBanIp(event.getIp(), event.getReason());
    }

    private void syncRuleToRedis(RiskIpRule rule) {
        if (rule.getRuleType() == IpRuleType.BLACKLIST) {
            String key = VREDIS_VERIFICATION_RISK_BAN_IP_KEY_PREFIX + rule.getIp();
            String value = StringUtils.hasText(rule.getReason()) ? rule.getReason() : "BANNED";
            if (rule.getExpiresAt() != null) {
                Duration ttl = Duration.between(Instant.now(), rule.getExpiresAt());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(key, value, ttl);
                }
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            redisTemplate.delete(VREDIS_VERIFICATION_RISK_WHITELIST_IP_KEY_PREFIX + rule.getIp());
        } else if (rule.getRuleType() == IpRuleType.WHITELIST) {
            String key = VREDIS_VERIFICATION_RISK_WHITELIST_IP_KEY_PREFIX + rule.getIp();
            if (rule.getExpiresAt() != null) {
                Duration ttl = Duration.between(Instant.now(), rule.getExpiresAt());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(key, "WHITELIST", ttl);
                }
            } else {
                redisTemplate.opsForValue().set(key, "WHITELIST");
            }
            redisTemplate.delete(VREDIS_VERIFICATION_RISK_BAN_IP_KEY_PREFIX + rule.getIp());
        }
    }

    private void removeRuleFromRedis(String ip, IpRuleType ruleType) {
        if (ruleType == IpRuleType.BLACKLIST) {
            redisTemplate.delete(VREDIS_VERIFICATION_RISK_BAN_IP_KEY_PREFIX + ip);
        } else if (ruleType == IpRuleType.WHITELIST) {
            redisTemplate.delete(VREDIS_VERIFICATION_RISK_WHITELIST_IP_KEY_PREFIX + ip);
        }
    }

    private boolean isBypassedIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return true;
        }
        String clean = ip.trim();
        return "127.0.0.1".equals(clean) || "0:0:0:0:0:0:0:1".equals(clean) || "localhost".equalsIgnoreCase(clean);
    }

}
