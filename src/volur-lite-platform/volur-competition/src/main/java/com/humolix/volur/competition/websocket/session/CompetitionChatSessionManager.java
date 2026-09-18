package com.humolix.volur.competition.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CompetitionChatSessionManager {

    /**
     * 发送超时时间: 10秒，缓冲区上限: 512KB
     */
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    /**
     * 战队会话映射: competitionId -> (teamId -> Set<ConcurrentWebSocketSessionDecorator>)
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Set<WebSocketSession>>> competitionTeamSessions = new ConcurrentHashMap<>();

    /**
     * 裁判会话映射: competitionId -> Set<ConcurrentWebSocketSessionDecorator>
     */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> competitionRefereeSessions = new ConcurrentHashMap<>();

    /**
     * 用户会话映射: userId -> Set<ConcurrentWebSocketSessionDecorator> (用于精准私信点对点推送)
     */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 会话上下文索引: sessionId -> SessionContext
     */
    private final ConcurrentHashMap<String, SessionContext> sessions = new ConcurrentHashMap<>();

    /**
     * 注册并封装高并发安全会话
     */
    public WebSocketSession registerSession(SessionContext context, WebSocketSession rawSession) {
        ConcurrentWebSocketSessionDecorator decoratedSession =
                new ConcurrentWebSocketSessionDecorator(rawSession, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);

        context.setSession(decoratedSession);
        sessions.put(rawSession.getId(), context);

        Long competitionId = context.getCompetitionId();
        Long teamId = context.getTeamId();
        Long userId = context.getUserId();

        // 注册到用户独立会话池
        if (userId != null) {
            userSessions
                    .computeIfAbsent(userId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(decoratedSession);
        }

        // 注册到战队会话池
        if (teamId != null) {
            competitionTeamSessions
                    .computeIfAbsent(competitionId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(teamId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(decoratedSession);
        }

        // 若为裁判/管理员，注册到裁判会话池
        if (context.isReferee()) {
            competitionRefereeSessions
                    .computeIfAbsent(competitionId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(decoratedSession);
        }

        log.info("[WebSocket-Chat] 用户连接建立: userId={}, username={}, compId={}, teamId={}, isReferee={}",
                context.getUserId(), context.getUsername(), competitionId, teamId, context.isReferee());

        return decoratedSession;
    }

    /**
     * 移除会话
     */
    public SessionContext removeSession(String sessionId) {
        SessionContext context = sessions.remove(sessionId);
        if (context == null) {
            return null;
        }

        Long competitionId = context.getCompetitionId();
        Long teamId = context.getTeamId();
        WebSocketSession session = context.getSession();

        if (teamId != null && session != null) {
            ConcurrentHashMap<Long, Set<WebSocketSession>> teamMap = competitionTeamSessions.get(competitionId);
            if (teamMap != null) {
                Set<WebSocketSession> set = teamMap.get(teamId);
                if (set != null) {
                    set.remove(session);
                    if (set.isEmpty()) {
                        teamMap.remove(teamId);
                    }
                }
                if (teamMap.isEmpty()) {
                    competitionTeamSessions.remove(competitionId);
                }
            }
        }

        if (context.getUserId() != null && session != null) {
            Set<WebSocketSession> userSet = userSessions.get(context.getUserId());
            if (userSet != null) {
                userSet.remove(session);
                if (userSet.isEmpty()) {
                    userSessions.remove(context.getUserId());
                }
            }
        }

        if (context.isReferee() && session != null) {
            Set<WebSocketSession> refereeSet = competitionRefereeSessions.get(competitionId);
            if (refereeSet != null) {
                refereeSet.remove(session);
                if (refereeSet.isEmpty()) {
                    competitionRefereeSessions.remove(competitionId);
                }
            }
        }

        log.info("[WebSocket-Chat] 用户连接断开注销: userId={}, username={}, sessionId={}",
                context.getUserId(), context.getUsername(), sessionId);

        return context;
    }

    public SessionContext getContext(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 向指定竞赛指定战队广播消息
     */
    public void broadcastToTeam(Long competitionId, Long teamId, String jsonPayload) {
        if (competitionId == null || teamId == null) return;
        ConcurrentHashMap<Long, Set<WebSocketSession>> teamMap = competitionTeamSessions.get(competitionId);
        if (teamMap == null) return;
        Set<WebSocketSession> teamSet = teamMap.get(teamId);
        if (teamSet == null || teamSet.isEmpty()) return;

        TextMessage message = new TextMessage(jsonPayload);
        for (WebSocketSession session : teamSet) {
            sendSafely(session, message);
        }
    }

    /**
     * 发送队员私信消息（仅定向推送给发送者与指定接收者的连接）
     */
    public void broadcastPrivateMessage(Long senderId, Long receiverId, String jsonPayload) {
        TextMessage message = new TextMessage(jsonPayload);
        sendToUser(senderId, message);
        if (receiverId != null && !receiverId.equals(senderId)) {
            sendToUser(receiverId, message);
        }
    }

    public void sendToUser(Long userId, TextMessage message) {
        if (userId == null) return;
        Set<WebSocketSession> userSet = userSessions.get(userId);
        if (userSet == null || userSet.isEmpty()) return;
        for (WebSocketSession session : userSet) {
            sendSafely(session, message);
        }
    }

    /**
     * 向指定竞赛所有裁判广播消息
     */
    public void broadcastToReferees(Long competitionId, String jsonPayload) {
        if (competitionId == null) return;
        Set<WebSocketSession> refereeSet = competitionRefereeSessions.get(competitionId);
        if (refereeSet == null || refereeSet.isEmpty()) return;

        TextMessage message = new TextMessage(jsonPayload);
        for (WebSocketSession session : refereeSet) {
            sendSafely(session, message);
        }
    }

    /**
     * 向指定队伍与裁判组联合广播（裁判工单通道专用）
     */
    public void broadcastToTeamAndReferees(Long competitionId, Long teamId, String jsonPayload) {
        broadcastToTeam(competitionId, teamId, jsonPayload);
        broadcastToReferees(competitionId, jsonPayload);
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("[WebSocket-Chat] 消息推送失败: sessionId={}, error={}", session.getId(), e.getMessage());
            }
        }
    }

    public int getActiveConnectionCount() {
        return sessions.size();
    }

}
