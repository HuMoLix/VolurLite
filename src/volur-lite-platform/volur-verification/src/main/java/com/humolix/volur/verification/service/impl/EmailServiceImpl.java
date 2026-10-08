package com.humolix.volur.verification.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.bo.SendEmailBO;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.executor.MailTaskExecutor;
import com.humolix.volur.verification.mail.service.MailTransportService;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import com.humolix.volur.verification.service.EmailService;
import com.humolix.volur.verification.service.EmailVerificationCodeService;
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
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String VREDIS_EMAIL_SEND_LIMIT_KEY_PREFIX = "vredis:verification:email_send_limit:";
    private static final String VREDIS_EMAIL_DAILY_CAP_KEY_PREFIX = "vredis:verification:email_daily_cap:";
    private static final String VREDIS_EMAIL_IP_LIMIT_KEY_PREFIX = "vredis:verification:email_ip_limit:";
    private static final String VREDIS_PASSWORD_RESET_KEY_PREFIX = "vredis:verification:password_reset:";

    private static final int SEND_LIMIT_PERIOD_SECONDS = 60;
    private static final int VERIFY_CODE_EXPIRATION_SECONDS = 300;
    private static final int PASSWORD_RESET_EXPIRE_MINUTES = 15;
    private static final int MAX_EMAILS_PER_IP_HOUR = 10;
    private static final int MAX_EMAILS_PER_EMAIL_DAY = 5;
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:5173";

    private static final String TEMPLATE_NAME = "verification_email";
    private static final String RESET_TEMPLATE_NAME = "reset_password_email";
    private static final String CHANGE_PASSWORD_TEMPLATE_NAME = "change_password_email";
    private static final String DELETE_ACCOUNT_TEMPLATE_NAME = "delete_account_email";
    private static final String TEST_TEMPLATE_NAME = "smtp_test_email";
    private static final String RECOVER_USERNAME_TEMPLATE_NAME = "recover_username_email";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    private final SystemSettingService systemSettingService;
    private final CaptchaSDK captchaSDK;
    private final MailTransportService mailTransportService;
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final UserRepository userRepository;
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
            templateCache.put(DELETE_ACCOUNT_TEMPLATE_NAME, loadTemplate(DELETE_ACCOUNT_TEMPLATE_NAME));
            templateCache.put(TEST_TEMPLATE_NAME, loadTemplate(TEST_TEMPLATE_NAME));
            templateCache.put(RECOVER_USERNAME_TEMPLATE_NAME, loadTemplate(RECOVER_USERNAME_TEMPLATE_NAME));
            log.info("[Volur-Verification] 邮件模板预加载完成");
        } catch (Exception ex) {
            log.warn("[Volur-Verification] 邮件模板预加载警告: {}", ex.getMessage());
        }
    }

    @Override
    public void sendEmail(SendEmailBO bo) {
        String normalizedEmail = bo.getTo().toLowerCase().trim();

        // 1. 多阶梯防刷限频判定
        if (bo.isCheckRateLimit()) {
            checkMultiTierSendLimit(normalizedEmail);
        }

        // 2. 邮件内容解析：优先使用模板渲染，若无指定模板则使用传入内容
        String htmlContent;
        String fallbackContent;
        if (StringUtils.hasText(bo.getTemplateName())) {
            htmlContent = buildTemplate(bo.getTemplateName(), bo.getTemplateParams());
        } else {
            htmlContent = bo.getContent() != null ? bo.getContent() : "";
        }
        fallbackContent = htmlContent;

        // 3. 读取系统发件人配置
        String from = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS, "");
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("发件人邮箱地址 (system.mail.from_address) 未配置或为空，无法发送邮件");
        }
        from = from.trim();
        String senderName = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SENDER_NAME, "");

        String effectiveType = StringUtils.hasText(bo.getType()) ? bo.getType() : "SYSTEM";
        String operatorIp = StringUtils.hasText(bo.getOperatorIp()) ? bo.getOperatorIp() : RequestUtil.getRemoteIp();

        MailMessageBO message = MailMessageBO.builder()
                .from(from)
                .senderName(senderName)
                .to(normalizedEmail)
                .subject(bo.getSubject())
                .content(htmlContent)
                .fallbackContent(fallbackContent)
                .html(true)
                .type(effectiveType)
                .operatorId(bo.getOperatorId())
                .operatorIp(operatorIp)
                .build();

        // 4. 执行投递 (异步任务线程池 或 底层直接投递)
        if (bo.isAsync()) {
            mailTaskExecutor.executeAsyncMail(message);
        } else {
            mailTransportService.sendEmail(message.getTo(), message.getSubject(), message.getContent(), message.getType());
        }
    }

    @Override
    public void sendVerificationEmail(EmailCaptchaRequestDTO request) {
        if (request != null) {
            captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.REGISTER);
            sendVerificationEmail(request.getEmail(), request.getUsername(), false);
        }
    }

    @Override
    public void sendVerificationEmail(String email, String username) {
        sendVerificationEmail(email, username, false);
    }

    @Override
    public void sendVerificationEmail(String email, String username, boolean skipExistenceCheck) {
        String normalizedEmail = email.toLowerCase().trim();

        if (!skipExistenceCheck && userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该邮箱已被注册");
        }

        String code = emailVerificationCodeService.generateAndStoreCode(normalizedEmail);
        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";
        String safeCode = HtmlUtils.htmlEscape(code);

        sendEmail(SendEmailBO.builder()
                .to(normalizedEmail)
                .subject("[VolurCTF] 账户注册——邮箱验证")
                .templateName(TEMPLATE_NAME)
                .templateParams(Map.of(
                        "username", safeUsername,
                        "code", safeCode,
                        "expirationMinutes", String.valueOf(VERIFY_CODE_EXPIRATION_SECONDS / 60)
                ))
                .type("REGISTER_CAPTCHA")
                .build());
    }

    @Override
    public void sendChangePasswordEmail(EmailCaptchaRequestDTO request) {
        if (request != null) {
            captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.CHANGE_PASSWORD);
            sendChangePasswordEmail(request.getEmail(), request.getUsername());
        }
    }

    @Override
    public void sendChangePasswordEmail(String email, String username) {
        String normalizedEmail = email.toLowerCase().trim();
        String code = emailVerificationCodeService.generateAndStoreCode(normalizedEmail);
        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";
        String safeCode = HtmlUtils.htmlEscape(code);

        sendEmail(SendEmailBO.builder()
                .to(normalizedEmail)
                .subject("[Volur Lite] 修改密码——安全验证")
                .templateName(CHANGE_PASSWORD_TEMPLATE_NAME)
                .templateParams(Map.of(
                        "username", safeUsername,
                        "code", safeCode,
                        "expirationMinutes", String.valueOf(VERIFY_CODE_EXPIRATION_SECONDS / 60)
                ))
                .type("CHANGE_PASSWORD")
                .build());
    }

    @Override
    public void sendDeleteAccountEmail(EmailCaptchaRequestDTO request) {
        if (request != null) {
            captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.CHANGE_PASSWORD);
            sendDeleteAccountEmail(request.getEmail(), request.getUsername());
        }
    }

    @Override
    public void sendDeleteAccountEmail(String email, String username) {
        String normalizedEmail = email.toLowerCase().trim();
        String code = emailVerificationCodeService.generateAndStoreCode(normalizedEmail);
        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";
        String safeCode = HtmlUtils.htmlEscape(code);

        sendEmail(SendEmailBO.builder()
                .to(normalizedEmail)
                .subject("[Volur Lite] 注销账号——安全验证")
                .templateName(DELETE_ACCOUNT_TEMPLATE_NAME)
                .templateParams(Map.of(
                        "username", safeUsername,
                        "code", safeCode,
                        "expirationMinutes", String.valueOf(VERIFY_CODE_EXPIRATION_SECONDS / 60)
                ))
                .type("DELETE_ACCOUNT")
                .build());
    }

    @Override
    public void sendResetPasswordEmail(String email, String username) {
        String normalizedEmail = email.toLowerCase().trim();

        String resetToken = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(VREDIS_PASSWORD_RESET_KEY_PREFIX + resetToken, normalizedEmail, Duration.ofMinutes(PASSWORD_RESET_EXPIRE_MINUTES));

        String frontendUrl = systemSettingService.getSettingValue("system.general.frontend_url", "");
        if (!StringUtils.hasText(frontendUrl)) {
            try {
                frontendUrl = RequestUtil.getDomainUrl();
            } catch (Exception ex) {
                frontendUrl = DEFAULT_FRONTEND_URL;
            }
        }
        if (frontendUrl.endsWith("/")) {
            frontendUrl = frontendUrl.substring(0, frontendUrl.length() - 1);
        }
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";

        sendEmail(SendEmailBO.builder()
                .to(normalizedEmail)
                .subject("[VolurCTF] 找回密码")
                .templateName(RESET_TEMPLATE_NAME)
                .templateParams(Map.of(
                        "username", safeUsername,
                        "resetLink", resetLink
                ))
                .type("RESET_PASSWORD")
                .build());
    }

    @Override
    public void sendRecoverUsernameEmail(String email, String username) {
        String normalizedEmail = email.toLowerCase().trim();
        String safeUsername = username != null ? HtmlUtils.htmlEscape(username) : "";

        String frontendUrl = systemSettingService.getSettingValue("system.general.frontend_url", "");
        if (!StringUtils.hasText(frontendUrl)) {
            try {
                frontendUrl = RequestUtil.getDomainUrl();
            } catch (Exception ex) {
                frontendUrl = DEFAULT_FRONTEND_URL;
            }
        }
        if (frontendUrl.endsWith("/")) {
            frontendUrl = frontendUrl.substring(0, frontendUrl.length() - 1);
        }
        String signinLink = frontendUrl + "/v1/signin/identifier";

        sendEmail(SendEmailBO.builder()
                .to(normalizedEmail)
                .subject("[Volur Lite] 找回用户名")
                .templateName(RECOVER_USERNAME_TEMPLATE_NAME)
                .templateParams(Map.of(
                        "username", safeUsername,
                        "signinLink", signinLink
                ))
                .type("RECOVER_USERNAME")
                .build());
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return emailVerificationCodeService.verifyCode(email, code);
    }

    @Override
    public void testSmtpConnection() {
        mailTransportService.testSmtpConnection();
    }

    private void checkMultiTierSendLimit(String email) {
        String ip = RequestUtil.getRemoteIp();

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

}
