package com.humolix.volur.competition.websocket.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.humolix.volur.competition.domain.dto.chat.ChatMessageSendDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatReadDTO;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import com.humolix.volur.competition.domain.vo.chat.ChatUnreadCountVO;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatWebSocketHandler extends TextWebSocketHandler {

    private final CompetitionChatSessionManager sessionManager;
    private final CompetitionChatService chatService;
    private final ObjectMapper objectMapper;

    private final Cache<Long, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .maximumSize(50_000)
            .build();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long competitionId = (Long) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_COMPETITION_ID);
        Long userId = (Long) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_USER_ID);
        String username = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_USERNAME);
        Boolean isReferee = (Boolean) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_IS_REFEREE);
        Long teamId = (Long) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_TEAM_ID);
        String teamName = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_TEAM_NAME);
        String role = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_ROLE);
        String avatar = (String) attributes.get(CompetitionChatHandshakeInterceptor.ATTR_AVATAR);

        if (competitionId == null || userId == null) {
            log.warn("[WebSocket-Chat] 握手参数缺失，关闭连接: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        SessionContext context = SessionContext.builder()
                .sessionId(session.getId())
                .competitionId(competitionId)
                .userId(userId)
                .username(username != null ? username : "")
                .isReferee(Boolean.TRUE.equals(isReferee))
                .teamId(teamId)
                .teamName(teamName != null ? teamName : "")
                .role(role != null ? role : "MEMBER")
                .avatar(avatar)
                .build();

        WebSocketSession decoratedSession = sessionManager.registerSession(context, session);
        ChatUnreadCountVO unreadVO = chatService.getUnreadCount(competitionId, userId);

        Map<String, Object> readyData = new HashMap<>();
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
        decoratedSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(readyFrame)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String raw = message.getPayload();
        if (!StringUtils.hasText(raw)) {
            return;
        }

        JsonNode frame;
        try {
            frame = objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("[WebSocket-Chat] 非法 JSON 数据帧: sessionId={}, raw={}", session.getId(), raw);
            sendError(session, "非法数据帧格式");
            return;
        }

        if (frame == null || frame.isMissingNode() || !frame.hasNonNull("type")) {
            return;
        }
        String type = frame.get("type").asText();

        SessionContext context = sessionManager.getContext(session.getId());
        if (context == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        switch (type.toUpperCase()) {
            case "PING" -> {
                Map<String, Object> pongFrame = Map.of(
                        "type", "PONG",
                        "timestamp", System.currentTimeMillis()
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongFrame)));
            }
            case "SEND" -> {
                AtomicInteger counter = rateLimitCache.get(context.getUserId(), k -> new AtomicInteger(0));
                if (counter != null && counter.incrementAndGet() > 6) {
                    sendError(session, "发送过于频繁，请稍后再试");
                    return;
                }

                JsonNode payloadJson = frame.path("payload");
                if (payloadJson.isMissingNode() || payloadJson.isNull()) {
                    sendError(session, "缺少消息有效载荷 payload");
                    return;
                }

                ChatMessageSendDTO sendDTO = objectMapper.treeToValue(payloadJson, ChatMessageSendDTO.class);
                if (sendDTO == null || !StringUtils.hasText(sendDTO.getContent())) {
                    sendError(session, "消息内容不能为空");
                    return;
                }

                if (!context.isReferee() || sendDTO.getTeamId() == null) {
                    if (context.getTeamId() != null) {
                        sendDTO.setTeamId(context.getTeamId());
                    }
                }

                try {
                    ChatMessageVO vo = chatService.sendMessage(context.getCompetitionId(), context.getUserId(), sendDTO);
                    String clientMsgId = frame.path("clientMsgId").asText("");
                    Map<String, Object> ackFrame = Map.of(
                            "type", "SEND_ACK",
                            "clientMsgId", clientMsgId,
                            "messageId", String.valueOf(vo.getId())
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackFrame)));
                } catch (Exception e) {
                    log.error("[WebSocket-Chat] 消息发送处理异常: {}", e.getMessage(), e);
                    sendError(session, e.getMessage());
                }
            }
            case "READ_ACK" -> {
                JsonNode payloadJson = frame.path("payload");
                if (!payloadJson.isMissingNode() && !payloadJson.isNull()) {
                    String target = payloadJson.path("target").asText(null);
                    Long lastMessageId = payloadJson.hasNonNull("lastMessageId") ? payloadJson.get("lastMessageId").asLong() : null;
                    if (StringUtils.hasText(target)) {
                        chatService.markRead(context.getCompetitionId(), context.getUserId(),
                                new ChatReadDTO(target, lastMessageId));
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
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errFrame)));
            } catch (IOException ignored) {
            }
        }
    }
}
