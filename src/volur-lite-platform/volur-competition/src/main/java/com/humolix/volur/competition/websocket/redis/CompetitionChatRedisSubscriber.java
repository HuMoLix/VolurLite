package com.humolix.volur.competition.websocket.redis;

import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.websocket.model.CompetitionChatBroadcastMessage;
import com.humolix.volur.competition.websocket.session.CompetitionChatSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatRedisSubscriber implements MessageListener {

    private final CompetitionChatSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            CompetitionChatBroadcastMessage broadcast = objectMapper.readValue(json, CompetitionChatBroadcastMessage.class);

            if (broadcast == null || broadcast.getCompetitionId() == null || broadcast.getPayload() == null) {
                return;
            }

            Map<String, Object> frame = Map.of(
                    "type", "MESSAGE",
                    "channelType", broadcast.getChannelType().name(),
                    "teamId", String.valueOf(broadcast.getTeamId()),
                    "data", broadcast.getPayload()
            );
            String clientFrameJson = objectMapper.writeValueAsString(frame);

            if (broadcast.getChannelType() == ChatChannelType.TEAM) {
                sessionManager.broadcastToTeam(broadcast.getCompetitionId(), broadcast.getTeamId(), clientFrameJson);
                if (broadcast.getSenderId() != null) {
                    sessionManager.sendToUser(broadcast.getSenderId(), new TextMessage(clientFrameJson));
                }
            } else if (broadcast.getChannelType() == ChatChannelType.PRIVATE) {
                sessionManager.broadcastPrivateMessage(broadcast.getSenderId(), broadcast.getReceiverId(), clientFrameJson);
            } else if (broadcast.getChannelType() == ChatChannelType.REFEREE) {
                sessionManager.broadcastToTeamAndReferees(broadcast.getCompetitionId(), broadcast.getTeamId(), clientFrameJson);
            }

        } catch (Exception e) {
            log.error("[WebSocket-Chat] 处理 Redis 聊天广播异常: {}", e.getMessage(), e);
        }
    }

}
