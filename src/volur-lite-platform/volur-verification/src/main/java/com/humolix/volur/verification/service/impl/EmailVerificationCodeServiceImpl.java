package com.humolix.volur.verification.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.verification.service.EmailVerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 邮件动态验证码生命周期管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationCodeServiceImpl implements EmailVerificationCodeService {

    public static final String VREDIS_EMAIL_CODE_KEY_PREFIX = "vredis:verification:email_code:";
    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    @Override
    public String generateAndStoreCode(String email) {
        String code = RandomUtil.randomNumbers(6);
        storeCode(email, code, DEFAULT_EXPIRATION);
        log.info("[Volur-Verification] 生成并存储邮件验证码 -> 邮箱: {}, 验证码: {}", email, code);
        return code;
    }

    @Override
    public void storeCode(String email, String code, Duration duration) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return;
        }
        String key = VREDIS_EMAIL_CODE_KEY_PREFIX + email.toLowerCase().trim();
        Duration ttl = duration != null ? duration : DEFAULT_EXPIRATION;
        redisTemplate.opsForValue().set(key, code.trim(), ttl);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return false;
        }
        String key = VREDIS_EMAIL_CODE_KEY_PREFIX + email.toLowerCase().trim();
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            return false;
        }
        boolean matches = storedCode.equalsIgnoreCase(code.trim());
        if (matches) {
            redisTemplate.delete(key);
            log.info("[Volur-Verification] 邮箱验证码原子核验成功并销毁 -> 邮箱: {}", email);
        }
        return matches;
    }

    @Override
    public String getStoredCode(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String key = VREDIS_EMAIL_CODE_KEY_PREFIX + email.toLowerCase().trim();
        return redisTemplate.opsForValue().get(key);
    }

}
