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

    public void dispatch(MailMessageBO message) throws Exception {
        Exception lastException = null;

        // 1. 优先遍历外部真实发信渠道 (如 SMTP, 阿里云邮件推送)
        for (MailDeliveryProvider provider : providers) {
            if ("LoggingFallback".equals(provider.getProviderName())) {
                continue; // 排除本地日志通道，避免假成功
            }

            if (provider.isAvailable()) {
                try {
                    provider.send(message);
                    return; // 真实发信成功，立即退出
                } catch (Exception ex) {
                    lastException = ex;
                    log.warn("[Volur-Verification] 外部发信渠道 [{}] 投递失败，尝试自动容灾降级: {}", provider.getProviderName(), ex.getMessage());
                }
            }
        }

        // 2. 若所有外部真实渠道均失败或未就绪，调用本地日志兜底打印邮件内容供排查
        for (MailDeliveryProvider provider : providers) {
            if ("LoggingFallback".equals(provider.getProviderName()) && provider.isAvailable()) {
                try {
                    provider.send(message);
                } catch (Exception ignored) {
                }
                break;
            }
        }

        // 3. 严格阻断：未真实送达时绝不静默吞没异常，必须明确抛出
        if (lastException != null) {
            log.error("[Volur-Verification] 所有可用真实邮件投递渠道均发送失败: to={}, subject={}", message.getTo(), message.getSubject(), lastException);
            throw lastException;
        } else {
            log.error("[Volur-Verification] 未配置任何可用的外部邮件服务 (SMTP / Aliyun): to={}", message.getTo());
            throw new IllegalStateException("未配置任何可用的外部邮件服务 (SMTP / Aliyun)，邮件未能实际发送");
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
