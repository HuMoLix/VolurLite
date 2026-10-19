package com.humolix.volur.notification.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalSseManager {

    private static final Long DEFAULT_SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> userEmitterMap = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> topicEmitterMap = new ConcurrentHashMap<>();
    private final List<SseEmitter> allActiveEmitters = new CopyOnWriteArrayList<>();
    private final Map<SseEmitter, EmitterMetadata> emitterMetadataMap = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId, Set<String> topics) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT);
        Set<String> cleanTopics = topics != null ? new HashSet<>(topics) : Collections.emptySet();
        EmitterMetadata metadata = new EmitterMetadata(userId, cleanTopics);

        allActiveEmitters.add(emitter);
        emitterMetadataMap.put(emitter, metadata);

        if (userId != null) {
            userEmitterMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        }

        for (String topic : cleanTopics) {
            if (topic != null && !topic.isBlank()) {
                topicEmitterMap.computeIfAbsent(topic.trim(), k -> new CopyOnWriteArrayList<>()).add(emitter);
            }
        }

        emitter.onCompletion(() -> removeEmitter(emitter));
        emitter.onTimeout(() -> removeEmitter(emitter));
        emitter.onError(e -> removeEmitter(emitter));

        try {
            Map<String, Object> welcomeData = Map.of(
                    "status", "connected",
                    "userId", userId != null ? userId : "ANONYMOUS",
                    "serverTime", Instant.now().toString(),
                    "heartbeatIntervalSeconds", 25
            );
            emitter.send(SseEmitter.event().name("connected").data(welcomeData));
        } catch (IOException e) {
            log.warn("[SSE] 初始连接事件发送失败，移除连接 -> userId: {}", userId);
            removeEmitter(emitter);
        }

        log.info("[SSE] 新连接建立 -> userId: {}, topics: {}, 当前全局在线连接数: {}, 登录用户数: {}",
                userId, cleanTopics, allActiveEmitters.size(), userEmitterMap.size());
        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = userEmitterMap.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                log.debug("[SSE] 用户 [{}] 推送失败，标记移除", userId);
                deadEmitters.add(emitter);
            }
        }
        deadEmitters.forEach(this::removeEmitter);
    }

    public void broadcastAll(String eventName, Object data) {
        if (allActiveEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : allActiveEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        deadEmitters.forEach(this::removeEmitter);
    }

    public void broadcastToTopic(String topic, String eventName, Object data) {
        if (topic == null || topic.isBlank()) {
            return;
        }
        List<SseEmitter> emitters = topicEmitterMap.get(topic.trim());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        deadEmitters.forEach(this::removeEmitter);
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (allActiveEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        long timestamp = Instant.now().toEpochMilli();
        for (SseEmitter emitter : allActiveEmitters) {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(timestamp));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        deadEmitters.forEach(this::removeEmitter);
    }

    public void removeEmitter(SseEmitter emitter) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 连接已断开或已完成时忽略
        }
        allActiveEmitters.remove(emitter);
        EmitterMetadata metadata = emitterMetadataMap.remove(emitter);
        if (metadata != null) {
            if (metadata.userId() != null) {
                List<SseEmitter> userList = userEmitterMap.get(metadata.userId());
                if (userList != null) {
                    userList.remove(emitter);
                    if (userList.isEmpty()) {
                        userEmitterMap.remove(metadata.userId());
                    }
                }
            }
            for (String topic : metadata.topics()) {
                List<SseEmitter> topicList = topicEmitterMap.get(topic);
                if (topicList != null) {
                    topicList.remove(emitter);
                    if (topicList.isEmpty()) {
                        topicEmitterMap.remove(topic);
                    }
                }
            }
        }
    }

    public int getActiveConnectionCount() {
        return allActiveEmitters.size();
    }

    public int getOnlineUserCount() {
        return userEmitterMap.size();
    }

    private record EmitterMetadata(Long userId, Set<String> topics) {
    }

}
