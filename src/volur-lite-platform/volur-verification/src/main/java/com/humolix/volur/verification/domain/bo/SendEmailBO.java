package com.humolix.volur.verification.domain.bo;

import com.humolix.volur.framework.security.CurrentUserSchema;
import com.humolix.volur.framework.security.util.SecurityContextUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * 跨模块通用业务发信命令对象 (BO - 业务内面)
 * <p>
 * 供系统内部各业务模块调用 {@code EmailService.sendEmail()} 时使用。
 * 封装了包括收件人、主题、模板名称、模板动态插值变量、限频控制及安全审计等完整业务上下文。
 * <p>
 * <b>安全审计铁律</b>：{@code operatorId} 与 {@code operatorIp} 严禁由外部显式传参或篡改，
 * 必须且只能在构造生成时由 {@link SecurityContextUtil#getCurrentUserSchema()} 一次性获取当前请求上下文注入。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SendEmailBO {

    /**
     * 目标收件人邮箱地址 (必填，仅校验 RFC 标准邮箱格式，业务白名单由表现层 DTO 负责)
     */
    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "收件人邮箱格式不合法")
    private String to;

    /**
     * 邮件主题 (必填)
     */
    @NotBlank(message = "邮件主题不能为空")
    private String subject;

    /**
     * 纯文本或自定义 HTML 邮件内容 (与 templateName 二选一，当 templateName 为空时生效)
     */
    private String content;

    /**
     * 预置 HTML 邮件模板标识 (如 register_captcha, change_password_captcha, smtp_test_email)
     */
    private String templateName;

    /**
     * 模板动态参数插值键值对 (如 code=123456, expirationMinutes=5)
     */
    private Map<String, String> templateParams;

    /**
     * 业务场景分类标识 (如 VERIFICATION, NOTIFICATION, SYSTEM, MANUAL 等)
     */
    @Builder.Default
    private String type = "SYSTEM";

    /**
     * 是否执行多阶梯滑动窗口防刷限流 (默认 true；内部管理员发信或极高优先级系统广播可置为 false)
     */
    @Builder.Default
    private boolean checkRateLimit = true;

    /**
     * 是否通过异步任务线程池执行投递 (默认 true；探活测试或同步等待发信结果时可置为 false)
     */
    @Builder.Default
    private boolean async = true;

    /**
     * 操作人唯一用户 ID (严禁外部显式赋值与篡改，强制由 SecurityContextUtil 自动注入)
     */
    @Setter(AccessLevel.NONE)
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    /**
     * 操作人客户端 IP 地址 (严禁外部显式赋值与篡改，强制由 SecurityContextUtil 自动注入)
     */
    @Setter(AccessLevel.NONE)
    @NotBlank(message = "操作人IP不能为空")
    private String operatorIp;

    /**
     * 定制 Lombok Builder，屏蔽外部显式设置 operatorId 与 operatorIp，
     * 强制在 build() 时单次调用 SecurityContextUtil.getCurrentUserSchema() 原子注入。
     */
    public static class SendEmailBOBuilder {

        // 私有化 operatorId 与 operatorIp 方法，彻底禁止外部调用方通过链式语法设置
        private SendEmailBOBuilder operatorId(Long operatorId) {
            return this;
        }

        private SendEmailBOBuilder operatorIp(String operatorIp) {
            return this;
        }

        /**
         * 构建 SendEmailBO 实例并自动绑定当前请求执行上下文快照
         */
        public SendEmailBO build() {
            CurrentUserSchema schema = SecurityContextUtil.getCurrentUserSchema();
            return new SendEmailBO(
                    this.to,
                    this.subject,
                    this.content,
                    this.templateName,
                    this.templateParams,
                    this.type$set ? this.type$value : "SYSTEM",
                    !this.checkRateLimit$set || this.checkRateLimit$value,
                    !this.async$set || this.async$value,
                    schema.getId(),
                    schema.getIp()
            );
        }
    }
}
