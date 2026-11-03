package com.humolix.volur.verification.service;

import java.time.Duration;

/**
 * 邮件动态验证码生命周期管理服务 (ISP 隔离接口)
 * <p>
 * 职责：专注负责验证码的随机生成、Redis 暂存与原子核销，无任何业务邮件模板与人机安全校验依赖。
 * </p>
 */
public interface EmailVerificationCodeService {

    /**
     * 生成并存储默认 5 分钟有效期的 6 位数字验证码
     *
     * @param email 目标邮箱
     * @return 生成的验证码
     */
    String generateAndStoreCode(String email);

    /**
     * 存储指定验证码与过期时间
     *
     * @param email    目标邮箱
     * @param code     验证码内容
     * @param duration 有效时长
     */
    void storeCode(String email, String code, Duration duration);

    /**
     * 原子核销验证码 (核验成功后单次使用即销毁)
     *
     * @param email 目标邮箱
     * @param code  待核验验证码
     * @return 是否核验匹配通过
     */
    boolean verifyCode(String email, String code);

    /**
     * 获取当前邮箱存储的验证码 (供受信任内部逻辑检索，不销毁)
     *
     * @param email 目标邮箱
     * @return 存储的验证码，若无则返回 null
     */
    String getStoredCode(String email);

}
