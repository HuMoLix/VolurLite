package com.humolix.volur.competition.domain.entity;

import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.domain.enums.ChatMessageType;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_chat_message", indexes = {
        @Index(name = "idx_chat_comp_team_chan", columnList = "competition_id, team_id, channel_type, id"),
        @Index(name = "idx_chat_comp_priv", columnList = "competition_id, channel_type, sender_id, receiver_id"),
        @Index(name = "idx_chat_created_at", columnList = "created_at")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionChatMessage implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private ChatChannelType channelType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CompetitionTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private User sender;

    @Column(name = "sender_name", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String senderName;

    @Column(name = "sender_role", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private String senderRole;

    @Column(name = "sender_avatar", length = 255, columnDefinition = "VARCHAR(255)")
    private String senderAvatar;

    /**
     * 私信接收者用户ID（仅 PRIVATE 私信时必填，公共频道为 null）
     */
    @Column(name = "receiver_id", columnDefinition = "BIGINT")
    private Long receiverId;

    /**
     * 私信接收者用户名
     */
    @Column(name = "receiver_name", length = 64, columnDefinition = "VARCHAR(64)")
    private String receiverName;

    /**
     * 队伍公屏中被 @ 提及的队员用户 ID 集合（英文逗号隔开）
     */
    @Column(name = "mention_user_ids", length = 255, columnDefinition = "VARCHAR(255)")
    private String mentionUserIds;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "msg_type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private ChatMessageType msgType = ChatMessageType.TEXT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

}
