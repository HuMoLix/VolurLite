package com.humolix.volur.notification.listener;

import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SecurityNotificationEvent;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.humolix.volur.notification.service.NotificationService;
import com.humolix.volur.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final NotificationService notificationService;
    private final VolurThreadManager volurThreadManager;

    @Async("volurVirtualExecutor")
    @EventListener
    public void handleSendNotification(SendNotificationEvent event) {
        if (event == null || event.getReceiverId() == null) {
            return;
        }
        log.debug("[Notification] 收到异步站内信发送事件 -> 接收者: {}, 类型: {}, 标题: {}",
                event.getReceiverId(), event.getType(), event.getTitle());

        volurThreadManager.runWithPermit(() -> {
            try {
                notificationService.sendNotification(event.toBO());
            } catch (Exception ex) {
                log.error("[Notification] 虚拟线程异步发送站内信失败 -> 接收者: {}, 类型: {}, 错误: {}",
                        event.getReceiverId(), event.getType(), ex.getMessage(), ex);
            }
        });
    }

    @Async("volurVirtualExecutor")
    @EventListener
    public void handleSecurityNotification(SecurityNotificationEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }
        log.info("[Notification] 收到安全事件通知 -> 用户: {}, 事件类型: {}, IP: {}",
                event.getUserId(), event.getEventType(), event.getClientIp());

        volurThreadManager.runWithPermit(() -> {
            try {
                String formattedTime = TIME_FORMATTER.format(event.getOccurTime());
                String title = "【安全提醒】" + event.getEventType().getDescription();
                String content = String.format("您的账号于 %s 发生安全事件: %s。操作IP: %s。如非本人操作，请尽快修改密码或联系管理员。",
                        formattedTime,
                        event.getDescription() != null ? event.getDescription() : event.getEventType().getDescription(),
                        event.getClientIp() != null ? event.getClientIp() : "未知"
                );

                Map<String, Object> actionData = Map.of(
                        "eventType", event.getEventType().name(),
                        "occurTime", formattedTime,
                        "clientIp", event.getClientIp() != null ? event.getClientIp() : "UNKNOWN"
                );

                SendNotificationBO sendBO = SendNotificationBO.builder()
                        .senderId(null) // 系统发送
                        .receiverId(event.getUserId())
                        .title(title)
                        .content(content)
                        .type(NotificationType.SECURITY_ALERT)
                        .actionStatus(NotificationActionStatus.NONE)
                        .actionData(JsonUtils.toJsonString(actionData))
                        .build();

                notificationService.sendNotification(sendBO);
            } catch (Exception ex) {
                log.error("[Notification] 处理安全事件通知失败 -> 用户: {}, 错误: {}",
                        event.getUserId(), ex.getMessage(), ex);
            }
        });
    }

}
