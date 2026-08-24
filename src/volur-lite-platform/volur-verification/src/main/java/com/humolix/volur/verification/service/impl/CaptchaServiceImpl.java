package com.humolix.volur.verification.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.security.util.RequestHolder;
import com.humolix.volur.verification.captcha.manager.CaptchaProviderManager;
import com.humolix.volur.verification.captcha.provider.CaptchaProvider;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final String VREDIS_VERIFICATION_CAPTCHA_IMAGE_KEY_PREFIX = "vredis:verification:captcha:image:";
    private static final String VREDIS_VERIFICATION_CAPTCHA_TICKET_KEY_PREFIX = "vredis:verification:captcha:ticket:";
    private static final String VREDIS_VERIFICATION_CAPTCHA_RATE_LIMIT_KEY_PREFIX = "vredis:verification:captcha:rate_limit:";

    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$|^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");

    private final CaptchaProviderManager providerManager;
    private final RiskEvaluationEngine riskEngine;
    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaResponseVO generateCaptcha(String oldCaptchaId) {
        return generateChallenge(null, null, oldCaptchaId);
    }

    @Override
    public CaptchaResponseVO generateChallenge(String sceneStr, String typeStr, String oldCaptchaId) {
        String ip = RequestHolder.getRemoteIp();
        checkRateLimit(ip);

        CaptchaScene scene = CaptchaScene.fromString(sceneStr);
        CaptchaType requestedType = CaptchaType.fromString(typeStr);

        if (StringUtils.hasText(oldCaptchaId) && isValidCaptchaId(oldCaptchaId)) {
            redisTemplate.delete(VREDIS_VERIFICATION_CAPTCHA_IMAGE_KEY_PREFIX + scene.name() + ":" + oldCaptchaId);
        }

        RiskLevel riskLevel = riskEngine.evaluateRisk(ip, scene);
        CaptchaType effectiveType = riskEngine.resolveEscalatedType(requestedType, riskLevel);

        CaptchaProvider provider = providerManager.getProvider(effectiveType);
        CaptchaResponseVO response = provider.generate(scene, riskLevel);

        riskEngine.recordVelocity(ip);
        return response;
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String code) {
        return verifyCaptcha(captchaId, code, CaptchaScene.DEFAULT);
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String code, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(code)) {
            return false;
        }

        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String ip = RequestHolder.getRemoteIp();

        // 尝试从各 Provider 校验
        for (CaptchaType type : CaptchaType.values()) {
            CaptchaProvider provider = providerManager.getProvider(type);
            if (provider.verify(captchaId, code, effectiveScene)) {
                riskEngine.recordSuccess(ip, effectiveScene);
                return true;
            }
        }

        riskEngine.recordFailure(ip, effectiveScene);
        return false;
    }

    @Override
    public CaptchaTicketVO verifyAndIssueTicket(CaptchaVerifyDTO request) {
        CaptchaScene scene = CaptchaScene.fromString(request.getScene());
        boolean valid = verifyCaptcha(request.getCaptchaId(), request.getUserInput(), scene);

        if (!valid) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证未通过或已过期，请重新验证");
        }

        String ticket = IdUtil.fastSimpleUUID();
        String ticketKey = VREDIS_VERIFICATION_CAPTCHA_TICKET_KEY_PREFIX + scene.name() + ":" + ticket;
        redisTemplate.opsForValue().set(ticketKey, "VALID", Duration.ofMinutes(5));

        return CaptchaTicketVO.builder()
                .ticket(ticket)
                .scene(scene.name())
                .expireSeconds(300L)
                .build();
    }

    @Override
    public boolean verifyTicket(String ticket, CaptchaScene scene) {
        if (!StringUtils.hasText(ticket)) {
            return false;
        }
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String ticketKey = VREDIS_VERIFICATION_CAPTCHA_TICKET_KEY_PREFIX + effectiveScene.name() + ":" + ticket;
        String val = redisTemplate.opsForValue().get(ticketKey);
        if (StringUtils.hasText(val)) {
            redisTemplate.delete(ticketKey);
            return true;
        }
        return false;
    }

    private void checkRateLimit(String ip) {
        String key = VREDIS_VERIFICATION_CAPTCHA_RATE_LIMIT_KEY_PREFIX + ip;
        boolean allowed = rateLimiter.tryAcquire(key, MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
        if (!allowed) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "验证码获取频率过高，请一分钟后再试");
        }
    }

    private boolean isValidCaptchaId(String captchaId) {
        return captchaId != null && UUID_PATTERN.matcher(captchaId).matches();
    }

}
