package com.humolix.volur.verification.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.repository.EmailLogRepository;
import com.humolix.volur.verification.service.MailWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailWebhookServiceImpl implements MailWebhookService {

    private final EmailLogRepository emailLogRepository;
    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processWebhook(String payload, String providedSecret) {
        boolean webhookEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_WEBHOOK_ENABLED, false);
        if (!webhookEnabled) {
            log.warn("[Volur-Verification] 收到邮件 Webhook 回调，但系统尚未开启 Webhook 回调监听 (system.mail.webhook_enabled=false)");
            return;
        }

        String configuredSecret = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_WEBHOOK_SECRET, "");
        if (StringUtils.hasText(configuredSecret)) {
            String trimmedSecret = configuredSecret.trim();
            if (!StringUtils.hasText(providedSecret) || !trimmedSecret.equals(providedSecret.trim())) {
                log.warn("[Volur-Verification] 邮件 Webhook 密钥鉴权失败，拒绝处理: providedSecret={}", providedSecret);
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Webhook 签名或密钥校验失败，无权访问");
            }
        }

        if (!StringUtils.hasText(payload)) {
            log.warn("[Volur-Verification] 收到空的 Webhook Payload");
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            List<JsonNode> eventNodes = new ArrayList<>();
            if (rootNode.isArray()) {
                rootNode.forEach(eventNodes::add);
            } else if (rootNode.isObject()) {
                eventNodes.add(rootNode);
            }

            for (JsonNode node : eventNodes) {
                handleSingleEvent(node);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("[Volur-Verification] 解析邮件 Webhook Payload 发生异常: payload={}", payload, ex);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解析 Webhook 载荷失败: " + ex.getMessage());
        }
    }

    private void handleSingleEvent(JsonNode node) {
        // 1. 提取事件类型 (兼容 Resend 'type' 与 SendGrid/通用 'event')
        String event = node.path("type").asText("");
        if (!StringUtils.hasText(event)) {
            event = node.path("event").asText("");
        }
        event = event.trim().toLowerCase();

        // 2. 提取邮件唯一标识 (兼容 Resend data.email_id / messageId / sg_message_id)
        String messageId = node.path("data").path("email_id").asText("");
        if (!StringUtils.hasText(messageId)) {
            messageId = node.path("messageId").asText("");
        }
        if (!StringUtils.hasText(messageId)) {
            messageId = node.path("message_id").asText("");
        }
        if (!StringUtils.hasText(messageId)) {
            messageId = node.path("sg_message_id").asText("");
        }

        // 3. 提取收件人邮箱
        String recipient = "";
        JsonNode toNode = node.path("data").path("to");
        if (toNode.isArray() && !toNode.isEmpty()) {
            recipient = toNode.get(0).asText("");
        } else if (toNode.isTextual()) {
            recipient = toNode.asText("");
        }
        if (!StringUtils.hasText(recipient)) {
            recipient = node.path("email").asText("");
        }
        if (!StringUtils.hasText(recipient)) {
            recipient = node.path("recipient").asText("");
        }
        recipient = recipient.trim();

        // 4. 提取退信或失败原因
        String reason = node.path("data").path("bounce").path("message").asText("");
        if (!StringUtils.hasText(reason)) {
            reason = node.path("reason").asText("");
        }
        if (!StringUtils.hasText(reason)) {
            reason = node.path("response").asText("");
        }

        log.info("[Volur-Verification] 收到邮件服务商 Webhook 事件: event={}, messageId={}, recipient={}", event, messageId, recipient);

        // 5. 查找匹配的 EmailLog
        EmailLog targetLog = null;
        if (StringUtils.hasText(messageId)) {
            targetLog = emailLogRepository.findByMessageId(messageId).orElse(null);
        }
        if (targetLog == null && StringUtils.hasText(recipient)) {
            List<EmailLog> candidates = emailLogRepository.findTop5ByRecipientAndStatusInOrderByCreatedAtDesc(
                    recipient,
                    List.of("SENDING", "SENT", "DELIVERED")
            );
            if (!candidates.isEmpty()) {
                targetLog = candidates.get(0);
            }
        }

        if (targetLog == null) {
            log.warn("[Volur-Verification] Webhook 事件未匹配到对应的系统邮件日志记录: messageId={}, recipient={}", messageId, recipient);
            return;
        }

        // 6. 推进状态机
        if (event.contains("delivered")) {
            targetLog.setStatus("DELIVERED");
            targetLog.setErrorMessage(null);
            log.info("[Volur-Verification] Webhook 状态机流转 -> [DELIVERED (真实送达)]: logId={}, to={}", targetLog.getId(), targetLog.getRecipient());
        } else if (event.contains("bounce")) {
            targetLog.setStatus("BOUNCED");
            targetLog.setErrorMessage(StringUtils.hasText(reason) ? reason : "收件方邮件服务商退信 (Mailbox Rejected / Bounced)");
            log.warn("[Volur-Verification] Webhook 状态机流转 -> [BOUNCED (退信)]: logId={}, to={}, reason={}", targetLog.getId(), targetLog.getRecipient(), reason);
        } else if (event.contains("complain")) {
            targetLog.setStatus("COMPLAINED");
            targetLog.setErrorMessage("收件人向邮件服务商标记举报为垃圾邮件 (Spam Complaint)");
            log.warn("[Volur-Verification] Webhook 状态机流转 -> [COMPLAINED (垃圾投诉)]: logId={}, to={}", targetLog.getId(), targetLog.getRecipient());
        } else if (event.contains("fail") || event.contains("drop")) {
            targetLog.setStatus("FAILED");
            targetLog.setErrorMessage(StringUtils.hasText(reason) ? reason : "邮件投递失败 (Delivery Dropped)");
            log.warn("[Volur-Verification] Webhook 状态机流转 -> [FAILED (投递失败)]: logId={}, to={}, reason={}", targetLog.getId(), targetLog.getRecipient(), reason);
        }

        emailLogRepository.save(targetLog);
    }

}
