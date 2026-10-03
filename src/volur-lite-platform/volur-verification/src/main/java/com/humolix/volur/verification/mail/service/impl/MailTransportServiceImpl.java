package com.humolix.volur.verification.mail.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.dispatcher.FailoverMailDispatcher;
import com.humolix.volur.verification.mail.service.MailTransportService;
import com.humolix.volur.verification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 底层邮件传输与投递网关服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailTransportServiceImpl implements MailTransportService {

    private final SystemSettingService systemSettingService;
    private final FailoverMailDispatcher mailDispatcher;
    private final EmailLogRepository emailLogRepository;

    @Override
    public void sendEmail(String to, String subject, String content) {
        String type = "[VolurCTF] SMTP 服务测试邮件".equals(subject) ? "TEST" : "SYSTEM";
        sendEmail(to, subject, content, type);
    }

    @Override
    public void sendEmail(String to, String subject, String content, String type) {
        String normalizedTo = to != null ? to.trim() : "";
        String from = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS, "");
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("发件人邮箱地址 (system.mail.from_address) 未配置或为空，无法发送邮件");
        }
        from = from.trim();
        String senderName = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SENDER_NAME, "");
        String ip = RequestUtil.getRemoteIp();

        MailMessageBO message = MailMessageBO.builder()
                .from(from)
                .senderName(senderName)
                .to(normalizedTo)
                .subject(subject)
                .content(content)
                .fallbackContent(content)
                .html(true)
                .type(type)
                .operatorIp(ip)
                .build();

        try {
            mailDispatcher.dispatch(message);
            emailLogRepository.save(EmailLog.builder()
                    .recipient(normalizedTo)
                    .subject(subject)
                    .content(content)
                    .type(type)
                    .status("SUCCESS")
                    .sender(from)
                    .operatorIp(ip)
                    .build());
        } catch (Exception e) {
            log.error("[Volur-Verification] 发送邮件至 {} 失败", normalizedTo, e);
            emailLogRepository.save(EmailLog.builder()
                    .recipient(normalizedTo)
                    .subject(subject)
                    .content(content)
                    .type(type)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .sender(from)
                    .operatorIp(ip)
                    .build());
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

}
