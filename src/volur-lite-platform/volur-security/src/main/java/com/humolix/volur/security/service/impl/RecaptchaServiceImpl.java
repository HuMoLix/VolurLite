package com.humolix.volur.security.service.impl;

import cn.hutool.core.util.IdUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.RecaptchaService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.service.CaptchaService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaServiceImpl implements RecaptchaService {

    private static final String VREDIS_SECURITY_RECAPTCHA_CHALLENGE_KEY_PREFIX = "vredis:security:recaptcha:challenge:";
    private static final String VREDIS_SECURITY_RECAPTCHA_TICKET_KEY_PREFIX = "vredis:security:recaptcha:ticket:";

    private static final Duration RECAPTCHA_CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final Duration RECAPTCHA_TICKET_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final CaptchaCryptoService captchaCryptoService;
    private final CaptchaService captchaService;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ChallengeContext implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String dsh;
        private String challengeType;
        private String captchaId;
        private Long createdAt;
    }

    @Override
    public RecaptchaChallengeVO getRecaptchaChallenge(String dsh, String tl) {
        transitionLockManager.validateTransLock(dsh, tl, "recaptcha");
        Session session = sessionManager.getSession(dsh);

        String clientIp = RequestUtil.getClientIp();
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        String fp = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getFingerprint() : null;
        if (!StringUtils.hasText(fp) && session.getMetadata() != null) {
            fp = (String) session.getMetadata().get("fp");
        }

        RiskLevel riskLevel = riskEvaluationEngine.evaluateComprehensiveRisk(clientIp, identifier, fp, CaptchaScene.LOGIN);

        String challengeType = "CLICK";
        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.MEDIUM || "HIGH".equalsIgnoreCase(session.getRiskLevel())) {
            challengeType = "PUZZLE";
        }

        String recaptchaToken = "rctok_" + IdUtil.fastSimpleUUID();
        String publicKey = captchaCryptoService.getPublicKeyPem();

        RecaptchaChallengeVO.RecaptchaChallengeVOBuilder voBuilder = RecaptchaChallengeVO.builder()
                .recaptchaToken(recaptchaToken)
                .challengeType(challengeType)
                .publicKey(publicKey);

        String innerCaptchaId = null;

        if ("PUZZLE".equalsIgnoreCase(challengeType)) {
            CaptchaResponseVO captchaVO = captchaService.generateChallenge("LOGIN", CaptchaType.SLIDE_PUZZLE.name(), null);
            innerCaptchaId = captchaVO.getCaptchaId();
            voBuilder.backgroundImage(captchaVO.getImageBase64())
                    .puzzleImage(captchaVO.getSliderBase64())
                    .puzzleY(captchaVO.getYPosition())
                    .promptText("请拖动滑块完成拼图");
        } else if ("IMAGE".equalsIgnoreCase(challengeType)) {
            CaptchaResponseVO captchaVO = captchaService.generateChallenge("LOGIN", CaptchaType.LINE.name(), null);
            innerCaptchaId = captchaVO.getCaptchaId();
            voBuilder.captchaImage(captchaVO.getImageBase64())
                    .promptText("请输入图片中的字符");
        }

        ChallengeContext context = ChallengeContext.builder()
                .dsh(dsh)
                .challengeType(challengeType)
                .captchaId(innerCaptchaId)
                .createdAt(System.currentTimeMillis())
                .build();

        String key = VREDIS_SECURITY_RECAPTCHA_CHALLENGE_KEY_PREFIX + recaptchaToken;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(context), RECAPTCHA_CHALLENGE_TTL);
        } catch (Exception e) {
            log.error("[Recaptcha] 序列化人机验证挑战上下文失败", e);
        }

        return voBuilder.build();
    }

    @Override
    public RecaptchaVerifyResponseVO verifyRecaptcha(String dsh, String tl, RecaptchaVerifyRequestDTO request) {
        transitionLockManager.validateTransLock(dsh, tl, "recaptcha");

        String recaptchaToken = request.getRecaptchaToken().trim();
        String key = VREDIS_SECURITY_RECAPTCHA_CHALLENGE_KEY_PREFIX + recaptchaToken;
        String contextJson = redisTemplate.opsForValue().get(key);

        if (!StringUtils.hasText(contextJson)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证挑战已失效，请重新验证");
        }

        ChallengeContext context;
        try {
            context = objectMapper.readValue(contextJson, ChallengeContext.class);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解析人机验证挑战数据失败");
        }
        if (context == null || !Objects.equals(context.getDsh(), dsh)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证会话不匹配");
        }

        String plainJson = captchaCryptoService.decryptPayload(request.getPayload());
        if (!StringUtils.hasText(plainJson)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解密人机验证载荷失败");
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(plainJson);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证载荷格式错误");
        }

        long ts = rootNode.has("ts") ? rootNode.get("ts").asLong() : 0L;
        long current = System.currentTimeMillis();
        if (ts <= 0 || Math.abs(current - ts) > 120_000L) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证凭据已过期或时间戳非法");
        }

        String challengeType = context.getChallengeType();
        if ("PUZZLE".equalsIgnoreCase(challengeType)) {
            String userInput = rootNode.has("x") ? rootNode.get("x").asString() : "";
            if (!StringUtils.hasText(userInput) && rootNode.has("userInput")) {
                userInput = rootNode.get("userInput").asString();
            }
            if (!StringUtils.hasText(userInput)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请先拖动滑块完成拼图");
            }

            CaptchaVerifyDTO verifyDTO = new CaptchaVerifyDTO();
            verifyDTO.setCaptchaId(context.getCaptchaId());
            verifyDTO.setUserInput(userInput);
            verifyDTO.setScene("LOGIN");
            captchaService.verifyAndIssueTicket(verifyDTO);
        } else if ("IMAGE".equalsIgnoreCase(challengeType)) {
            String userInput = rootNode.has("code") ? rootNode.get("code").asString() : "";
            if (!StringUtils.hasText(userInput) && rootNode.has("userInput")) {
                userInput = rootNode.get("userInput").asString();
            }
            if (!StringUtils.hasText(userInput)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请输入验证码");
            }

            CaptchaVerifyDTO verifyDTO = new CaptchaVerifyDTO();
            verifyDTO.setCaptchaId(context.getCaptchaId());
            verifyDTO.setUserInput(userInput);
            verifyDTO.setScene("LOGIN");
            captchaService.verifyAndIssueTicket(verifyDTO);
        } else if ("CLICK".equalsIgnoreCase(challengeType)) {
            double risk = evaluateClickRisk(rootNode);
            if (risk >= 0.45) {
                riskEvaluationEngine.recordFailure(RequestUtil.getClientIp(), CaptchaScene.LOGIN);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "行为模式与运行环境存在异常，人机验证失败");
            }
        }

        redisTemplate.delete(key);

        String recaptchaTicket = "rctk_" + IdUtil.fastSimpleUUID();
        String ticketKey = VREDIS_SECURITY_RECAPTCHA_TICKET_KEY_PREFIX + recaptchaTicket;
        redisTemplate.opsForValue().set(ticketKey, dsh, RECAPTCHA_TICKET_TTL);

        return RecaptchaVerifyResponseVO.builder()
                .passed(true)
                .recaptchaTicket(recaptchaTicket)
                .build();
    }

    private double evaluateClickRisk(JsonNode rootNode) {
        double risk = 0.0;

        JsonNode envNode = rootNode.has("env") ? rootNode.get("env") : null;
        JsonNode bioNode = rootNode.has("bio") ? rootNode.get("bio") : null;
        JsonNode ecoNode = rootNode.has("eco") ? rootNode.get("eco") : null;

        int pts = bioNode != null ? bioNode.path("pts").asInt(0) : 0;
        double accVar = bioNode != null ? bioNode.path("accVar").asDouble(0.0) : 0.0;

        if (rootNode.has("isTrusted")) {
            boolean isTrusted = rootNode.get("isTrusted").asBoolean(false);
            if (!isTrusted) {
                risk += 1.0;
            }
        } else {
            if (pts < 3 || accVar < 0.0000008) {
                risk += 0.85;
            }
        }

        boolean isWd = (envNode != null && envNode.path("wd").asBoolean(false))
                || rootNode.path("webdriver").asBoolean(false);
        boolean isCdp = (envNode != null && envNode.path("cdp").asBoolean(false))
                || rootNode.path("cdpDetected").asBoolean(false);
        boolean protoTampered = (envNode != null && envNode.path("protoTampered").asBoolean(false))
                || rootNode.path("protoTampered").asBoolean(false);
        if (isWd || isCdp) {
            risk += 0.85;
        } else if (protoTampered) {
            risk += 0.40;
        }

        boolean headless = (envNode != null && envNode.path("headless").asBoolean(false))
                || rootNode.path("headlessUa").asBoolean(false);
        boolean zeroOuter = (envNode != null && envNode.path("zeroOuter").asBoolean(false))
                || rootNode.path("zeroOuter").asBoolean(false);
        if (headless || zeroOuter) {
            risk += 0.70;
        }

        boolean webglSoftware = (envNode != null && envNode.path("webglSoftware").asBoolean(false))
                || rootNode.path("webglSoftware").asBoolean(false);
        if (webglSoftware) {
            risk += 0.45;
        }

        boolean coreHook = (envNode != null && envNode.path("coreHook").asBoolean(false))
                || rootNode.path("coreHook").asBoolean(false);
        if (coreHook && (isWd || isCdp)) {
            risk += 0.35;
        } else if (coreHook) {
            risk += 0.15;
        } else {
            int extHookCount = envNode != null ? envNode.path("extHookCount").asInt(0) : rootNode.path("extHookCount").asInt(0);
            if (extHookCount > 0) {
                risk += Math.min(0.10, extHookCount * 0.03);
            }
        }

        boolean screenInconsistent = (envNode != null && envNode.path("screenInconsistent").asBoolean(false))
                || rootNode.path("screenInconsistent").asBoolean(false);
        boolean hardwareSpoofed = (envNode != null && envNode.path("hardwareSpoofed").asBoolean(false))
                || rootNode.path("hardwareSpoofed").asBoolean(false);
        if (screenInconsistent || hardwareSpoofed) {
            risk += 0.45;
        }

        long dur = bioNode != null ? bioNode.path("dur").asLong(0L) : rootNode.path("clickDuration").asLong(0L);
        if (dur < 20L || dur > 4000L) {
            risk += 0.45;
        } else if (dur >= 45L && dur <= 450L) {
            risk -= 0.10;
        }

        boolean isCenterTarget = bioNode != null && bioNode.path("isCenterTarget").asBoolean(false);
        boolean inActionBounds = bioNode != null && bioNode.path("inActionBounds").asBoolean(false);
        if (isCenterTarget) {
            risk += 0.65;
        } else if (inActionBounds) {
            risk -= 0.08;
        }

        boolean isNoMovement = bioNode != null && bioNode.path("isNoMovement").asBoolean(false);
        boolean isStraightLine = bioNode != null && bioNode.path("isStraightLine").asBoolean(false);
        boolean isTeleport = bioNode != null && bioNode.path("isTeleport").asBoolean(false);
        if (isStraightLine) {
            risk += 0.75;
        } else if (isTeleport) {
            risk += 0.80;
        } else if (isNoMovement) {
            risk += 0.35;
        }

        if (accVar < 0.0000008 && pts > 6) {
            risk += 0.50;
        } else if (pts >= 15 && accVar > 0.001) {
            risk -= 0.35;
        } else if (pts >= 4 && accVar > 0.000005) {
            risk -= 0.20;
        }

        if (ecoNode != null) {
            int hist = ecoNode.path("hist").asInt(1);
            long usage = ecoNode.path("usage").asLong(-1L);
            if (hist <= 1 && usage == 0L) {
                risk += 0.10;
            }
            if (ecoNode.path("navTimingZero").asBoolean(false) && hist <= 1) {
                risk += 0.10;
            }
        }

        return Math.clamp(risk, 0.0, 1.0);
    }

    @Override
    public boolean validateAndBurnTicket(String dsh, String ticket) {
        if (!StringUtils.hasText(dsh) || !StringUtils.hasText(ticket)) {
            return false;
        }
        String ticketKey = VREDIS_SECURITY_RECAPTCHA_TICKET_KEY_PREFIX + ticket.trim();
        String boundDsh = redisTemplate.opsForValue().get(ticketKey);
        if (StringUtils.hasText(boundDsh) && boundDsh.equals(dsh.trim())) {
            redisTemplate.delete(ticketKey);
            return true;
        }
        return false;
    }

}
