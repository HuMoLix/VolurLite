package com.humolix.volur.security.service.impl;

import cn.hutool.core.util.IdUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.RecaptchaService;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
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

    private final TransitionLockManager transitionLockManager;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final CaptchaCryptoService captchaCryptoService;
    private final CaptchaService captchaService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String VREDIS_SECURITY_RECAPTCHA_CHALLENGE_KEY_PREFIX = "vredis:security:recaptcha:challenge:";
    private static final String VREDIS_SECURITY_RECAPTCHA_TICKET_KEY_PREFIX = "vredis:security:recaptcha:ticket:";
    private static final Duration RECAPTCHA_CHALLENGE_TTL = Duration.ofMinutes(3);
    private static final Duration RECAPTCHA_TICKET_TTL = Duration.ofMinutes(5);

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
        transitionLockManager.validateStepAndTl(dsh, tl, "recaptcha");
        AuthFlowSession session = transitionLockManager.getSession(dsh);

        String clientIp = RequestUtil.getClientIp();
        String identifier = session.getIdentifier();
        String fp = session.getDeviceFingerprint();

        RiskLevel riskLevel = riskEvaluationEngine.evaluateComprehensiveRisk(clientIp, identifier, fp, CaptchaScene.LOGIN);

        String challengeType = "CLICK";
        if (riskLevel == RiskLevel.HIGH) {
            challengeType = "PUZZLE";
        } else if (riskLevel == RiskLevel.MEDIUM) {
            challengeType = "PUZZLE";
        } else if ("HIGH".equalsIgnoreCase(session.getRiskLevel())) {
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
            CaptchaResponseVO captchaVO = captchaService.generateChallenge("LOGIN", "SLIDER", null);
            innerCaptchaId = captchaVO.getCaptchaId();
            voBuilder.backgroundImage(captchaVO.getImageBase64())
                    .puzzleImage(captchaVO.getSliderBase64())
                    .puzzleY(captchaVO.getYPosition())
                    .promptText("请拖动滑块完成拼图");
        } else if ("IMAGE".equalsIgnoreCase(challengeType)) {
            CaptchaResponseVO captchaVO = captchaService.generateChallenge("LOGIN", "IMAGE", null);
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
        transitionLockManager.validateStepAndTl(dsh, tl, "recaptcha");

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
        if (ts > 0 && Math.abs(current - ts) > 120_000L) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证凭据已过期，请重试");
        }

        String challengeType = context.getChallengeType();
        if ("PUZZLE".equalsIgnoreCase(challengeType)) {
            String userInput = rootNode.has("x") ? rootNode.get("x").asText() : "";
            if (!StringUtils.hasText(userInput) && rootNode.has("userInput")) {
                userInput = rootNode.get("userInput").asText();
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
            String userInput = rootNode.has("code") ? rootNode.get("code").asText() : "";
            if (!StringUtils.hasText(userInput) && rootNode.has("userInput")) {
                userInput = rootNode.get("userInput").asText();
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
            if (rootNode.has("webdriver") && rootNode.get("webdriver").asBoolean(false)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "检测到自动化程序驱动，验证失败");
            }
            if (rootNode.has("isTrusted") && !rootNode.get("isTrusted").asBoolean(true)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "人机交互真实性校验未通过");
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
