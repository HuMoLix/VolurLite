package com.humolix.volur.verification.mail.executor;

import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.dispatcher.FailoverMailDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailTaskExecutor {

    private static final int MAX_CONCURRENT_MAILS = 50;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_MAILS);
    private final FailoverMailDispatcher mailDispatcher;

    @Async
    public void executeAsyncMail(MailMessageBO message) {
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;

            int attempt = 0;
            boolean sent = false;

            while (attempt < MAX_RETRY_ATTEMPTS && !sent) {
                attempt++;
                try {
                    mailDispatcher.dispatch(message);
                    sent = true;
                } catch (Exception ex) {
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
            log.warn("[Volur-Verification] 邮件发送线程被中断: to={}", message.getTo());
        } catch (Exception ex) {
            log.error("[Volur-Verification] 邮件异步发送异常: to={}, subject={}", message.getTo(), message.getSubject(), ex);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

}
