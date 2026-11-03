package com.humolix.volur.verification.mail.provider;

import com.humolix.volur.verification.mail.bo.MailMessageBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
public class LoggingFallbackMailDeliveryProvider implements MailDeliveryProvider {

    @Override
    public String getProviderName() {
        return "LoggingFallback";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void send(MailMessageBO message) {
        log.warn("[Volur-Verification] [Fallback] 未激活外部真实邮件服务，已在日志中记录邮件 -> to: {}, subject: {}, content: {}",
                message.getTo(), message.getSubject(), message.getFallbackContent() != null ? message.getFallbackContent() : message.getContent());
    }

    @Override
    public void testConnection() {
        log.info("[Volur-Verification] LoggingFallback 连接测试通过");
    }

}
