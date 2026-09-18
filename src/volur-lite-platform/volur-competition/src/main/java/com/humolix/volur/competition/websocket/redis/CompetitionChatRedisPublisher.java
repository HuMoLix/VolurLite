package com.humolix.volur.competition.websocket.redis;

import com.alibaba.fastjson2.JSON;
import com.humolix.volur.competition.websocket.model.CompetitionChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatRedisPublisher {

    public static final String VREDIS_COMPETITION_CHAT_CHANNEL = "vredis:competition:chat:pubsub";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 将聊天消息广播发布至 Redis 集群通道
     */
    public void publish(CompetitionChatBroadcastMessage message) {
        try {
            String json = JSON.toJSONString(message);
            stringRedisTemplate.convertAndSend(VREDIS_COMPETITION_CHAT_CHANNEL, json);
        } catch (Exception e) {
            log.error("[WebSocket-Chat] Redis 广播消息发布失败: {}", e.getMessage(), e);
        }
    }

}
