package com.humolix.volur.notification.redis;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.notification.domain.bo.RealtimeMessageBO;
import com.humolix.volur.notification.sse.GlobalSseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeRedisSubscriber implements MessageListener {

    private final GlobalSseManager globalSseManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            RealtimeMessageBO messageBO = objectMapper.readValue(json, RealtimeMessageBO.class);
            if (messageBO == null || messageBO.getEventType() == null) {
                return;
            }

            String eventType = messageBO.getEventType();
            Object payload = messageBO.getPayload();

            if (messageBO.getTargetUserId() != null) {
                // 单播给指定用户
                globalSseManager.sendToUser(messageBO.getTargetUserId(), eventType, payload);
            } else if (messageBO.getTopic() != null && !messageBO.getTopic().isBlank()) {
                // 组播给指定主题
                globalSseManager.broadcastToTopic(messageBO.getTopic(), eventType, payload);
            } else {
                // 全站全局广播
                globalSseManager.broadcastAll(eventType, payload);
            }
        } catch (Exception ex) {
            log.error("[Redis-PubSub] 处理实时广播消息异常", ex);
        }
    }

}
