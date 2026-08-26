package com.humolix.volur.notification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.notification.sse.GlobalSseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;

@Tag(name = "实时事件流 (SSE)", description = "全局实时事件流订阅与长连接状态监控接口")
@RestController
@RequestMapping("/api/v1/realtime")
@RequiredArgsConstructor
public class RealtimeStreamController {

    private final GlobalSseManager globalSseManager;

    @Operation(summary = "订阅全站实时事件流 (SSE)")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream(
            @CurrentUserId(required = false) Long userId,
            @RequestParam(required = false) Set<String> topics) {
        return globalSseManager.connect(userId, topics);
    }

    @Operation(summary = "获取实时连接统计信息")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStreamStats() {
        Map<String, Object> stats = Map.of(
                "activeConnections", globalSseManager.getActiveConnectionCount(),
                "onlineUsers", globalSseManager.getOnlineUserCount()
        );
        return ApiResponseEntity.ok("获取实时连接状态成功", stats);
    }

}
