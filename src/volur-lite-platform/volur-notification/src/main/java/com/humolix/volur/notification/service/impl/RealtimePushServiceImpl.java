package com.humolix.volur.notification.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import com.humolix.volur.notification.domain.bo.RealtimeMessageBO;
import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.domain.enums.RealtimeEventType;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;
import com.humolix.volur.notification.domain.vo.NotificationVO;
import com.humolix.volur.notification.mapper.NotificationMapper;
import com.humolix.volur.notification.redis.RealtimeRedisConfig;
import com.humolix.volur.notification.service.RealtimePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimePushServiceImpl implements RealtimePushService {

    private final StringRedisTemplate redisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void pushNotification(Long userId, NotificationVO notificationVO) {
        if (userId == null || notificationVO == null) {
            return;
        }

        RealtimeMessageBO messageBO = RealtimeMessageBO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(RealtimeEventType.NOTIFICATION.getValue())
                .targetUserId(userId)
                .payload(notificationVO)
                .timestamp(Instant.now())
                .build();

        publishToRedisChannel(messageBO);
        log.debug("[Realtime] 推送个人站内信 -> userId: {}, notificationId: {}", userId, notificationVO.getId());
    }

    @Override
    public void publishAnnouncement(BroadcastMessageDTO broadcastDTO) {
        if (broadcastDTO == null) {
            return;
        }

        BroadcastMessageVO vo = notificationMapper.toBroadcastVO(broadcastDTO);
        vo.setId(snowflakeIdGenerator.nextId());
        RealtimeMessageBO messageBO = RealtimeMessageBO.builder()
                .eventId(String.valueOf(vo.getId()))
                .eventType(RealtimeEventType.ANNOUNCEMENT.getValue())
                .targetUserId(null)
                .payload(vo)
                .timestamp(Instant.now())
                .build();

        publishToRedisChannel(messageBO);
        log.info("[Realtime] 发布全站系统公告 -> id: {}, title: {}, displayType: {}",
                vo.getId(), vo.getTitle(), vo.getDisplayType());
    }

    @Override
    public void publishPromotion(BroadcastMessageDTO promotionDTO) {
        if (promotionDTO == null) {
            return;
        }

        BroadcastMessageVO vo = notificationMapper.toBroadcastVO(promotionDTO);
        vo.setId(snowflakeIdGenerator.nextId());
        RealtimeMessageBO messageBO = RealtimeMessageBO.builder()
                .eventId(String.valueOf(vo.getId()))
                .eventType(RealtimeEventType.PROMOTION.getValue())
                .targetUserId(null)
                .payload(vo)
                .timestamp(Instant.now())
                .build();

        publishToRedisChannel(messageBO);
        log.info("[Realtime] 发布运营活动/广告弹窗 -> id: {}, title: {}, linkUrl: {}",
                vo.getId(), vo.getTitle(), vo.getLinkUrl());
    }

    @Override
    public void broadcastToTopic(String topic, String eventName, Object payload) {
        if (topic == null || eventName == null) {
            return;
        }

        RealtimeMessageBO messageBO = RealtimeMessageBO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventName)
                .topic(topic)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        publishToRedisChannel(messageBO);
    }

    @Override
    public void broadcastAll(String eventName, Object payload) {
        if (eventName == null) {
            return;
        }

        RealtimeMessageBO messageBO = RealtimeMessageBO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventName)
                .targetUserId(null)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        publishToRedisChannel(messageBO);
    }

    @Override
    public void sendToUser(Long userId, String eventName, Object payload) {
        if (userId == null || eventName == null) {
            return;
        }

        RealtimeMessageBO messageBO = RealtimeMessageBO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventName)
                .targetUserId(userId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        publishToRedisChannel(messageBO);
    }

    private void publishToRedisChannel(RealtimeMessageBO messageBO) {
        try {
            String json = objectMapper.writeValueAsString(messageBO);
            redisTemplate.convertAndSend(RealtimeRedisConfig.VREDIS_REALTIME_PUBSUB_CHANNEL, json);
        } catch (Exception e) {
            log.error("[Realtime] 序列化实时推送消息失败", e);
        }
    }

}
