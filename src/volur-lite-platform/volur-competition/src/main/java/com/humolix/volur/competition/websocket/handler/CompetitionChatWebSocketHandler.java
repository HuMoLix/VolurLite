package com.humolix.volur.competition.websocket.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.humolix.volur.competition.domain.dto.chat.ChatMessageSendDTO;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import com.humolix.volur.competition.service.CompetitionChatService;
import com.humolix.volur.competition.websocket.interceptor.CompetitionChatHandshakeInterceptor;
import com.humolix.volur.competition.websocket.session.CompetitionChatSessionManager;
import com.humolix.volur.competition.websocket.session.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatWebSocketHandler extends TextWebSocketHandler {

    private final CompetitionChatSessionManager sessionManager;
    private final CompetitionChatService chatService;

    /**
     * 防刷流控：每 2 秒内单个用户最多允许发送 6 条聊天消息
     */
    private final Cache<Long, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .maximumSize(50_000)
            .build();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();

        Long userId = (Long) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_USER_ID);
        String username = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_USERNAME);
        Long competitionId = (Long) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_COMPETITION_ID);
        Long teamId = (Long) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_TEAM_ID);
        String teamName = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_TEAM_NAME);
        Boolean isReferee = (Boolean) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_IS_REFEREE);
        String role = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_ROLE);
        String avatar = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_AVATAR);

        SessionContext context = SessionContext.builder()
                .sessionId(session.getId())
                .userId(userId)
                .username(username)
                .competitionId(competitionId)
                .teamId(teamId)
                .teamName(teamName)
                .isReferee(Boolean.TRUE.equals(isReferee))
                .role(role)
                .avatar(avatar)
                .build();

        WebSocketSession decoratedSession = sessionManager.registerSession(context, session);

        // 发送握手成功就绪帧（附带各频道未读数，避免进入页面后重复拉取冗余数据）
        com.humolix.volur.competition.domain.vo.chat.ChatUnreadCountVO unreadVO =
                chatService.getUnreadCount(competitionId, userId);

        java.util.Map<String, Object> readyData = new java.util.HashMap<>();
        readyData.put("userId", userId != null ? String.valueOf(userId) : "");
        readyData.put("username", username != null ? username : "");
        readyData.put("competitionId", competitionId != null ? String.valueOf(competitionId) : "");
        readyData.put("teamId", teamId != null ? String.valueOf(teamId) : "0");
        readyData.put("teamName", teamName != null ? teamName : "");
        readyData.put("isReferee", Boolean.TRUE.equals(isReferee));
        readyData.put("role", role != null ? role : "MEMBER");
        readyData.put("unread", unreadVO);

        Map<String, Object> readyFrame = Map.of(
                "type", "CONNECTED",
                "data", readyData
        );
        decoratedSession.sendMessage(new TextMessage(JSON.toJSONString(readyFrame, com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String raw = message.getPayload();
        if (!StringUtils.hasText(raw)) {
            return;
        }

        JSONObject frame;
        try {
            frame = JSON.parseObject(raw);
        } catch (Exception e) {
            log.warn("[WebSocket-Chat] 非法 JSON 数据帧: sessionId={}, raw={}", session.getId(), raw);
            sendError(session, "非法数据帧格式");
            return;
        }

        if (frame == null) return;
        String type = frame.getString("type");
        if (type == null) return;

        SessionContext context = sessionManager.getContext(session.getId());
        if (context == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        switch (type.toUpperCase()) {
            case "PING" -> {
                // 心跳响应
                Map<String, Object> pongFrame = Map.of(
                        "type", "PONG",
                        "timestamp", System.currentTimeMillis()
                );
                session.sendMessage(new TextMessage(JSON.toJSONString(pongFrame)));
            }
            case "SEND" -> {
                // 防刷流控检查
                AtomicInteger counter = rateLimitCache.get(context.getUserId(), k -> new AtomicInteger(0));
                if (counter != null && counter.incrementAndGet() > 6) {
                    sendError(session, "发送过于频繁，请稍后再试");
                    return;
                }

                JSONObject payloadJson = frame.getJSONObject("payload");
                if (payloadJson == null) {
                    sendError(session, "缺少消息有效载荷 payload");
                    return;
                }

                ChatMessageSendDTO sendDTO = payloadJson.toJavaObject(ChatMessageSendDTO.class);
                if (sendDTO == null || !StringUtils.hasText(sendDTO.getContent())) {
                    sendError(session, "消息内容不能为空");
                    return;
                }

                // 权限保护：非裁判用户不可篡改目标队伍 ID
                if (!context.isReferee()) {
                    sendDTO.setTeamId(context.getTeamId());
                }

                try {
                    ChatMessageVO vo = chatService.sendMessage(context.getCompetitionId(), context.getUserId(), sendDTO);
                    // 确认应答 ACK
                    String clientMsgId = frame.getString("clientMsgId");
                    Map<String, Object> ackFrame = Map.of(
                            "type", "SEND_ACK",
                            "clientMsgId", clientMsgId != null ? clientMsgId : "",
                            "messageId", String.valueOf(vo.getId())
                    );
                    session.sendMessage(new TextMessage(JSON.toJSONString(ackFrame, com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString)));
                } catch (Exception e) {
                    log.error("[WebSocket-Chat] 消息发送处理异常: {}", e.getMessage(), e);
                    sendError(session, e.getMessage());
                }
            }
            case "READ_ACK" -> {
                // 客户端已读同步确认
                JSONObject payloadJson = frame.getJSONObject("payload");
                if (payloadJson != null) {
                    String target = payloadJson.getString("target");
                    Long lastMessageId = payloadJson.getLong("lastMessageId");
                    if (StringUtils.hasText(target)) {
                        chatService.markRead(context.getCompetitionId(), context.getUserId(),
                                new com.humolix.volur.competition.domain.dto.chat.ChatReadDTO(target, lastMessageId));
                    }
                }
            }
            default -> log.debug("[WebSocket-Chat] 未知指令类型: {}", type);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[WebSocket-Chat] 传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        sessionManager.removeSession(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session.getId());
    }

    private void sendError(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> errFrame = Map.of(
                        "type", "ERROR",
                        "message", message != null ? message : "服务器处理异常"
                );
                session.sendMessage(new TextMessage(JSON.toJSONString(errFrame)));
            } catch (IOException ignored) {
            }
        }
    }

}
