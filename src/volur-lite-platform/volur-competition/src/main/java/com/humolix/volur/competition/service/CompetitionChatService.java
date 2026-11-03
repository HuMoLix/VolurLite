package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.chat.ChatMessagePageQueryDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatMessageSendDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatReadDTO;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import com.humolix.volur.competition.domain.vo.chat.ChatUnreadCountVO;
import com.humolix.volur.competition.domain.vo.chat.RefereeTeamSummaryVO;
import com.humolix.volur.framework.common.api.CursorResponse;

import java.util.List;

public interface CompetitionChatService {

    /**
     * 发送聊天消息并广播至 Redis / WebSocket
     */
    ChatMessageVO sendMessage(Long competitionId, Long userId, ChatMessageSendDTO dto);

    /**
     * 游标流式拉取历史聊天记录（访问即自动将该频道标记已读）
     */
    CursorResponse<ChatMessageVO> getHistoryMessages(Long competitionId, ChatMessagePageQueryDTO query, Long userId);

    /**
     * 裁判获取有申诉沟通记录的队伍列表概览
     */
    List<RefereeTeamSummaryVO> getRefereeTeamSummaries(Long competitionId, Long userId);

    /**
     * 客户端标记已读状态
     */
    void markRead(Long competitionId, Long userId, ChatReadDTO dto);

    /**
     * 获取用户在当前比赛各频道的未读消息统计
     */
    ChatUnreadCountVO getUnreadCount(Long competitionId, Long userId);

}
