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
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "竞赛实时聊天与申诉沟通", description = "同队队员实时协同交流与裁判双向答疑申诉接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/chat")
@RequiredArgsConstructor
public class CompetitionChatController {

    private final CompetitionChatService chatService;

    @Operation(summary = "获取当前竞赛各频道未读消息统计（刷新页面轻量获取）")
    @GetMapping("/unread")
    public ApiResponse<ChatUnreadCountVO> getUnreadCount(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取未读消息统计成功", chatService.getUnreadCount(competitionId, userId));
    }

    @Operation(summary = "标记指定频道消息为已读")
    @PostMapping("/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long competitionId,
            @Valid @RequestBody ChatReadDTO dto,
            @CurrentUserId Long userId) {
        chatService.markRead(competitionId, userId, dto);
        return ApiResponseEntity.success("标记已读成功");
    }

    @Operation(summary = "获取历史聊天与沟通记录")
    @GetMapping("/history")
    public ApiResponse<PageResponse<ChatMessageVO>> getHistory(
            @PathVariable Long competitionId,
            @ModelAttribute ChatMessagePageQueryDTO query,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取历史聊天记录成功", chatService.getHistoryMessages(competitionId, query, userId));
    }

    @Operation(summary = "裁判端获取全部参赛队伍工单列表")
    @GetMapping("/referee/teams")
    public ApiResponse<List<RefereeTeamSummaryVO>> getRefereeTeamSummaries(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取申诉队伍概览成功", chatService.getRefereeTeamSummaries(competitionId, userId));
    }

    @Operation(summary = "发送聊天消息 (HTTP 降级/补充通道)")
    @PostMapping("/send")
    public ApiResponse<ChatMessageVO> sendMessage(
            @PathVariable Long competitionId,
            @Valid @RequestBody ChatMessageSendDTO dto,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("消息发送成功", chatService.sendMessage(competitionId, userId, dto));
    }

}
