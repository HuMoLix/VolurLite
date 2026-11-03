package com.humolix.volur.verification.sdk;

import com.humolix.volur.verification.domain.dto.CaptchaGenerateRequestDTO;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
import com.humolix.volur.verification.domain.vo.CaptchaChallengeDecisionVO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;

/**
 * 统一验证码与风控多模态核验 SDK 契约 (CaptchaSDK)
 * 对外屏蔽字符图形码、行为式滑块拼图凭证、带外邮件 OTP 细节，对内整合风控引擎、人机验证与邮件投递
 */
public interface CaptchaSDK {

    /**
     * 基于前端加密场景凭据生成自适应人机验证挑战 (POST /api/v1/captcha)
     *
     * @param request 包含加密 sceneId 与账号的请求入参 DTO
     * @return 精简人机验证码响应视图 (仅包含 captchaId, captchaType, imageBase64 等)
     */
    CaptchaResponseVO generateCaptchaWithEncryptedScene(CaptchaGenerateRequestDTO request);

    /**
     * 核心核销入口：自适应多模态验证（包含图形码、滑块凭证、邮件 OTP 原子核验）
     *
     * @param request 标准化验证码参数 DTO
     * @param account 关联账号（用户名或邮箱，用于账号级风控与带外 OTP 校验）
     * @param scene   业务场景
     */
    void verify(CaptchaVerifyRequestDTO request, String account, CaptchaScene scene);

    /**
     * 简易核销入口（无特定账号上下文时使用，如匿名公开接口限流）
     *
     * @param request 标准化验证码参数 DTO
     * @param scene   业务场景
     */
    void verify(CaptchaVerifyRequestDTO request, CaptchaScene scene);

    /**
     * 评估并探测当前请求所需的挑战模式与风控等级
     *
     * @param account 关联账号
     * @param scene   业务场景
     * @return 包含模式 (NONE, IMAGE, SLIDER, EMAIL_OTP) 及图形码等预置信息的决策视图
     */
    CaptchaChallengeDecisionVO evaluateChallenge(String account, CaptchaScene scene);

    /**
     * 针对极高风控账号发送带外登录安全动态码 (OTP)
     *
     * @param account       关联账号
     * @param captchaTicket 行为式滑块拼图通行凭证（防邮件轰炸与无门槛滥用）
     */
    void sendLoginOtp(String account, String captchaTicket);

    /**
     * 业务操作成功回调：削减该 IP 及账号在风控滑动窗口中的失败惩罚
     *
     * @param account 关联账号
     * @param scene   业务场景
     */
    void recordSuccess(String account, CaptchaScene scene);

    /**
     * 业务操作失败回调（如密码错误）：在滑动窗口中累加失败计数并自适应跃迁
     *
     * @param account 关联账号
     * @param scene   业务场景
     */
    void recordFailure(String account, CaptchaScene scene);

}
