package com.humolix.volur.verification.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.security.util.RequestHolder;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.dispatcher.FailoverMailDispatcher;
import com.humolix.volur.verification.mail.executor.MailTaskExecutor;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.CaptchaService;
import com.humolix.volur.verification.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String VREDIS_EMAIL_CODE_KEY_PREFIX = "vredis:verification:email_code:";
    private static final String VREDIS_EMAIL_SEND_LIMIT_KEY_PREFIX = "vredis:verification:email_send_limit:";
    private static final String VREDIS_EMAIL_DAILY_CAP_KEY_PREFIX = "vredis:verification:email_daily_cap:";
    private static final String VREDIS_EMAIL_IP_LIMIT_KEY_PREFIX = "vredis:verification:email_ip_limit:";
    private static final String VREDIS_PASSWORD_RESET_KEY_PREFIX = "vredis:verification:password_reset:";
    private static final String VREDIS_CHANGE_PASSWORD_KEY_PREFIX = "vredis:verification:change_password:";

    private static final int SEND_LIMIT_PERIOD_SECONDS = 60;
    private static final int VERIFY_CODE_EXPIRATION_SECONDS = 300;
    private static final int PASSWORD_RESET_EXPIRE_MINUTES = 15;
    private static final int MAX_EMAILS_PER_IP_HOUR = 10;
    private static final int MAX_EMAILS_PER_EMAIL_DAY = 5;
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:5173";

    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "tempmail.com", "mailinator.com", "guerrillamail.com", "10minutemail.com",
            "throwawaymail.com", "sharklasers.com", "getairmail.com", "yopmail.com"
    );

    private static final String TEMPLATE_NAME = "verification_email";
    private static final String RESET_TEMPLATE_NAME = "reset_password_email";
    private static final String CHANGE_PASSWORD_TEMPLATE_NAME = "change_password_email";
    private static final String TEST_TEMPLATE_NAME = "smtp_test_email";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    private final SystemSettingService systemSettingService;
    private final CaptchaService captchaService;
    private final UserRepository userRepository;
    private final FailoverMailDispatcher mailDispatcher;
    private final MailTaskExecutor mailTaskExecutor;
    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    @Override
    public void preloadTemplate() {
        try {
            templateCache.put(TEMPLATE_NAME, loadTemplate(TEMPLATE_NAME));
            templateCache.put(RESET_TEMPLATE_NAME, loadTemplate(RESET_TEMPLATE_NAME));
            templateCache.put(CHANGE_PASSWORD_TEMPLATE_NAME, loadTemplate(CHANGE_PASSWORD_TEMPLATE_NAME));
            templateCache.put(TEST_TEMPLATE_NAME, loadTemplate(TEST_TEMPLATE_NAME));
            log.info("[Volur-Verification] 邮件模板预加载完成");
        } catch (Exception ex) {
            log.warn("[Volur-Verification] 邮件模板预加载警告: {}", ex.getMessage());
        }
    }

    @Override
    public void sendVerificationEmail(EmailCaptchaRequestDTO request) {
        checkCaptchaIfEnabled(request, CaptchaScene.REGISTER);
        sendVerificationEmail(request.getEmail(), request.getUsername(), false);
    }

    @Override
    public void sendVerificationEmail(String email, String username) {
        sendVerificationEmail(email, username, false);
    }

    @Override
    public void sendVerificationEmail(String email, String username, boolean skipExistenceCheck) {
        String normalizedEmail = email.toLowerCase().trim();
        validateEmailDomain(normalizedEmail);

        if (!skipExistenceCheck && userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该邮箱已被注册");
        }

        if (systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)) {
            String whitelistStr = systemSettingService.getSettingValue("EMAIL_WHITELIST_SUFFIXES", "");
            if (StringUtils.hasText(whitelistStr)) {
                String[] whitelist = whitelistStr.split(",");
                boolean isWhitelisted = Arrays.stream(whitelist)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .anyMatch(suffix -> normalizedEmail.endsWith(suffix.startsWith("@") ? suffix : "@" + suffix));
                if (!isWhitelisted) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持该邮箱后缀注册，仅限: " + whitelistStr);
                }
            }
        }

        sendVerificationCodeEmail(normalizedEmail, username, "[VolurCTF] 账户注册——邮箱验证", TEMPLATE_NAME);
    }

    @Override
    public void sendChangePasswordEmail(EmailCaptchaRequestDTO request) {
        checkCaptchaIfEnabled(request, CaptchaScene.CHANGE_PASSWORD);
        sendChangePasswordEmail(request.getEmail(), request.getUsername());
    }

    @Override
    public void sendChangePasswordEmail(String email, String username) {
        String normalizedEmail = email.toLowerCase().trim();
        validateEmailDomain(normalizedEmail);
        sendVerificationCodeEmail(normalizedEmail, username, "[VolurCTF] 修改密码——邮箱验证", CHANGE_PASSWORD_TEMPLATE_NAME);
    }

    private void checkCaptchaIfEnabled(EmailCaptchaRequestDTO request, CaptchaScene scene) {
        if (systemSettingService.getBoolean("ENABLE_CAPTCHA", false)) {
            if (StringUtils.hasText(request.getCaptchaTicket())) {
                if (!captchaService.verifyTicket(request.getCaptchaTicket(), scene)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "人机验证凭证已失效，请重新验证");
                }
            } else if (StringUtils.hasText(request.getCaptchaId()) && StringUtils.hasText(request.getCaptchaCode())) {
                if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode(), scene)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "图形验证码错误或已过期");
                }
            } else {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请先完成人机安全验证");
            }
        }
    }

    private void validateEmailDomain(String email) {
        int atIndex = email.lastIndexOf("@");
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱格式不合法");
        }
        String domain = email.substring(atIndex + 1).toLowerCase().trim();
        if (DISPOSABLE_EMAIL_DOMAINS.contains(domain)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "禁止使用临时一次性邮箱");
        }
    }

    private void sendVerificationCodeEmail(String email, String username, String subject, String templateName) {
        checkMultiTierSendLimit(email);

        String code = generateVerificationCode();
        String verifyCodeKey = VREDIS_EMAIL_CODE_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(verifyCodeKey, code, Duration.ofSeconds(VERIFY_CODE_EXPIRATION_SECONDS));
        log.info("[Volur-Verification] 生成邮件验证码 -> 邮箱: {}, 验证码: {}", email, code);

        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";
        String safeCode = HtmlUtils.htmlEscape(code);

        String htmlContent = buildTemplate(templateName, Map.of(
                "username", safeUsername,
                "code", safeCode,
                "expirationMinutes", String.valueOf(VERIFY_CODE_EXPIRATION_SECONDS / 60)
        ));
        String simpleContent = "您的验证码是: " + code + "\n有效期为 " + (VERIFY_CODE_EXPIRATION_SECONDS / 60) + " 分钟";

        MailMessageBO message = MailMessageBO.builder()
                .to(email)
                .subject(subject)
                .content(htmlContent)
                .fallbackContent(simpleContent)
                .html(true)
                .build();

        mailTaskExecutor.executeAsyncMail(message);
    }

    @Override
    public String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (!verifyCode(normalizedEmail, request.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        String token = IdUtil.fastSimpleUUID();
        String redisKey = VREDIS_CHANGE_PASSWORD_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, normalizedEmail, Duration.ofMinutes(PASSWORD_RESET_EXPIRE_MINUTES));
        return token;
    }

    @Override
    public void sendResetPasswordEmail(String email, String username) {
        String normalizedEmail = email.toLowerCase().trim();
        checkMultiTierSendLimit(normalizedEmail);

        String resetToken = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(VREDIS_PASSWORD_RESET_KEY_PREFIX + resetToken, normalizedEmail, Duration.ofMinutes(PASSWORD_RESET_EXPIRE_MINUTES));

        String frontendUrl = systemSettingService.getSettingValue("FRONTEND_URL", "");
        if (!StringUtils.hasText(frontendUrl)) {
            try {
                frontendUrl = RequestHolder.getDomainUrl();
            } catch (Exception ex) {
                frontendUrl = DEFAULT_FRONTEND_URL;
            }
        }
        if (frontendUrl.endsWith("/")) {
            frontendUrl = frontendUrl.substring(0, frontendUrl.length() - 1);
        }
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";
        String htmlContent = buildTemplate(RESET_TEMPLATE_NAME, Map.of(
                "username", safeUsername,
                "resetLink", resetLink
        ));
        String simpleContent = "请点击以下链接重置密码: " + resetLink + " (有效期15分钟)";

        MailMessageBO message = MailMessageBO.builder()
                .to(normalizedEmail)
                .subject("[VolurCTF] 找回密码")
                .content(htmlContent)
                .fallbackContent(simpleContent)
                .html(true)
                .build();

        mailTaskExecutor.executeAsyncMail(message);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return false;
        }
        String key = VREDIS_EMAIL_CODE_KEY_PREFIX + email.toLowerCase().trim();
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            return false;
        }
        boolean matches = storedCode.equalsIgnoreCase(code.trim());
        if (matches) {
            redisTemplate.delete(key);
        }
        return matches;
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            if ("[VolurCTF] SMTP 服务测试邮件".equals(subject)) {
                content = buildTemplate(TEST_TEMPLATE_NAME, Map.of());
            }
            MailMessageBO message = MailMessageBO.builder()
                    .to(to)
                    .subject(subject)
                    .content(content)
                    .fallbackContent(content)
                    .html(true)
                    .build();
            mailDispatcher.dispatch(message);
        } catch (Exception e) {
            log.error("[Volur-Verification] 发送邮件至 {} 失败", to, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "邮件发送失败: " + e.getMessage());
        }
    }

    @Override
    public void testSmtpConnection() {
        try {
            mailDispatcher.testConnection();
        } catch (Exception ex) {
            log.error("[Volur-Verification] 邮件服务连通性测试失败", ex);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮件服务测试失败: " + ex.getMessage(), ex);
        }
    }

    private void checkMultiTierSendLimit(String email) {
        String ip = RequestHolder.getRemoteIp();

        // 1. IP 级别限流 (每小时最多 10 封)
        String ipLimitKey = VREDIS_EMAIL_IP_LIMIT_KEY_PREFIX + ip;
        if (!rateLimiter.tryAcquire(ipLimitKey, MAX_EMAILS_PER_IP_HOUR, Duration.ofHours(1))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "IP 请求发信过于频繁，请一小时后再试");
        }

        // 2. 邮箱单日限额 (24 小时最多 5 封)
        String dailyCapKey = VREDIS_EMAIL_DAILY_CAP_KEY_PREFIX + email;
        if (!rateLimiter.tryAcquire(dailyCapKey, MAX_EMAILS_PER_EMAIL_DAY, Duration.ofDays(1))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "该邮箱今日接收验证码已达上限（5次/天）");
        }

        // 3. 邮箱单次 60 秒冷却
        String sendLimitKey = VREDIS_EMAIL_SEND_LIMIT_KEY_PREFIX + email;
        Long sendCount = redisTemplate.opsForValue().increment(sendLimitKey);
        Long expire = redisTemplate.getExpire(sendLimitKey);
        if (sendCount != null && (sendCount == 1 || expire == null || expire == -1)) {
            redisTemplate.expire(sendLimitKey, Duration.ofSeconds(SEND_LIMIT_PERIOD_SECONDS));
        }
        if (sendCount != null && sendCount > 1) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "发送过于频繁，请 60 秒后再试");
        }
    }

    private String buildTemplate(String templateName, Map<String, String> params) {
        try {
            String template = getTemplateContent(templateName);
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String value = entry.getValue() != null ? entry.getValue() : "";
                    template = template.replace("${{" + entry.getKey() + "}}", value);
                }
            }
            template = template.replace("${{year}}", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
            return template;
        } catch (Exception e) {
            log.error("[Volur-Verification] 构建邮件模板失败: {}", templateName, e);
            return "";
        }
    }

    private String getTemplateContent(String templateName) throws Exception {
        String template = templateCache.get(templateName);
        if (template == null || template.isEmpty()) {
            template = loadTemplate(templateName);
            templateCache.put(templateName, template);
        }
        return template;
    }

    private String loadTemplate(String name) throws Exception {
        String templatePath = "templates/email-templates/" + name + ".html";
        Resource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("邮件模板文件不存在: " + templatePath);
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException ex) {
            throw new IOException("读取邮件模板文件失败: " + templatePath, ex);
        }
    }

    private String generateVerificationCode() {
        return RandomUtil.randomNumbers(6);
    }

}
