package com.humolix.volur.competition.websocket.redis;

import com.alibaba.fastjson2.JSON;
import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.websocket.model.CompetitionChatBroadcastMessage;
import com.humolix.volur.competition.websocket.session.CompetitionChatSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatRedisSubscriber implements MessageListener {

    private final CompetitionChatSessionManager sessionManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            CompetitionChatBroadcastMessage broadcast = JSON.parseObject(json, CompetitionChatBroadcastMessage.class);

            if (broadcast == null || broadcast.getCompetitionId() == null || broadcast.getPayload() == null) {
                return;
            }

            Map<String, Object> frame = Map.of(
                    "type", "MESSAGE",
                    "channelType", broadcast.getChannelType().name(),
                    "teamId", String.valueOf(broadcast.getTeamId()),
                    "data", broadcast.getPayload()
            );
            String clientFrameJson = JSON.toJSONString(frame, com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString);

            if (broadcast.getChannelType() == ChatChannelType.TEAM) {
                // 仅推送给本队伍所有队员
                sessionManager.broadcastToTeam(broadcast.getCompetitionId(), broadcast.getTeamId(), clientFrameJson);
                // 若为裁判发信或非本队会话发送，额外向发送者本人长连接推送回显
                if (broadcast.getSenderId() != null) {
                    sessionManager.sendToUser(broadcast.getSenderId(), new org.springframework.web.socket.TextMessage(clientFrameJson));
                }
            } else if (broadcast.getChannelType() == ChatChannelType.PRIVATE) {
                // 队员点对点私信：仅定向推送给发送者与接收者
                sessionManager.broadcastPrivateMessage(broadcast.getSenderId(), broadcast.getReceiverId(), clientFrameJson);
            } else if (broadcast.getChannelType() == ChatChannelType.REFEREE) {
                // 工单申诉：同时推送给提工单战队及裁判组
                sessionManager.broadcastToTeamAndReferees(broadcast.getCompetitionId(), broadcast.getTeamId(), clientFrameJson);
            }

        } catch (Exception e) {
            log.error("[WebSocket-Chat] 处理 Redis 聊天广播异常: {}", e.getMessage(), e);
        }
    }

}
