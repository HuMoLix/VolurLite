package com.humolix.volur.competition.domain.dto.chat;

import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.domain.enums.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSendDTO implements Serializable {

    @NotNull(message = "频道类型不能为空")
    private ChatChannelType channelType;

    /**
     * 目标队伍ID（裁判对特定队伍回复工单时必填；普通选手发送时自动绑定本队）
     */
    private Long teamId;

    /**
     * 私信接收者用户ID（仅 PRIVATE 私信时生效）
     */
    private Long receiverId;

    /**
     * 队伍公屏被 @ 提及的队员用户ID列表
     */
    private java.util.List<Long> mentionUserIds;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息内容不能超过 4000 字符")
    private String content;

    private ChatMessageType msgType = ChatMessageType.TEXT;

}
