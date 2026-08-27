package com.humolix.volur.verification.mail.provider;

import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class AliyunDirectMailDeliveryProvider implements MailDeliveryProvider {

    private final SystemSettingService systemSettingService;

    @Override
    public String getProviderName() {
        return "AliyunDirectMail";
    }

    @Override
    public boolean isAvailable() {
        String accessKey = systemSettingService.getSettingValue("system.mail.aliyun_access_key");
        String secretKey = systemSettingService.getSettingValue("system.mail.aliyun_secret_key");
        String account = systemSettingService.getSettingValue("system.mail.aliyun_account");
        return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey) && StringUtils.hasText(account);
    }

    @Override
    public void send(MailMessageBO message) throws Exception {
        log.info("[Volur-Verification] 通过阿里云邮件推送通道发送邮件 -> to: {}, subject: {}", message.getTo(), message.getSubject());
        // 预留阿里云 SDK / RESTful API 签名投递逻辑
    }

    @Override
    public void testConnection() throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("阿里云邮件推送配置未就绪");
        }
    }

}
