package com.humolix.volur.verification.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
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
        return generateCaptcha(oldCaptchaId, null);
    }

    @Override
    public CaptchaResponseVO generateCaptcha(String oldCaptchaId, String ticket) {
        String ip = RequestUtil.getRemoteIp();
        checkRateLimit(ip);

        // 1. 若携带了滑动验证码 Ticket，核销 Ticket 并适度降低风险计数
        if (StringUtils.hasText(ticket)) {
            boolean ticketValid = verifyTicket(ticket, CaptchaScene.LOGIN, CaptchaType.SLIDE_PUZZLE);
            if (!ticketValid) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证凭证已失效或过期，请重新完成滑动验证");
            }
            // 降低风险计数（扣减失败计数惩罚，保留底数，避免一次性重置为0）
            riskEngine.reduceRisk(ip);
        } else {
            // 2. 未携带 Ticket 时评估当前风险
            RiskLevel currentRisk = riskEngine.evaluateRisk(ip, CaptchaScene.LOGIN);
            if (currentRisk == RiskLevel.HIGH) {
                // 触发高风控：通知前端需要进行行为式验证，严禁直接下发滑动大图伪装成普通图形验证码
                riskEngine.recordVelocity(ip);
                return CaptchaResponseVO.builder()
                        .requireBehaviorVerify(true)
                        .captchaType(CaptchaType.SLIDE_PUZZLE.name())
                        .build();
            }
        }

        // 3. 正常生成字符图形验证码 (LOW 为 LINE，MEDIUM 为 SHEAR 扭曲加噪)
        RiskLevel effectiveRisk = riskEngine.evaluateRisk(ip, CaptchaScene.LOGIN);
        CaptchaType effectiveType = (effectiveRisk == RiskLevel.MEDIUM) ? CaptchaType.SHEAR : CaptchaType.LINE;

        if (StringUtils.hasText(oldCaptchaId) && isValidCaptchaId(oldCaptchaId)) {
            redisTemplate.delete(VREDIS_VERIFICATION_CAPTCHA_IMAGE_KEY_PREFIX + CaptchaScene.LOGIN.name() + ":" + oldCaptchaId);
        }

        CaptchaProvider provider = providerManager.getProvider(effectiveType);
        CaptchaResponseVO response = provider.generate(CaptchaScene.LOGIN, effectiveRisk);
        response.setRequireBehaviorVerify(false);

        // 持续记录访问速率：若用户仍在此后频繁重复获取，速率累加将再次推高风控等级
        riskEngine.recordVelocity(ip);
        return response;
    }

    @Override
    public CaptchaResponseVO generateChallenge(String sceneStr, String typeStr, String oldCaptchaId) {
        String ip = RequestUtil.getRemoteIp();
        checkRateLimit(ip);

        CaptchaScene scene = CaptchaScene.fromString(sceneStr);
        CaptchaType requestedType = CaptchaType.fromString(typeStr);

        if (StringUtils.hasText(oldCaptchaId) && isValidCaptchaId(oldCaptchaId)) {
            redisTemplate.delete(VREDIS_VERIFICATION_CAPTCHA_IMAGE_KEY_PREFIX + scene.name() + ":" + oldCaptchaId);
        }

        RiskLevel riskLevel = riskEngine.evaluateRisk(ip, scene);
        CaptchaType effectiveType = riskEngine.resolveEscalatedType(requestedType, riskLevel, scene);

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
        return verifyAndGetMatchedType(captchaId, code, scene) != null;
    }

    private CaptchaType verifyAndGetMatchedType(String captchaId, String code, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(code)) {
            return null;
        }

        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String ip = RequestUtil.getRemoteIp();

        for (CaptchaType type : CaptchaType.values()) {
            CaptchaProvider provider = providerManager.getProvider(type);
            if (provider != null && provider.verify(captchaId, code, effectiveScene)) {
                riskEngine.recordSuccess(ip, effectiveScene);
                return type;
            }
        }

        riskEngine.recordFailure(ip, effectiveScene);
        return null;
    }

    @Override
    public CaptchaTicketVO verifyAndIssueTicket(CaptchaVerifyDTO request) {
        CaptchaScene scene = CaptchaScene.fromString(request.getScene());
        CaptchaType matchedType = verifyAndGetMatchedType(request.getCaptchaId(), request.getUserInput(), scene);

        if (matchedType == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证未通过或已过期，请重新验证");
        }

        // 防降级校验：通过的验证类型必须满足场景最低基准
        if (!matchedType.isAtLeast(scene.getMinRequiredType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该场景要求更高级别的人机验证，禁止降级");
        }

        String ticket = IdUtil.fastSimpleUUID();
        String ticketKey = VREDIS_VERIFICATION_CAPTCHA_TICKET_KEY_PREFIX + scene.name() + ":" + ticket;
        String ip = RequestUtil.getRemoteIp();

        // 写入结构化紧凑载荷: scene#verifiedType#ip#timestamp
        String payload = scene.name() + "#" + matchedType.name() + "#" + (ip != null ? ip : "") + "#" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(ticketKey, payload, Duration.ofMinutes(5));

        return CaptchaTicketVO.builder()
                .ticket(ticket)
                .scene(scene.name())
                .expireSeconds(300L)
                .build();
    }

    @Override
    public boolean verifyTicket(String ticket, CaptchaScene scene) {
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        return verifyTicket(ticket, effectiveScene, effectiveScene.getMinRequiredType());
    }

    @Override
    public boolean verifyTicket(String ticket, CaptchaScene scene, CaptchaType minRequiredType) {
        if (!StringUtils.hasText(ticket)) {
            return false;
        }
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String ticketKey = VREDIS_VERIFICATION_CAPTCHA_TICKET_KEY_PREFIX + effectiveScene.name() + ":" + ticket;

        // 原子获取并删除 (防止重放攻击，用完即销毁)
        String payload = redisTemplate.opsForValue().get(ticketKey);
        if (!StringUtils.hasText(payload)) {
            return false;
        }
        redisTemplate.delete(ticketKey);

        String[] parts = payload.split("#");
        if (parts.length < 2) {
            return true; // 兼容旧版格式
        }

        String payloadScene = parts[0];
        String payloadType = parts[1];

        if (!effectiveScene.name().equalsIgnoreCase(payloadScene)) {
            log.warn("[Volur-Verification] Ticket 场景不匹配: expected={}, actual={}", effectiveScene.name(), payloadScene);
            return false;
        }

        CaptchaType verifiedType = CaptchaType.fromString(payloadType);
        if (verifiedType == null) {
            return false;
        }

        CaptchaType requiredType = minRequiredType != null ? minRequiredType : effectiveScene.getMinRequiredType();
        if (!verifiedType.isAtLeast(requiredType)) {
            log.warn("[Volur-Verification] Ticket 验证码强度不足: required={}, actual={}", requiredType.name(), verifiedType.name());
            return false;
        }

        return true;
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
