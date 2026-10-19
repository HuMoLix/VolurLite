package com.humolix.volur.competition.websocket.model;

import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionChatBroadcastMessage implements Serializable {

    private Long competitionId;

    private ChatChannelType channelType;

    /**
     * 所属/目标队伍ID
     */
    private Long teamId;

    private Long senderId;

    private Long receiverId;

    /**
     * 完整消息视图
     */
    private ChatMessageVO payload;

}
