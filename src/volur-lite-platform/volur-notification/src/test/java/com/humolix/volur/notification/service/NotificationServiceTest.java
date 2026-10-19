package com.humolix.volur.notification.service;

import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.entity.Notification;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.domain.vo.NotificationVO;
import com.humolix.volur.notification.mapper.NotificationMapper;
import com.humolix.volur.notification.repository.NotificationRepository;
import com.humolix.volur.notification.service.impl.NotificationServiceImpl;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RealtimePushService realtimePushService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("内部API重载测试：sendNotification(from, to, type, content) 自动推导默认标题")
    void testSendNotification_overloadWithDefaultTitle() {
        Long senderId = 1L;
        Long receiverId = 2L;
        NotificationType type = NotificationType.COMPETITION_AUDIT;
        String content = "战队已通过审核";

        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Notification entity = Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .title("【战队审核】")
                .content(content)
                .status(NotificationStatus.UNREAD)
                .actionStatus(NotificationActionStatus.NONE)
                .build();

        NotificationVO vo = NotificationVO.builder()
                .id(100L)
                .senderId(senderId)
                .receiverId(receiverId)
                .title("【战队审核】")
                .content(content)
                .build();

        when(notificationMapper.toEntity(any(SendNotificationBO.class))).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(notificationMapper.toVO(any(Notification.class))).thenReturn(vo);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(User.builder().id(senderId).username("sender").build()));

        notificationService.sendNotification(senderId, receiverId, type, content);

        ArgumentCaptor<SendNotificationBO> boCaptor = ArgumentCaptor.forClass(SendNotificationBO.class);
        verify(notificationMapper).toEntity(boCaptor.capture());

        SendNotificationBO captured = boCaptor.getValue();
        assertEquals(senderId, captured.getSenderId());
        assertEquals(receiverId, captured.getReceiverId());
        assertEquals(type, captured.getType());
        assertEquals("【战队审核】", captured.getTitle());
        assertEquals(content, captured.getContent());
        verify(realtimePushService).pushNotification(eq(receiverId), any(NotificationVO.class));
    }

    @Test
    @DisplayName("内部API重载测试：updateActionStatus 便捷方法成功更新状态")
    void testUpdateActionStatus_convenienceMethod() {
        Long receiverId = 2L;
        NotificationType type = NotificationType.COMPETITION_INVITE;
        String pattern = "\"teamId\":123";

        Notification notification = Notification.builder()
                .id(10L)
                .receiverId(receiverId)
                .type(type)
                .actionData("{\"teamId\":123}")
                .actionStatus(NotificationActionStatus.PENDING)
                .build();

        when(notificationRepository.findByReceiverIdAndTypeAndActionDataContaining(receiverId, type, pattern))
                .thenReturn(List.of(notification));

        notificationService.updateActionStatus(receiverId, type, pattern, NotificationActionStatus.ACCEPTED);

        assertEquals(NotificationActionStatus.ACCEPTED, notification.getActionStatus());
        verify(notificationRepository).saveAll(List.of(notification));
    }

}
