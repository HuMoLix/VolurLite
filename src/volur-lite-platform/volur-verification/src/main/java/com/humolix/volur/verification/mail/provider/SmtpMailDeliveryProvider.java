package com.humolix.volur.verification.mail.provider;

import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SmtpMailDeliveryProvider implements MailDeliveryProvider {

    private final SystemSettingService systemSettingService;

    @Override
    public String getProviderName() {
        return "SMTP";
    }

    @Override
    public boolean isAvailable() {
        String host = systemSettingService.getSettingValue("SMTP_HOST");
        String port = systemSettingService.getSettingValue("SMTP_PORT");
        String username = systemSettingService.getSettingValue("SMTP_USERNAME");
        String password = systemSettingService.getSettingValue("SMTP_PASSWORD");
        return StringUtils.hasText(host) && StringUtils.hasText(port) && StringUtils.hasText(username) && StringUtils.hasText(password);
    }

    @Override
    public void send(MailMessageBO message) throws Exception {
        JavaMailSenderImpl mailSender = createMailSender();
        String from = message.getFrom();
        if (!StringUtils.hasText(from)) {
            from = systemSettingService.getSettingValue("SMTP_FROM", mailSender.getUsername());
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, message.isHtml(), "UTF-8");
            helper.setFrom(from);
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(message.getContent(), message.isHtml());

            mailSender.send(mimeMessage);
            log.info("[Volur-Verification] SMTP 邮件发送成功 -> to: {}, subject: {}", message.getTo(), message.getSubject());
        } catch (Exception ex) {
            log.warn("[Volur-Verification] SMTP 邮件 HTML 投递失败，尝试降级纯文本: {}", ex.getMessage());
            if (StringUtils.hasText(message.getFallbackContent())) {
                MimeMessage fallbackMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(fallbackMessage, false, "UTF-8");
                helper.setFrom(from);
                helper.setTo(message.getTo());
                helper.setSubject(message.getSubject());
                helper.setText(message.getFallbackContent(), false);
                mailSender.send(fallbackMessage);
                log.info("[Volur-Verification] SMTP 邮件纯文本降级发送成功 -> to: {}", message.getTo());
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void testConnection() throws Exception {
        JavaMailSenderImpl mailSender = createMailSender();
        mailSender.testConnection();
    }

    private JavaMailSenderImpl createMailSender() {
        String host = trim(systemSettingService.getSettingValue("SMTP_HOST"));
        String portValue = trim(systemSettingService.getSettingValue("SMTP_PORT"));
        String username = trim(systemSettingService.getSettingValue("SMTP_USERNAME"));
        String password = trim(systemSettingService.getSettingValue("SMTP_PASSWORD"));

        if (!StringUtils.hasText(host) || !StringUtils.hasText(portValue) || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("SMTP 服务配置不完整");
        }

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SMTP_PORT 配置格式错误: " + portValue);
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
        }

        props.put("mail.debug", "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }

    private String trim(String val) {
        return val != null ? val.trim() : null;
    }

}
