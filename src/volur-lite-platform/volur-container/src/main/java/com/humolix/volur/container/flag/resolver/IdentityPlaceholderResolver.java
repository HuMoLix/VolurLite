package com.humolix.volur.container.flag.resolver;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.humolix.volur.container.flag.DynamicFlagContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 用户、队伍、赛题与阶段等实体身份标识占位符解析器
 */
@Component
@Order(10)
public class IdentityPlaceholderResolver implements FlagPlaceholderResolver {

    private static final Set<String> SUPPORTED = Set.of(
            "USER_ID",
            "USERNAME",
            "CHALLENGE_ID",
            "CHALLENGE_KEY",
            "COMPETITION_ID",
            "STAGE_ID",
            "TEAM_ID",
            "TEAM_TOKEN"
    );

    @Override
    public boolean supports(String placeholder) {
        if (placeholder == null) {
            return false;
        }
        return SUPPORTED.contains(placeholder.toUpperCase()) || placeholder.startsWith("ATTR:");
    }

    @Override
    public String resolve(String placeholder, DynamicFlagContext context) {
        if (context == null) {
            return "0";
        }

        String key = placeholder.toUpperCase();
        return switch (key) {
            case "USER_ID" -> context.getUserId() != null ? String.valueOf(context.getUserId()) : "0";
            case "USERNAME" -> StringUtils.hasText(context.getUsername()) ? context.getUsername() : "player";
            case "CHALLENGE_ID" -> context.getChallengeId() != null ? String.valueOf(context.getChallengeId()) : "0";
            case "CHALLENGE_KEY" -> StringUtils.hasText(context.getChallengeKey()) ? context.getChallengeKey() : "chal";
            case "COMPETITION_ID" -> context.getCompetitionId() != null ? String.valueOf(context.getCompetitionId()) : "0";
            case "STAGE_ID" -> context.getStageId() != null ? String.valueOf(context.getStageId()) : "0";
            case "TEAM_ID" -> context.getTeamId() != null ? String.valueOf(context.getTeamId()) : "0";
            case "TEAM_TOKEN" -> resolveTeamToken(context);
            default -> {
                if (placeholder.startsWith("ATTR:") || placeholder.startsWith("attr:")) {
                    String attrKey = placeholder.substring(5);
                    Object val = context.getAttribute(attrKey);
                    yield val != null ? String.valueOf(val) : "";
                }
                yield "";
            }
        };
    }

    private String resolveTeamToken(DynamicFlagContext context) {
        if (StringUtils.hasText(context.getTeamToken())) {
            return context.getTeamToken();
        }
        if (context.getTeamId() != null && context.getTeamId() > 0) {
            return SecureUtil.md5("volur_team_" + context.getTeamId()).substring(0, 16);
        }
        return RandomUtil.randomString(16);
    }

}
