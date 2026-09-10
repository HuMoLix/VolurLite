package com.humolix.volur.verification.mail.executor;

import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.dispatcher.FailoverMailDispatcher;
import com.humolix.volur.verification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailTaskExecutor {

    private static final int MAX_CONCURRENT_MAILS = 50;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_MAILS);
    private final FailoverMailDispatcher mailDispatcher;
    private final EmailLogRepository emailLogRepository;

    @Async
    public void executeAsyncMail(MailMessageBO message) {
        boolean acquired = false;
        Exception lastException = null;
        boolean sent = false;
        try {
            semaphore.acquire();
            acquired = true;

            int attempt = 0;

            while (attempt < MAX_RETRY_ATTEMPTS && !sent) {
                attempt++;
                try {
                    mailDispatcher.dispatch(message);
                    sent = true;
                } catch (Exception ex) {
                    lastException = ex;
                    log.warn("[Volur-Verification] 异步发送邮件第 {} 次尝试失败: to={}, error={}", attempt, message.getTo(), ex.getMessage());
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            lastException = e;
            log.warn("[Volur-Verification] 邮件发送线程被中断: to={}", message.getTo());
        } catch (Exception ex) {
            lastException = ex;
            log.error("[Volur-Verification] 邮件异步发送异常: to={}, subject={}", message.getTo(), message.getSubject(), ex);
        } finally {
            if (acquired) {
                semaphore.release();
            }
            try {
                if (emailLogRepository != null) {
                    EmailLog emailLog = EmailLog.builder()
                            .recipient(message.getTo())
                            .subject(message.getSubject())
                            .content(message.getContent() != null ? message.getContent() : message.getFallbackContent())
                            .type(StringUtils.hasText(message.getType()) ? message.getType() : "SYSTEM")
                            .status(sent ? "SUCCESS" : "FAILED")
                            .errorMessage(sent ? null : (lastException != null ? lastException.getMessage() : "发送超时或多次重试失败"))
                            .sender(message.getFrom())
                            .operatorId(message.getOperatorId())
                            .operatorIp(message.getOperatorIp())
                            .build();
                    emailLogRepository.save(emailLog);
                }
            } catch (Exception saveEx) {
                log.error("[Volur-Verification] 记录邮件日志失败: to={}, subject={}", message.getTo(), message.getSubject(), saveEx);
            }
        }
    }

}
