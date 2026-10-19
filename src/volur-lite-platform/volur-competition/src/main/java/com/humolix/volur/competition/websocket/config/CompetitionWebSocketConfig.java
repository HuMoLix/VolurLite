package com.humolix.volur.competition.websocket.config;

import com.humolix.volur.competition.websocket.handler.CompetitionChatWebSocketHandler;
import com.humolix.volur.competition.websocket.interceptor.CompetitionChatHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CompetitionWebSocketConfig implements WebSocketConfigurer {

    private final CompetitionChatWebSocketHandler chatWebSocketHandler;
    private final CompetitionChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/competitions/{competitionId}/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*");

        log.info("[WebSocket] 竞赛实时聊天 WebSocket 路由注册成功: /ws/competitions/{competitionId}/chat");
    }

}
