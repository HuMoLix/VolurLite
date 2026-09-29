package com.humolix.volur.verification;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.repository.EmailLogRepository;
import com.humolix.volur.verification.service.MailWebhookService;
import com.humolix.volur.verification.service.impl.MailWebhookServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MailWebhookServiceTest {

    private EmailLogRepository emailLogRepository;
    private SystemSettingService systemSettingService;
    private ObjectMapper objectMapper;
    private MailWebhookService mailWebhookService;

    @BeforeEach
    public void setup() {
        emailLogRepository = Mockito.mock(EmailLogRepository.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        objectMapper = new ObjectMapper();

        mailWebhookService = new MailWebhookServiceImpl(
                emailLogRepository,
                systemSettingService,
                objectMapper
        );
    }

    @Test
    public void testWebhookDisabled_NoOp() {
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_WEBHOOK_ENABLED), eq(false))).thenReturn(false);

        mailWebhookService.processWebhook("{\"type\":\"email.delivered\"}", "any-secret");

        verify(emailLogRepository, never()).save(any());
    }

    @Test
    public void testWebhookSecretMismatch_ThrowsUnauthorized() {
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_WEBHOOK_ENABLED), eq(false))).thenReturn(true);
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_WEBHOOK_SECRET), anyString())).thenReturn("secret-123");

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () ->
                mailWebhookService.processWebhook("{\"type\":\"email.delivered\"}", "wrong-secret")
        );
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(emailLogRepository, never()).save(any());
    }

    @Test
    public void testResendDeliveredEvent_UpdatesStatusToDelivered() {
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_WEBHOOK_ENABLED), eq(false))).thenReturn(true);
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_WEBHOOK_SECRET), anyString())).thenReturn("");

        EmailLog emailLog = EmailLog.builder()
                .id(1001L)
                .messageId("msg-abc-123")
                .recipient("alice@example.com")
                .status("SENT")
                .build();

        when(emailLogRepository.findByMessageId("msg-abc-123")).thenReturn(Optional.of(emailLog));

        String payload = """
                {
                    "type": "email.delivered",
                    "data": {
                        "email_id": "msg-abc-123",
                        "to": ["alice@example.com"],
                        "subject": "Hello"
                    }
                }
                """;

        mailWebhookService.processWebhook(payload, null);

        Assertions.assertEquals("DELIVERED", emailLog.getStatus());
        Assertions.assertNull(emailLog.getErrorMessage());
        verify(emailLogRepository).save(emailLog);
    }

    @Test
    public void testResendBouncedEvent_UpdatesStatusToBounced() {
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_WEBHOOK_ENABLED), eq(false))).thenReturn(true);
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_WEBHOOK_SECRET), anyString())).thenReturn("token-xyz");

        EmailLog emailLog = EmailLog.builder()
                .id(1002L)
                .messageId("msg-bounce-456")
                .recipient("baduser@example.com")
                .status("SENT")
                .build();

        when(emailLogRepository.findByMessageId("msg-bounce-456")).thenReturn(Optional.of(emailLog));

        String payload = """
                {
                    "type": "email.bounced",
                    "data": {
                        "email_id": "msg-bounce-456",
                        "to": ["baduser@example.com"],
                        "bounce": {
                            "message": "Mailbox not found"
                        }
                    }
                }
                """;

        mailWebhookService.processWebhook(payload, "token-xyz");

        Assertions.assertEquals("BOUNCED", emailLog.getStatus());
        Assertions.assertEquals("Mailbox not found", emailLog.getErrorMessage());
        verify(emailLogRepository).save(emailLog);
    }

    @Test
    public void testSendGridDeliveredEvent_UpdatesStatusByRecipient() {
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_WEBHOOK_ENABLED), eq(false))).thenReturn(true);
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_WEBHOOK_SECRET), anyString())).thenReturn("");

        EmailLog emailLog = EmailLog.builder()
                .id(1003L)
                .recipient("bob@example.com")
                .status("SENT")
                .build();

        when(emailLogRepository.findByMessageId("")).thenReturn(Optional.empty());
        when(emailLogRepository.findTop5ByRecipientAndStatusInOrderByCreatedAtDesc(eq("bob@example.com"), any()))
                .thenReturn(List.of(emailLog));

        String payload = """
                [
                    {
                        "event": "delivered",
                        "email": "bob@example.com"
                    }
                ]
                """;

        mailWebhookService.processWebhook(payload, null);

        Assertions.assertEquals("DELIVERED", emailLog.getStatus());
        verify(emailLogRepository).save(emailLog);
    }

}
