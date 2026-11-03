package com.humolix.volur.verification.service;

public interface MailWebhookService {

    /**
     * 处理外部邮件服务商（Resend / SendGrid / 自定义平台）的 Webhook 回调通知
     *
     * @param payload 原始 JSON 字符串
     * @param providedSecret 客户端提供的鉴权密钥/Token
     */
    void processWebhook(String payload, String providedSecret);

}
