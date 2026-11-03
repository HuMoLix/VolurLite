package com.humolix.volur.notification.service.impl;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.bo.UpdateNotificationActionBO;
import com.humolix.volur.notification.domain.dto.NotificationQueryDTO;
import com.humolix.volur.notification.domain.entity.Notification;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.domain.vo.NotificationVO;
import com.humolix.volur.notification.mapper.NotificationMapper;
import com.humolix.volur.notification.repository.NotificationRepository;
import com.humolix.volur.notification.service.NotificationService;
import com.humolix.volur.notification.service.RealtimePushService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String VREDIS_NOTIFICATION_UNREAD_COUNT_KEY_PREFIX = "vredis:notification:unread_count:";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate redisTemplate;
    private final RealtimePushService realtimePushService;

    @Override
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String cacheKey = VREDIS_NOTIFICATION_UNREAD_COUNT_KEY_PREFIX + userId;
        String cachedCount = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            try {
                return Long.parseLong(cachedCount);
            } catch (NumberFormatException ignored) {
            }
        }
        long count = notificationRepository.countByReceiverIdAndStatus(userId, NotificationStatus.UNREAD);
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(count), Duration.ofMinutes(10));
        return count;
    }

    @Override
    public PageResponse<NotificationVO> listNotifications(Long userId, NotificationQueryDTO queryDTO) {
        Pageable pageable = SortUtils.createPageable(
                queryDTO.getPage(),
                queryDTO.getSize(),
                Notification.class,
                null,
                null,
                "createdAt",
                Sort.Direction.DESC
        );

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("receiverId"), userId));

            if (StringUtils.hasText(queryDTO.getKeyword())) {
                String pattern = "%" + queryDTO.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                ));
            }
            if (queryDTO.getType() != null) {
                predicates.add(cb.equal(root.get("type"), queryDTO.getType()));
            }
            if (queryDTO.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), queryDTO.getStatus()));
            }
            if (queryDTO.getActionStatus() != null) {
                predicates.add(cb.equal(root.get("actionStatus"), queryDTO.getActionStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Notification> entityPage = notificationRepository.findAll(spec, pageable);
        Map<Long, String> senderNames = resolveSenderUsernames(entityPage.getContent());

        return PageResponse.of(entityPage.map(entity -> {
            NotificationVO vo = notificationMapper.toVO(entity);
            if (entity.getSenderId() != null) {
                vo.setSenderUsername(senderNames.getOrDefault(entity.getSenderId(), "未知用户"));
            } else {
                vo.setSenderUsername("系统通知");
            }
            return vo;
        }));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationVO getNotificationDetail(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "站内信不存在或无权查看"));

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadTime(Instant.now());
            notificationRepository.save(notification);
            evictOrDecrementUnreadCount(userId);
        }

        NotificationVO vo = notificationMapper.toVO(notification);
        if (notification.getSenderId() != null) {
            userRepository.findById(notification.getSenderId())
                    .ifPresent(sender -> vo.setSenderUsername(sender.getUsername()));
        } else {
            vo.setSenderUsername("系统通知");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByReceiverId(userId, Instant.now());
        String cacheKey = VREDIS_NOTIFICATION_UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(cacheKey, "0", Duration.ofMinutes(10));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "站内信不存在"));

        boolean wasUnread = notification.getStatus() == NotificationStatus.UNREAD;
        notificationRepository.delete(notification);
        if (wasUnread) {
            evictOrDecrementUnreadCount(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(SendNotificationBO sendBO) {
        if (sendBO.getReceiverId() == null || !StringUtils.hasText(sendBO.getTitle()) || !StringUtils.hasText(sendBO.getContent())) {
            log.warn("[Notification] 发送站内信失败，缺少必要参数 -> {}", sendBO);
            return;
        }

        Notification notification = notificationMapper.toEntity(sendBO);
        if (notification.getStatus() == null) {
            notification.setStatus(NotificationStatus.UNREAD);
        }
        if (notification.getActionStatus() == null) {
            notification.setActionStatus(NotificationActionStatus.NONE);
        }

        Notification saved = notificationRepository.save(notification);
        incrementUnreadCount(sendBO.getReceiverId());

        NotificationVO vo = notificationMapper.toVO(saved);
        if (saved.getSenderId() != null) {
            userRepository.findById(saved.getSenderId())
                    .ifPresent(sender -> vo.setSenderUsername(sender.getUsername()));
        } else {
            vo.setSenderUsername("系统通知");
        }
        realtimePushService.pushNotification(sendBO.getReceiverId(), vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long senderId, Long receiverId, NotificationType type, String content) {
        String defaultTitle = "【" + (type != null ? type.getDescription() : "系统通知") + "】";
        sendNotification(senderId, receiverId, type, defaultTitle, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content) {
        sendNotification(senderId, receiverId, type, title, content, NotificationActionStatus.NONE, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content, String actionData, Instant expireTime) {
        sendNotification(senderId, receiverId, type, title, content, NotificationActionStatus.NONE, actionData, expireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content, NotificationActionStatus actionStatus, String actionData, Instant expireTime) {
        SendNotificationBO bo = SendNotificationBO.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .title(title)
                .content(content)
                .actionStatus(actionStatus != null ? actionStatus : NotificationActionStatus.NONE)
                .actionData(actionData)
                .expireTime(expireTime)
                .build();
        sendNotification(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActionStatus(UpdateNotificationActionBO updateBO) {
        if (updateBO.getReceiverId() == null || updateBO.getType() == null || !StringUtils.hasText(updateBO.getActionDataPattern())) {
            return;
        }

        List<Notification> notifications = notificationRepository.findByReceiverIdAndTypeAndActionDataContaining(
                updateBO.getReceiverId(),
                updateBO.getType(),
                updateBO.getActionDataPattern()
        );

        for (Notification n : notifications) {
            n.setActionStatus(updateBO.getNewActionStatus());
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActionStatus(Long receiverId, NotificationType type, String actionDataPattern, NotificationActionStatus newActionStatus) {
        UpdateNotificationActionBO bo = UpdateNotificationActionBO.builder()
                .receiverId(receiverId)
                .type(type)
                .actionDataPattern(actionDataPattern)
                .newActionStatus(newActionStatus)
                .build();
        updateActionStatus(bo);
    }

    private Map<Long, String> resolveSenderUsernames(List<Notification> notifications) {
        Set<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (senderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (u1, u2) -> u1));
    }

    private void incrementUnreadCount(Long userId) {
        String cacheKey = VREDIS_NOTIFICATION_UNREAD_COUNT_KEY_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            redisTemplate.opsForValue().increment(cacheKey);
        } else {
            getUnreadCount(userId);
        }
    }

    private void evictOrDecrementUnreadCount(Long userId) {
        String cacheKey = VREDIS_NOTIFICATION_UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }

}
