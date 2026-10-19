package com.humolix.volur.verification.mail.service;

/**
 * 底层邮件传输与投递网关服务 (ISP 隔离接口)
 * <p>
 * 职责：纯粹的邮件参数解析、发信日志持久化及向底层 MailDispatcher 投递，无任何上层人机验证与验证码业务依赖。
 * </p>
 */
public interface MailTransportService {

    /**
     * 发送系统邮件 (HTML/文本)
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmail(String to, String subject, String content);

    /**
     * 发送带指定业务类型的邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param type    业务类型标识 (如 SYSTEM, LOGIN_OTP, TEST)
     */
    void sendEmail(String to, String subject, String content, String type);

    /**
     * 测试 SMTP 邮件服务连通性
     */
    void testSmtpConnection();

}
