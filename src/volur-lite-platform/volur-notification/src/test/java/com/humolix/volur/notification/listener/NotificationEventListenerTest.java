package com.humolix.volur.notification.listener;

import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.domain.enums.SecurityEventType;
import com.humolix.volur.notification.event.SecurityNotificationEvent;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.humolix.volur.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private VolurThreadManager volurThreadManager;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("事件监听测试：handleSendNotification 通过 VolurThreadManager.runWithPermit 异步提交并发送站内信")
    void testHandleSendNotification_executesWithPermit() {
        SendNotificationEvent event = SendNotificationEvent.of(
                this, 1L, 2L, NotificationType.COMPETITION_AUDIT, "【审核通过】", "战队已通过审核"
        );

        when(volurThreadManager.runWithPermit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return CompletableFuture.completedFuture(null);
        });

        listener.handleSendNotification(event);

        verify(volurThreadManager).runWithPermit(any(Runnable.class));

        ArgumentCaptor<SendNotificationBO> boCaptor = ArgumentCaptor.forClass(SendNotificationBO.class);
        verify(notificationService).sendNotification(boCaptor.capture());

        SendNotificationBO captured = boCaptor.getValue();
        assertEquals(1L, captured.getSenderId());
        assertEquals(2L, captured.getReceiverId());
        assertEquals(NotificationType.COMPETITION_AUDIT, captured.getType());
        assertEquals("【审核通过】", captured.getTitle());
        assertEquals("战队已通过审核", captured.getContent());
    }

    @Test
    @DisplayName("事件监听测试：handleSecurityNotification 通过 VolurThreadManager.runWithPermit 处理安全告警通知")
    void testHandleSecurityNotification_executesWithPermit() {
        SecurityNotificationEvent event = new SecurityNotificationEvent(
                this, 10L, SecurityEventType.PASSWORD_CHANGED, "127.0.0.1", "密码修改成功"
        );

        when(volurThreadManager.runWithPermit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return CompletableFuture.completedFuture(null);
        });

        listener.handleSecurityNotification(event);

        verify(volurThreadManager).runWithPermit(any(Runnable.class));

        ArgumentCaptor<SendNotificationBO> boCaptor = ArgumentCaptor.forClass(SendNotificationBO.class);
        verify(notificationService).sendNotification(boCaptor.capture());

        SendNotificationBO captured = boCaptor.getValue();
        assertEquals(10L, captured.getReceiverId());
        assertEquals(NotificationType.SECURITY_ALERT, captured.getType());
    }

}
