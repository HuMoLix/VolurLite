package com.humolix.volur.competition.domain.dto.chat;

import com.humolix.volur.competition.domain.enums.ChatChannelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePageQueryDTO implements Serializable {

    private ChatChannelType channelType = ChatChannelType.TEAM;

    /**
     * 指定队伍ID（仅裁判查询特定队伍申诉工单历史时生效；选手查询自动限定为本人队伍）
     */
    private Long teamId;

    /**
     * 私信目标用户ID（当 channelType == PRIVATE 时必填）
     */
    private Long targetUserId;

    /**
     * 游标 ID，用于向上拉取更早历史记录
     */
    private Long beforeId;

    private Integer page = 0;

    private Integer size = 50;

}
