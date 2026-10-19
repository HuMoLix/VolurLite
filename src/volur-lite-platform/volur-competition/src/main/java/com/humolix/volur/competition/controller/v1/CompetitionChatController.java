package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.dto.chat.ChatMessagePageQueryDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatMessageSendDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatReadDTO;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import com.humolix.volur.competition.domain.vo.chat.ChatUnreadCountVO;
import com.humolix.volur.competition.domain.vo.chat.RefereeTeamSummaryVO;
import com.humolix.volur.competition.service.CompetitionChatService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.CursorResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/chat")
@RequiredArgsConstructor
public class CompetitionChatController {

    private final CompetitionChatService chatService;

    @GetMapping("/unread")
    public ApiResponseEntity<ChatUnreadCountVO> getUnreadCount(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取未读消息统计成功", chatService.getUnreadCount(competitionId, userId));
    }

    @PostMapping("/read")
    public ApiResponseEntity<Void> markRead(
            @PathVariable Long competitionId,
            @Valid @RequestBody ChatReadDTO dto,
            @CurrentUserId Long userId) {
        chatService.markRead(competitionId, userId, dto);
        return ApiResponseEntity.success("标记已读成功");
    }

    @GetMapping("/history")
    public ApiResponseEntity<CursorResponse<ChatMessageVO>> getHistory(
            @PathVariable Long competitionId,
            @ModelAttribute ChatMessagePageQueryDTO query,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取历史聊天记录成功", chatService.getHistoryMessages(competitionId, query, userId));
    }

    @GetMapping("/referee/teams")
    public ApiResponseEntity<List<RefereeTeamSummaryVO>> getRefereeTeamSummaries(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取申诉队伍概览成功", chatService.getRefereeTeamSummaries(competitionId, userId));
    }

    @PostMapping("/send")
    public ApiResponseEntity<ChatMessageVO> sendMessage(
            @PathVariable Long competitionId,
            @Valid @RequestBody ChatMessageSendDTO dto,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("消息发送成功", chatService.sendMessage(competitionId, userId, dto));
    }

}
