package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人机多模态安全验证标准化入参 DTO (CaptchaSDK 核心契约载荷)
 * 收敛字符图形验证码、行为式滑块通行凭证以及高危跃迁下的带外 OTP 动态码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVerifyRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 1. 字符图形验证码唯一 ID (UUID，低/中风控时使用)
     */
    @Size(max = 64, message = "验证码 ID 格式非法")
    @Trimmed
    private String captchaId;

    /**
     * 1. 字符图形验证码用户输入的识别文本 (低/中风控时使用)
     */
    @Size(max = 16, message = "验证码内容长度超出范围")
    @Trimmed
    private String captchaCode;

    /**
     * 2. 行为式滑块拼图/无感验证通过后签发的高信誉临时通行凭证 Ticket (高风控时使用)
     */
    @Size(max = 128, message = "人机验证通行凭证格式非法")
    @Trimmed
    private String captchaTicket;

    /**
     * 3. 极高风控跃迁或敏感操作下的带外一次性动态口令 OTP (邮件动态码 / 短信动态码)
     */
    @Size(max = 16, message = "动态安全码格式非法")
    @Trimmed
    private String otpCode;

    /**
     * 业务上下文场景标识 (可选，若业务调用方上下文已确定则以调用方为准)
     */
    private CaptchaScene scene;

    /**
     * 关联的目标账户标识 (用户名或邮箱，用于账号维度的风控回溯与带外 OTP 寻址)
     */
    @Trimmed(toLowerCase = true)
    private String account;

    /**
     * 4. 设备环境与 Canvas 指纹摘要 (防无头脚本与跨代理池自动化攻击)
     */
    @Size(max = 256, message = "设备指纹格式非法")
    @Trimmed
    private String deviceFingerprint;

    /**
     * 5. 客户端浏览器识别标识 (CHROME, EDGE, SAFARI, FIREFOX, OTHER)
     */
    @Size(max = 32, message = "浏览器标识格式非法")
    private String browserFamily;

    public CaptchaVerifyRequestDTO(String captchaId, String captchaCode, String captchaTicket, String otpCode, CaptchaScene scene, String account) {
        this(captchaId, captchaCode, captchaTicket, otpCode, scene, account, null, null);
    }

}
