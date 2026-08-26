package com.humolix.volur.competition.listener;

import com.alibaba.fastjson2.JSON;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.event.TeamInvitationHandledEvent;
import com.humolix.volur.competition.event.TeamInvitationSentEvent;
import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.bo.UpdateNotificationActionBO;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionNotificationListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleTeamInvitationSent(TeamInvitationSentEvent event) {
        log.info("[Competition-Notification] 监听到组队邀请 -> 竞赛: {}, 队伍: {}, 邀请人: {}, 被邀请人: {}",
                event.getCompetitionTitle(), event.getTeamName(), event.getInviterName(), event.getInviteeId());

        Map<String, Object> actionContext = Map.of(
                "competitionId", event.getCompetitionId(),
                "teamId", event.getTeamId(),
                "teamName", event.getTeamName(),
                "inviterName", event.getInviterName(),
                "memberRole", event.getMemberRole().name()
        );

        String roleName = event.getMemberRole() == MemberRole.LEADER ? "领队" : "队员";
        String remarkText = event.getRemark() != null ? "，附言: 「" + event.getRemark() + "」" : "";
        String content = String.format("用户 %s 邀请您作为【%s】加入战队「%s」共同参加竞赛「%s」%s，请及时确认是否参赛。",
                event.getInviterName(), roleName, event.getTeamName(), event.getCompetitionTitle(), remarkText);

        SendNotificationBO sendBO = SendNotificationBO.builder()
                .senderId(event.getInviterId())
                .receiverId(event.getInviteeId())
                .title("【竞赛邀请】" + event.getInviterName() + " 邀请您加入战队「" + event.getTeamName() + "」")
                .content(content)
                .type(NotificationType.COMPETITION_INVITE)
                .actionStatus(NotificationActionStatus.PENDING)
                .actionData(JSON.toJSONString(actionContext))
                .expireTime(event.getExpireTime())
                .build();

        notificationService.sendNotification(sendBO);
    }

    @Async
    @EventListener
    public void handleTeamInvitationHandled(TeamInvitationHandledEvent event) {
        log.info("[Competition-Notification] 监听到组队邀请处理 -> 队伍ID: {}, 用户: {}, 处理状态: {}",
                event.getTeamId(), event.getMemberUsername(), event.getMemberStatus());

        NotificationActionStatus targetActionStatus = (event.getMemberStatus() == MemberStatus.INVITED_ACCEPTED)
                ? NotificationActionStatus.ACCEPTED
                : NotificationActionStatus.REJECTED;

        // 1. 同步被邀请人站内信卡片的动作状态
        UpdateNotificationActionBO updateBO = UpdateNotificationActionBO.builder()
                .receiverId(event.getMemberUserId())
                .type(NotificationType.COMPETITION_INVITE)
                .actionDataPattern("\"teamId\":" + event.getTeamId())
                .newActionStatus(targetActionStatus)
                .build();

        notificationService.updateActionStatus(updateBO);

        // 2. 若被邀请人做出决策，向邀请人（队长）发送一条反馈通知型站内信
        if (event.getMemberStatus() == MemberStatus.INVITED_ACCEPTED) {
            SendNotificationBO feedbackBO = SendNotificationBO.builder()
                    .senderId(event.getMemberUserId())
                    .receiverId(event.getCaptainId())
                    .title("【组队通知】" + event.getMemberUsername() + " 已同意加入战队")
                    .content(String.format("用户 %s 已同意邀请，成功加入战队「%s」（竞赛: 「%s」）。",
                            event.getMemberUsername(), event.getTeamName(), event.getCompetitionTitle()))
                    .type(NotificationType.COMPETITION_INVITE)
                    .actionStatus(NotificationActionStatus.NONE)
                    .build();

            notificationService.sendNotification(feedbackBO);
        } else if (event.getMemberStatus() == MemberStatus.NOT_PARTICIPATE) {
            SendNotificationBO feedbackBO = SendNotificationBO.builder()
                    .senderId(event.getMemberUserId())
                    .receiverId(event.getCaptainId())
                    .title("【组队通知】" + event.getMemberUsername() + " 拒绝了入队邀请")
                    .content(String.format("用户 %s 拒绝了加入战队「%s」（竞赛: 「%s」）的邀请。",
                            event.getMemberUsername(), event.getTeamName(), event.getCompetitionTitle()))
                    .type(NotificationType.COMPETITION_INVITE)
                    .actionStatus(NotificationActionStatus.NONE)
                    .build();

            notificationService.sendNotification(feedbackBO);
        }
    }

}
