package com.humolix.volur.verification.sdk;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.domain.dto.CaptchaGenerateRequestDTO;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
import com.humolix.volur.verification.domain.vo.CaptchaChallengeDecisionVO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.mail.service.MailTransportService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.risk.policy.CaptchaRoutePolicyManager;
import com.humolix.volur.verification.service.CaptchaService;
import com.humolix.volur.verification.service.EmailVerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 统一验证码与风控多模态核验 SDK 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaSDKImpl implements CaptchaSDK {

    private static final String VREDIS_EMAIL_SEND_LIMIT_KEY_PREFIX = "vredis:verification:email_send_limit:";

    private final SystemSettingService systemSettingService;
    private final RiskEvaluationEngine riskEngine;
    private final CaptchaService captchaService;
    private final MailTransportService mailTransportService;
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaCryptoService captchaCryptoService;
    private final CaptchaRoutePolicyManager captchaRoutePolicyManager;

    @Override
    public CaptchaResponseVO generateCaptchaWithEncryptedScene(CaptchaGenerateRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getCaptchaSceneId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "场景验证凭据不能为空");
        }

        // 1. 解密获得原始路由路径并校验防重放时间戳
        String path = captchaCryptoService.decryptScenePath(request.getCaptchaSceneId());
        String account = request.getAccount();

        // 2. 根据路径匹配基准场景与验证码类型策略
        CaptchaRoutePolicyManager.RoutePolicy policy = captchaRoutePolicyManager.resolvePolicy(path);
        CaptchaScene scene = policy.getScene();

        String ip = RequestUtil.getRemoteIp();
        String deviceToken = RequestUtil.getDeviceFingerprint();
        RiskLevel risk = riskEngine.evaluateComprehensiveRisk(ip, account, deviceToken, scene);
        boolean captchaEnabled = systemSettingService.getBoolean("system.captcha.enabled", false);
        boolean isCritical = isAccountCriticalUnderAttack(account);

        String captchaType;

        // 3. 结合风控引擎进行自适应决策
        if (isCritical) {
            captchaType = "EMAIL";
        } else if (risk == RiskLevel.HIGH || policy.getBaseType() == CaptchaType.SLIDE_PUZZLE) {
            captchaType = "SLIDER";
        } else if (!captchaEnabled && risk == RiskLevel.LOW) {
            captchaType = "NONE";
        } else {
            captchaType = "IMAGE";
        }

        // 4. 若为图形验证码，生成挑战并下发图片 Base64
        if ("IMAGE".equals(captchaType)) {
            CaptchaResponseVO capVo = captchaService.generateChallenge(
                    scene != null ? scene.name() : CaptchaScene.DEFAULT.name(),
                    risk == RiskLevel.MEDIUM ? CaptchaType.SHEAR.name() : CaptchaType.LINE.name(),
                    null
            );
            return CaptchaResponseVO.builder()
                    .captchaId(capVo.getCaptchaId())
                    .captchaType("IMAGE")
                    .imageBase64(capVo.getImageBase64())
                    .build();
        }

        // 5. 其余类型（SLIDER, EMAIL, NONE 等）精简下发，杜绝泄露图片及内部风控字段
        return CaptchaResponseVO.builder()
                .captchaType(captchaType)
                .build();
    }

    @Override
    public void verify(CaptchaVerifyRequestDTO request, CaptchaScene scene) {
        verify(request, null, scene);
    }

    @Override
    public void verify(CaptchaVerifyRequestDTO request, String account, CaptchaScene scene) {
        boolean captchaEnabled = systemSettingService.getBoolean("system.captcha.enabled", false);
        String ip = RequestUtil.getRemoteIp();

        // 1. 评估综合风控等级 (IP 维度、账号维度与设备指纹三轨研判)
        String deviceToken = request != null && StringUtils.hasText(request.getDeviceFingerprint())
                ? request.getDeviceFingerprint()
                : RequestUtil.getDeviceFingerprint();
        RiskLevel currentRisk = riskEngine.evaluateComprehensiveRisk(ip, account, deviceToken, scene);

        // 若全局未开启验证码且当前环境无异常风险，直接免验通行
        if (!captchaEnabled && currentRisk == RiskLevel.LOW) {
            return;
        }

        if (request == null) {
            recordFailure(account, scene);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请先完成人机安全验证");
        }

        // 2. 极高风控状态：强制带外邮件 OTP 核验
        if (isAccountCriticalUnderAttack(account)) {
            verifyOutOfBandOtp(request.getOtpCode(), account);
            return;
        }

        // 3. 高风控状态或强制滑块场景：必须提交滑块通行凭证 (captchaTicket)
        boolean requireSlider = (currentRisk == RiskLevel.HIGH)
                || (scene != null && scene.getMinRequiredType() == CaptchaType.SLIDE_PUZZLE);

        if (requireSlider) {
            if (!StringUtils.hasText(request.getCaptchaTicket())) {
                recordFailure(account, scene);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "当前操作环境存在安全风险，请先完成滑块人机验证");
            }
            boolean ticketValid = captchaService.verifyTicket(request.getCaptchaTicket(), scene, CaptchaType.SLIDE_PUZZLE);
            if (!ticketValid) {
                recordFailure(account, scene);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证凭证已失效或过期，请重新完成滑动验证");
            }
            return;
        }

        // 4. 常态与中度风控：优先核销滑块 Ticket，若无则核验字符图形验证码
        if (StringUtils.hasText(request.getCaptchaTicket())) {
            boolean ticketValid = captchaService.verifyTicket(request.getCaptchaTicket(), scene);
            if (!ticketValid) {
                recordFailure(account, scene);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证凭证已失效，请重新验证");
            }
        } else if (StringUtils.hasText(request.getCaptchaId()) && StringUtils.hasText(request.getCaptchaCode())) {
            boolean codeValid = captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode(), scene);
            if (!codeValid) {
                recordFailure(account, scene);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "图形验证码错误或已过期");
            }
        } else {
            recordFailure(account, scene);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请先输入验证码完成安全验证");
        }
    }

    @Override
    public CaptchaChallengeDecisionVO evaluateChallenge(String account, CaptchaScene scene) {
        String ip = RequestUtil.getRemoteIp();
        String deviceToken = RequestUtil.getDeviceFingerprint();
        RiskLevel risk = riskEngine.evaluateComprehensiveRisk(ip, account, deviceToken, scene);
        boolean captchaEnabled = systemSettingService.getBoolean("system.captcha.enabled", false);
        boolean isCritical = isAccountCriticalUnderAttack(account);

        String mode;
        boolean requireBehavior = false;
        boolean requireOtp = false;

        if (isCritical) {
            mode = "EMAIL_OTP";
            requireOtp = true;
        } else if (risk == RiskLevel.HIGH || (scene != null && scene.getMinRequiredType() == CaptchaType.SLIDE_PUZZLE)) {
            mode = "SLIDER";
            requireBehavior = true;
        } else if (!captchaEnabled && risk == RiskLevel.LOW) {
            mode = "NONE";
        } else {
            mode = "IMAGE";
        }

        CaptchaChallengeDecisionVO.CaptchaChallengeDecisionVOBuilder builder = CaptchaChallengeDecisionVO.builder()
                .riskLevel(risk.name())
                .mode(mode)
                .requireBehaviorVerify(requireBehavior)
                .requireOtp(requireOtp);

        if ("IMAGE".equals(mode)) {
            CaptchaResponseVO capVo = captchaService.generateChallenge(
                    scene != null ? scene.name() : CaptchaScene.DEFAULT.name(),
                    risk == RiskLevel.MEDIUM ? CaptchaType.SHEAR.name() : CaptchaType.LINE.name(),
                    null
            );
            builder.captchaId(capVo.getCaptchaId())
                    .imageBase64(capVo.getImageBase64())
                    .captchaType(capVo.getCaptchaType());
        }

        return builder.build();
    }

    @Override
    public void sendLoginOtp(String account, String captchaTicket) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "账号不能为空");
        }
        String targetEmail = resolveEmailByAccount(account);
        if (!StringUtils.hasText(targetEmail)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该账号不存在或未绑定有效邮箱");
        }

        // 门禁：高危场景下发送带外验证码必须核验滑块拼图凭证，防范恶意脚本无门槛轰炸目标邮箱
        if (!StringUtils.hasText(captchaTicket) || !captchaService.verifyTicket(captchaTicket, CaptchaScene.LOGIN, CaptchaType.SLIDE_PUZZLE)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "安全凭证无效，请先完成滑块拼图人机验证");
        }

        String normalizedEmail = targetEmail.toLowerCase().trim();
        String sendLimitKey = VREDIS_EMAIL_SEND_LIMIT_KEY_PREFIX + normalizedEmail;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(sendLimitKey))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "安全验证码发送过于频繁，请稍后再试");
        }

        String code = RandomUtil.randomNumbers(6);
        emailVerificationCodeService.storeCode(normalizedEmail, code, Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(sendLimitKey, "1", Duration.ofSeconds(60));

        String subject = "[VolurCTF] 登录安全动态验证码";
        String content = "<p>您正在尝试登录 Volur 平台，检测到环境安全风险，您的动态安全码为：<strong>" + code + "</strong>（5分钟内有效）。如非本人操作请警惕密码泄露风险。</p>";

        mailTransportService.sendEmail(normalizedEmail, subject, content, "LOGIN_OTP");
        log.info("[Volur-Verification] 为账号 [{}] 发送登录安全动态验证码", account);
    }

    @Override
    public void recordSuccess(String account, CaptchaScene scene) {
        String ip = RequestUtil.getRemoteIp();
        riskEngine.recordSuccess(ip, scene);
        String deviceToken = RequestUtil.getDeviceFingerprint();
        if (StringUtils.hasText(deviceToken)) {
            riskEngine.reduceDeviceRisk(deviceToken);
        }
        if (StringUtils.hasText(account)) {
            riskEngine.reduceAccountRisk(account);
        }
    }

    @Override
    public void recordFailure(String account, CaptchaScene scene) {
        String ip = RequestUtil.getRemoteIp();
        riskEngine.recordFailure(ip, scene);
        String deviceToken = RequestUtil.getDeviceFingerprint();
        if (StringUtils.hasText(deviceToken)) {
            riskEngine.recordDeviceFailure(deviceToken, scene);
        }
        if (StringUtils.hasText(account)) {
            riskEngine.recordAccountFailure(account, scene);
        }
    }

    private void verifyOutOfBandOtp(String otpCode, String account) {
        if (!StringUtils.hasText(otpCode)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "检测到多次异常尝试，请输入发送至绑定邮箱的安全动态码");
        }
        String targetEmail = resolveEmailByAccount(account);
        if (!StringUtils.hasText(targetEmail)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账户安全状态异常且未绑定邮箱，请联系系统管理员");
        }
        boolean matched = emailVerificationCodeService.verifyCode(targetEmail, otpCode);
        if (!matched) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱动态安全码错误或已过期");
        }
    }

    private boolean isAccountCriticalUnderAttack(String account) {
        if (!StringUtils.hasText(account)) {
            return false;
        }
        boolean emailEnabled = systemSettingService.getBoolean("system.mail.enabled", false);
        RiskLevel accountRisk = riskEngine.evaluateAccountRisk(account, CaptchaScene.LOGIN);
        return emailEnabled && accountRisk == RiskLevel.HIGH;
    }

    private String resolveEmailByAccount(String account) {
        String clean = account.toLowerCase().trim();
        if (clean.contains("@")) {
            return clean;
        }
        return userRepository.findByUsername(clean).map(User::getEmail).orElse(null);
    }

}
