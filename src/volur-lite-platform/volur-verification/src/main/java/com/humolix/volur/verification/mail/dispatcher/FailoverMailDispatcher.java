package com.humolix.volur.verification.mail.dispatcher;

import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.provider.MailDeliveryProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailoverMailDispatcher {

    private final List<MailDeliveryProvider> providers;

    public void dispatch(MailMessageBO message) {
        Exception lastException = null;

        for (MailDeliveryProvider provider : providers) {
            if (provider.isAvailable()) {
                try {
                    provider.send(message);
                    return;
                } catch (Exception ex) {
                    lastException = ex;
                    log.warn("[Volur-Verification] 发信渠道 [{}] 投递失败，尝试自动容灾降级: {}", provider.getProviderName(), ex.getMessage());
                }
            }
        }

        if (lastException != null) {
            log.error("[Volur-Verification] 所有可用邮件投递渠道均发送失败: to={}, subject={}", message.getTo(), message.getSubject(), lastException);
        }
    }

    public void testConnection() throws Exception {
        for (MailDeliveryProvider provider : providers) {
            if (provider.isAvailable() && !"LoggingFallback".equals(provider.getProviderName())) {
                provider.testConnection();
                return;
            }
        }
        throw new IllegalStateException("未配置任何可用的真实邮件发信服务 (SMTP / Aliyun)");
    }

}
