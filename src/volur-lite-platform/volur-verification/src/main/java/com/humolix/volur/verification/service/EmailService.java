package com.humolix.volur.verification.service;

import com.humolix.volur.verification.domain.bo.SendEmailBO;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * 平台邮件服务核心门面接口
 */
@Validated
public interface EmailService {

    /**
     * 统一核心发信接口：根据 SendEmailBO 业务对象执行全流程管道发信 (支持模板渲染、多级限频、异步/同步投递与审计)
     *
     * @param bo 业务发信命令对象
     */
    void sendEmail(@Valid @NotNull(message = "发信请求对象不能为空") SendEmailBO bo);

    /**
     * 便捷重载 1：发送简单纯文本或自定义 HTML 内容邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    default void sendEmail(String to, String subject, String content) {
        sendEmail(SendEmailBO.builder()
                .to(to)
                .subject(subject)
                .content(content)
                .build());
    }

    /**
     * 便捷重载 2：基于系统预置 HTML 模板与动态键值参数渲染发信
     *
     * @param to             收件人邮箱
     * @param subject        邮件主题
     * @param templateName   模板标识
     * @param templateParams 动态变量映射表
     */
    default void sendEmail(String to, String subject, String templateName, Map<String, String> templateParams) {
        sendEmail(SendEmailBO.builder()
                .to(to)
                .subject(subject)
                .templateName(templateName)
                .templateParams(templateParams)
                .build());
    }

    /**
     * 注册邮箱验证码发信 (带人机凭证核验)
     */
    void sendVerificationEmail(EmailCaptchaRequestDTO request);

    /**
     * 注册邮箱验证码发信 (内部轻量重载)
     */
    void sendVerificationEmail(String email, String username);

    /**
     * 注册邮箱验证码发信 (支持跳过查重)
     */
    void sendVerificationEmail(String email, String username, boolean skipExistenceCheck);

    /**
     * 修改密码安全验证码发信 (带人机凭证核验)
     */
    void sendChangePasswordEmail(EmailCaptchaRequestDTO request);

    /**
     * 修改密码安全验证码发信 (内部轻量重载)
     */
    void sendChangePasswordEmail(String email, String username);

    /**
     * 注销账号安全验证码发信 (带人机凭证核验)
     */
    void sendDeleteAccountEmail(EmailCaptchaRequestDTO request);

    /**
     * 注销账号安全验证码发信 (内部轻量重载)
     */
    void sendDeleteAccountEmail(String email, String username);

    void sendResetPasswordEmail(String email, String username);

    void sendRecoverUsernameEmail(String email, String username);

    /**
     * 校验邮箱动态验证码 (原子核销)
     */
    boolean verifyCode(String email, String code);

    /**
     * 测试 SMTP 连通性
     */
    void testSmtpConnection();

    /**
     * 预加载系统邮件模板
     */
    void preloadTemplate();

}
