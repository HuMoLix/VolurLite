package com.humolix.volur.competition.domain.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadDTO {

    /**
     * 沟通目标 ("TEAM" | "REFEREE" | "USER_{userId}")
     */
    @NotBlank(message = "已读目标不能为空")
    private String target;

    /**
     * 该目标下用户阅读到的最大消息 ID（雪花 ID 单调递增）
     */
    private Long lastMessageId;

}
