package com.humolix.volur.competition.domain.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUnreadCountVO {

    /**
     * 战队公屏未读消息数
     */
    @Builder.Default
    private long teamUnreadCount = 0L;

    /**
     * 裁判答疑/申诉未读消息数
     */
    @Builder.Default
    private long refereeUnreadCount = 0L;

    /**
     * 点对点私信未读消息总数
     */
    @Builder.Default
    private long privateTotalUnreadCount = 0L;

    /**
     * 所有频道未读消息总数（战队 + 私信 + 裁判）
     */
    @Builder.Default
    private long totalUnreadCount = 0L;

    /**
     * 各私信对象的未读数分布（Key: "USER_{userId}", Value: 未读数）
     */
    @Builder.Default
    private Map<String, Long> privateDetails = new HashMap<>();

}
