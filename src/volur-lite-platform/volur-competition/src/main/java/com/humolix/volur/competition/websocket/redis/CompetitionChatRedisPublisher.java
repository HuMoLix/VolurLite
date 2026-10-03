package com.humolix.volur.competition.websocket.redis;

import com.humolix.volur.competition.websocket.model.CompetitionChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatRedisPublisher {

    public static final String VREDIS_COMPETITION_CHAT_CHANNEL = "vredis:competition:chat:pubsub";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(CompetitionChatBroadcastMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(VREDIS_COMPETITION_CHAT_CHANNEL, json);
        } catch (Exception e) {
            log.error("[WebSocket-Chat] Redis 广播消息发布失败: {}", e.getMessage(), e);
        }
    }

}
