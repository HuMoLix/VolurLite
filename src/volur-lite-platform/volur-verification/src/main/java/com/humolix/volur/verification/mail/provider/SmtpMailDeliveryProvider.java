package com.humolix.volur.verification.mail.provider;

import com.humolix.volur.system.constant.SystemSettingKeys;
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
        String host = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_HOST);
        String port = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_PORT);
        String username = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_USER);
        String password = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_PASS);
        String fromAddress = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS);
        return StringUtils.hasText(host) && StringUtils.hasText(port) && StringUtils.hasText(username) && StringUtils.hasText(password) && StringUtils.hasText(fromAddress);
    }

    @Override
    public void send(MailMessageBO message) throws Exception {
        JavaMailSenderImpl mailSender = createMailSender();
        String recipient = message.getTo() != null ? message.getTo().trim() : "";
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("收件人邮箱地址不能为空");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, message.isHtml(), "UTF-8");
            applySender(helper, mailSender, message);
            helper.setTo(recipient);
            helper.setSubject(message.getSubject());
            helper.setText(message.getContent(), message.isHtml());
            if (StringUtils.hasText(message.getMessageId())) {
                mimeMessage.setHeader("Message-ID", "<" + message.getMessageId() + ">");
                mimeMessage.setHeader("X-Entity-Ref-ID", message.getMessageId());
                mimeMessage.setHeader("X-Volur-Message-Id", message.getMessageId());
            }

            mailSender.send(mimeMessage);
            log.info("[Volur-Verification] SMTP 邮件发送成功 -> to: {}, subject: {}", recipient, message.getSubject());
        } catch (Exception ex) {
            log.warn("[Volur-Verification] SMTP 邮件 HTML 投递失败，尝试降级纯文本: {}", ex.getMessage());
            if (StringUtils.hasText(message.getFallbackContent())) {
                MimeMessage fallbackMessage = mailSender.createMimeMessage();
                MimeMessageHelper fallbackHelper = new MimeMessageHelper(fallbackMessage, false, "UTF-8");
                applySender(fallbackHelper, mailSender, message);
                fallbackHelper.setTo(recipient);
                fallbackHelper.setSubject(message.getSubject());
                fallbackHelper.setText(message.getFallbackContent(), false);
                if (StringUtils.hasText(message.getMessageId())) {
                    fallbackMessage.setHeader("Message-ID", "<" + message.getMessageId() + ">");
                    fallbackMessage.setHeader("X-Entity-Ref-ID", message.getMessageId());
                    fallbackMessage.setHeader("X-Volur-Message-Id", message.getMessageId());
                }
                mailSender.send(fallbackMessage);
                log.info("[Volur-Verification] SMTP 邮件纯文本降级发送成功 -> to: {}", recipient);
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

    public void applySender(MimeMessageHelper helper, JavaMailSenderImpl mailSender, MailMessageBO message) throws Exception {
        String rawFrom = message != null ? message.getFrom() : null;
        String senderName = message != null ? message.getSenderName() : null;

        String fromAddress = null;

        if (StringUtils.hasText(rawFrom)) {
            rawFrom = rawFrom.trim();
            int startAngle = rawFrom.indexOf('<');
            int endAngle = rawFrom.lastIndexOf('>');
            if (startAngle >= 0 && endAngle > startAngle) {
                String extractedEmail = rawFrom.substring(startAngle + 1, endAngle).trim();
                if (extractedEmail.contains("@") && !hasWhitespace(extractedEmail)) {
                    fromAddress = extractedEmail;
                    if (!StringUtils.hasText(senderName)) {
                        senderName = rawFrom.substring(0, startAngle).trim();
                    }
                }
            } else if (rawFrom.contains("@") && !hasWhitespace(rawFrom)) {
                fromAddress = rawFrom;
            } else {
                if (!StringUtils.hasText(senderName)) {
                    senderName = rawFrom;
                }
            }
        }

        // 不进行回退至 smtp_user，直接从 system.mail.from_address 获取
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS);
        }

        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("发件人邮箱地址 (system.mail.from_address) 不能为空");
        }
        fromAddress = fromAddress.trim();

        if (!fromAddress.contains("@") || hasWhitespace(fromAddress)) {
            throw new IllegalArgumentException("发件人邮箱地址格式不合法: " + fromAddress);
        }

        if (!StringUtils.hasText(senderName)) {
            senderName = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SENDER_NAME, "");
        }
        if (StringUtils.hasText(senderName)) {
            senderName = senderName.trim();
        }

        if (StringUtils.hasText(senderName)) {
            helper.setFrom(fromAddress, senderName);
        } else {
            helper.setFrom(fromAddress);
        }
    }

    private boolean hasWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private JavaMailSenderImpl createMailSender() {
        String host = trim(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_HOST));
        String portValue = trim(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_PORT));
        String username = trim(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_USER));
        String password = trim(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_PASS));
        String fromAddress = trim(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS));

        if (!StringUtils.hasText(host) || !StringUtils.hasText(portValue) || !StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("SMTP 服务配置不完整（主机、端口、发信账号、密码及发件人邮箱地址均必填）");
        }

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("system.mail.smtp_port 配置格式错误: " + portValue);
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        boolean ssl = systemSettingService.getBoolean("system.mail.smtp_ssl", port == 465);
        if (ssl || port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
        }

        props.put("mail.smtp.ssl.trust", host);
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
