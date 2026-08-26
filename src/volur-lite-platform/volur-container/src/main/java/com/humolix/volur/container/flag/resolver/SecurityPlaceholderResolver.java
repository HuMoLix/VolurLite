package com.humolix.volur.container.flag.resolver;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.humolix.volur.container.flag.DynamicFlagContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 安全摘要、防作弊水印与哈希签名占位符解析器
 */
@Component
@Order(30)
public class SecurityPlaceholderResolver implements FlagPlaceholderResolver {

    private static final Set<String> SUPPORTED = Set.of(
            "MD5",
            "MD5_32",
            "HASH",
            "SHA256",
            "SIGN",
            "HMAC",
            "WATERMARK"
    );

    @Value("${volur.jwt.secret:VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!}")
    private String systemSecret;

    @Override
    public boolean supports(String placeholder) {
        if (placeholder == null) {
            return false;
        }
        return SUPPORTED.contains(placeholder.toUpperCase());
    }

    @Override
    public String resolve(String placeholder, DynamicFlagContext context) {
        String upper = placeholder.toUpperCase();

        String userIdStr = context != null && context.getUserId() != null ? String.valueOf(context.getUserId()) : "0";
        String challengeIdStr = context != null && context.getChallengeId() != null ? String.valueOf(context.getChallengeId()) : "0";
        String teamIdStr = context != null && context.getTeamId() != null ? String.valueOf(context.getTeamId()) : "0";
        String randomSeed = IdUtil.fastSimpleUUID();

        String payload = userIdStr + ":" + challengeIdStr + ":" + teamIdStr + ":" + randomSeed;

        return switch (upper) {
            case "MD5", "HASH" -> SecureUtil.md5(payload).substring(0, 16);
            case "MD5_32" -> SecureUtil.md5(payload);
            case "SHA256" -> SecureUtil.sha256(payload).substring(0, 32);
            case "SIGN", "HMAC", "WATERMARK" -> {
                // HMAC-MD5 防作弊水印：结合系统密钥与用户/题目标识，生成 12 位可溯源水印
                String hmac = SecureUtil.hmacMd5(systemSecret).digestHex(userIdStr + ":" + challengeIdStr + ":" + teamIdStr);
                yield hmac.substring(0, 12);
            }
            default -> SecureUtil.md5(payload).substring(0, 16);
        };
    }

}
