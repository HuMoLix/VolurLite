package com.humolix.volur.notification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.notification.sse.GlobalSseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/realtime")
@RequiredArgsConstructor
public class RealtimeStreamController {

    private final GlobalSseManager globalSseManager;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream(
            @CurrentUserId(required = false) Long userId,
            @RequestParam(required = false) Set<String> topics) {
        return globalSseManager.connect(userId, topics);
    }

    @GetMapping("/stats")
    public ApiResponseEntity<Map<String, Object>> getStreamStats() {
        Map<String, Object> stats = Map.of(
                "activeConnections", globalSseManager.getActiveConnectionCount(),
                "onlineUsers", globalSseManager.getOnlineUserCount()
        );
        return ApiResponseEntity.ok("获取实时连接状态成功", stats);
    }

}
