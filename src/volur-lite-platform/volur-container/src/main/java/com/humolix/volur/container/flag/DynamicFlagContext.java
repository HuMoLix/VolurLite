package com.humolix.volur.container.flag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态 Flag 生成多维上下文参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFlagContext implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 题目 ID
     */
    private Long challengeId;

    /**
     * 题目唯一标识 Key（如适用）
     */
    private String challengeKey;

    /**
     * 赛事 ID（如适用）
     */
    private Long competitionId;

    /**
     * 阶段 ID（如适用）
     */
    private Long stageId;

    /**
     * 队伍 ID（如适用）
     */
    private Long teamId;

    /**
     * 队伍 Token 或邀请码（如适用）
     */
    private String teamToken;

    /**
     * 业务扩展参数字典（支持任意自定义占位符注入）
     */
    @Builder.Default
    private Map<String, Object> extraAttributes = new HashMap<>();

    public DynamicFlagContext addAttribute(String key, Object value) {
        if (this.extraAttributes == null) {
            this.extraAttributes = new HashMap<>();
        }
        this.extraAttributes.put(key, value);
        return this;
    }

    public Object getAttribute(String key) {
        return this.extraAttributes != null ? this.extraAttributes.get(key) : null;
    }

}
