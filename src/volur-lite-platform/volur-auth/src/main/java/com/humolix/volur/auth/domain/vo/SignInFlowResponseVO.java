package com.humolix.volur.auth.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 阶段化认证流转决策与响应结果视图 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "阶段化认证流转决策响应")
public class SignInFlowResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分布式会话句柄 (dsh)", example = "S-969965009:1790527266429740")
    private String dsh;

    @Schema(description = "后端签发的下一步防跳步流转锁 (TL)", example = "ADG-GRTbPbNl0pSndZCtatZwpc273Y1KxgoCFYDnvVdrHspzQ9BKk03FfMrdB24l")
    private String tl;

    @Schema(description = "更新后的步骤序号 (cid)", example = "1")
    private Integer cid;

    @Schema(description = "下一步应进行的阶段或挑战类型: pwd(密码), recaptcha(人机), 2fa(二次验证), device(设备确认), completed(完成)", example = "pwd")
    private String nextStep;

    @Schema(description = "当前流转状态: CHALLENGE(需继续挑战), SUCCESS(全部认证通过)", example = "CHALLENGE")
    private String status;

    @Schema(description = "已绑定的账号标识", example = "user@example.com")
    private String identifier;

    @Schema(description = "认证完成时的访问令牌 Access Token (status=SUCCESS 时返回)")
    private String accessToken;

    @Schema(description = "认证完成时的刷新令牌 Refresh Token (status=SUCCESS 时返回)")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

}
