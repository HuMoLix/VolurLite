package com.humolix.volur.verification.mail.executor;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.dispatcher.FailoverMailDispatcher;
import com.humolix.volur.verification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailTaskExecutor {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final FailoverMailDispatcher mailDispatcher;
    private final EmailLogRepository emailLogRepository;
    private final VolurThreadManager volurThreadManager;
    private final SystemSettingService systemSettingService;

    /**
     * 在 Java 25 虚拟线程上异步执行邮件投递任务
     * 具备先持久化落库 (SENDING 状态) -> 虚拟线程非阻塞投递 -> 终态状态机推进 (SENT / DELIVERED / FAILED)
     */
    public CompletableFuture<Void> executeAsyncMail(MailMessageBO message) {
        String messageId = message.getMessageId();
        if (!StringUtils.hasText(messageId)) {
            messageId = IdUtil.fastSimpleUUID();
            message.setMessageId(messageId);
        }

        EmailLog initialLog = EmailLog.builder()
                .messageId(messageId)
                .recipient(message.getTo())
                .subject(message.getSubject())
                .content(message.getContent() != null ? message.getContent() : message.getFallbackContent())
                .type(StringUtils.hasText(message.getType()) ? message.getType() : "SYSTEM")
                .status("SENDING")
                .sender(message.getFrom())
                .operatorId(message.getOperatorId())
                .operatorIp(message.getOperatorIp())
                .build();

        final EmailLog savedLog = emailLogRepository.save(initialLog);
        final Long logId = savedLog.getId();

        return volurThreadManager.runAsync(() -> {
            int attempt = 0;
            boolean sent = false;
            Exception lastException = null;

            while (attempt < MAX_RETRY_ATTEMPTS && !sent) {
                attempt++;
                try {
                    mailDispatcher.dispatch(message);
                    sent = true;
                    log.info("[Volur-Verification] 邮件虚拟线程投递成功: to={}, subject={}", message.getTo(), message.getSubject());
                } catch (Exception ex) {
                    lastException = ex;
                    log.warn("[Volur-Verification] 邮件虚拟线程投递第 {} 次尝试失败: to={}, error={}", attempt, message.getTo(), ex.getMessage());
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        try {
                            long backoffMs = (long) Math.pow(2, attempt - 1) * 1000L;
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            try {
                EmailLog logToUpdate = emailLogRepository.findById(logId).orElse(null);
                if (logToUpdate != null) {
                    if (sent) {
                        boolean webhookEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_WEBHOOK_ENABLED, false);
                        if (webhookEnabled) {
                            // 开启 Webhook 回调：初始投递成功标记为 SENT，等待外部 Webhook 回调事件推进终态
                            logToUpdate.setStatus("SENT");
                        } else {
                            // 备用策略固定为 SMTP 250 OK 即为 DELIVERED
                            logToUpdate.setStatus("DELIVERED");
                        }
                        logToUpdate.setErrorMessage(null);
                    } else {
                        logToUpdate.setStatus("FAILED");
                        logToUpdate.setErrorMessage(lastException != null ? lastException.getMessage() : "发送超时或重试耗尽");
                    }
                    emailLogRepository.save(logToUpdate);
                }
            } catch (Exception saveEx) {
                log.error("[Volur-Verification] 异步更新邮件日志终态失败: logId={}", logId, saveEx);
            }
        });
    }

}
