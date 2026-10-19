package com.humolix.volur.verification;

import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.entity.RiskIpRule;
import com.humolix.volur.verification.domain.enums.IpRuleStatus;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import com.humolix.volur.verification.domain.vo.AdminIpDiagnosisVO;
import com.humolix.volur.verification.mapper.RiskIpRuleMapper;
import com.humolix.volur.verification.repository.RiskIpRuleRepository;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.admin.impl.AdminRiskServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class AdminRiskServiceTest {

    private RiskIpRuleRepository ipRuleRepository;
    private RiskIpRuleMapper ipRuleMapper;
    private SystemSettingService systemSettingService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisSlidingWindowRateLimiter rateLimiter;
    private AdminRiskServiceImpl adminRiskService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        ipRuleRepository = Mockito.mock(RiskIpRuleRepository.class);
        ipRuleMapper = Mockito.mock(RiskIpRuleMapper.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        rateLimiter = Mockito.mock(RedisSlidingWindowRateLimiter.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(systemSettingService.getBoolean(eq(SystemSettingKeys.RISK_BYPASS_LOOPBACK), anyBoolean())).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.RISK_IP_FILTER_ENABLED), anyBoolean())).thenReturn(true);
        when(systemSettingService.getInt(eq(SystemSettingKeys.RISK_DEFAULT_BAN_DURATION_MINUTES), anyInt())).thenReturn(1440);

        adminRiskService = new AdminRiskServiceImpl(
                ipRuleRepository,
                ipRuleMapper,
                systemSettingService,
                redisTemplate,
                rateLimiter
        );
    }

    @Test
    public void testIsIpBannedRedisHit() {
        String testIp = "1.2.3.4";
        when(redisTemplate.hasKey("vredis:verification:risk:whitelist:ip:" + testIp)).thenReturn(false);
        when(redisTemplate.hasKey("vredis:verification:risk:ban:ip:" + testIp)).thenReturn(true);

        boolean banned = adminRiskService.isIpBanned(testIp);
        Assertions.assertTrue(banned);
    }

    @Test
    public void testIsIpBannedWhitelistBypass() {
        String testIp = "1.2.3.4";
        when(redisTemplate.hasKey("vredis:verification:risk:whitelist:ip:" + testIp)).thenReturn(true);

        boolean banned = adminRiskService.isIpBanned(testIp);
        Assertions.assertFalse(banned);
    }

    @Test
    public void testIsIpBannedDbHitAndSync() {
        String testIp = "5.6.7.8";
        when(redisTemplate.hasKey("vredis:verification:risk:whitelist:ip:" + testIp)).thenReturn(false);
        when(redisTemplate.hasKey("vredis:verification:risk:ban:ip:" + testIp)).thenReturn(false);

        RiskIpRule rule = RiskIpRule.builder()
                .id(1001L)
                .ip(testIp)
                .ruleType(IpRuleType.BLACKLIST)
                .status(IpRuleStatus.ACTIVE)
                .expiresAt(Instant.now().plus(Duration.ofHours(2)))
                .build();

        when(ipRuleRepository.findFirstByIpAndRuleTypeAndStatusOrderByCreatedAtDesc(
                eq(testIp), eq(IpRuleType.BLACKLIST), eq(IpRuleStatus.ACTIVE)
        )).thenReturn(Optional.of(rule));

        boolean banned = adminRiskService.isIpBanned(testIp);
        Assertions.assertTrue(banned);
        Mockito.verify(valueOperations).set(eq("vredis:verification:risk:ban:ip:" + testIp), anyString(), any(Duration.class));
    }

    @Test
    public void testDiagnoseIp() {
        String testIp = "9.9.9.9";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(rateLimiter.getCurrentCount(contains("failures:"), any(Duration.class))).thenReturn(6L);
        when(rateLimiter.getCurrentCount(contains("velocity:"), any(Duration.class))).thenReturn(1L);

        AdminIpDiagnosisVO diagnosis = adminRiskService.diagnoseIp(testIp);
        Assertions.assertNotNull(diagnosis);
        Assertions.assertEquals(testIp, diagnosis.getIp());
        Assertions.assertEquals("MEDIUM", diagnosis.getRiskLevel());
        Assertions.assertEquals(6L, diagnosis.getRecentFailures());
    }

    @Test
    public void testGetSettingsWithCaptcha() {
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.RISK_ENABLED), anyBoolean())).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.CAPTCHA_ENABLED), anyBoolean())).thenReturn(true);
        when(systemSettingService.getInt(eq(SystemSettingKeys.CAPTCHA_TOLERANCE_PX), anyInt())).thenReturn(10);
        when(systemSettingService.getInt(eq(SystemSettingKeys.CAPTCHA_TICKET_TTL_SECONDS), anyInt())).thenReturn(600);

        com.humolix.volur.verification.domain.vo.AdminRiskSettingsVO settings = adminRiskService.getSettings();
        Assertions.assertNotNull(settings);
        Assertions.assertTrue(settings.getEnabled());
        Assertions.assertTrue(settings.getCaptchaEnabled());
        Assertions.assertEquals(10, settings.getCaptchaTolerance());
        Assertions.assertEquals(600, settings.getCaptchaTicketTtl());
    }

    @Test
    public void testUpdateSettingsWithCaptcha() {
        com.humolix.volur.verification.domain.dto.AdminRiskSettingsDTO dto = new com.humolix.volur.verification.domain.dto.AdminRiskSettingsDTO(
                true, true, 1440, "拦截提示", true, true, 8, 300, 5, true
        );
        adminRiskService.updateSettings(dto, 1L);
        Mockito.verify(systemSettingService).updateSetting(argThat(d -> SystemSettingKeys.RISK_ENABLED.equals(d.getKey()) && "true".equals(d.getValue())));
        Mockito.verify(systemSettingService).updateSetting(argThat(d -> SystemSettingKeys.CAPTCHA_ENABLED.equals(d.getKey()) && "true".equals(d.getValue())));
        Mockito.verify(systemSettingService).updateSetting(argThat(d -> SystemSettingKeys.CAPTCHA_TOLERANCE_PX.equals(d.getKey()) && "8".equals(d.getValue())));
    }

    @Test
    public void testGetPolicyWithEmailAndInterception() {
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES), anyString())).thenReturn("edu.cn, humolix.com");
        when(systemSettingService.getInt(eq(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW), anyInt())).thenReturn(120);
        when(systemSettingService.getInt(eq(SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS), anyInt())).thenReturn(300);
        when(systemSettingService.getInt(eq(SystemSettingKeys.RISK_FAILURES_MEDIUM_THRESHOLD), anyInt())).thenReturn(5);

        com.humolix.volur.verification.domain.vo.AdminRiskPolicyVO policy = adminRiskService.getPolicy();
        Assertions.assertNotNull(policy);
        Assertions.assertEquals(2, policy.getEmailWhitelistSuffixes().size());
        Assertions.assertTrue(policy.getEmailWhitelistSuffixes().contains("edu.cn"));
        Assertions.assertTrue(policy.getEmailWhitelistSuffixes().contains("humolix.com"));
        Assertions.assertEquals(120, policy.getRateLimitWindow());
        Assertions.assertEquals(300, policy.getRateLimitMaxRequests());
    }

    @Test
    public void testUpdatePolicyWithEmailAndInterception() {
        com.humolix.volur.verification.domain.dto.AdminRiskPolicyDTO dto = new com.humolix.volur.verification.domain.dto.AdminRiskPolicyDTO(
                java.util.List.of("edu.cn", "org.cn"), 60, 200, 15, 5, 10, 5, 10, 20, 6
        );
        adminRiskService.updatePolicy(dto, 1L);
        Mockito.verify(systemSettingService).updateSetting(argThat(d -> SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES.equals(d.getKey()) && "edu.cn,org.cn".equals(d.getValue())));
        Mockito.verify(systemSettingService).updateSetting(argThat(d -> SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW.equals(d.getKey()) && "60".equals(d.getValue())));
        Mockito.verify(systemSettingService).updateSetting(argThat(d -> SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS.equals(d.getKey()) && "200".equals(d.getValue())));
    }

}
