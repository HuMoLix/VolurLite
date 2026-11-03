package com.humolix.volur.competition.domain.vo.chat;

import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.domain.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO implements Serializable {

    private Long id;

    private Long competitionId;

    private ChatChannelType channelType;

    private Long teamId;

    private String teamName;

    private Long senderId;

    private String senderName;

    private String senderRole;

    private String senderAvatar;

    private Long receiverId;

    private String receiverName;

    private String mentionUserIds;

    private String content;

    @Builder.Default
    private ChatMessageType msgType = ChatMessageType.TEXT;

    private Instant createdAt;

}
