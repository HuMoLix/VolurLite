package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段化登录第一阶段：账号标识提交请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录第一阶段账号标识请求")
public class SignInIdentifierRequestDTO {

    @NotBlank(message = "登录账号或邮箱不能为空")
    @Schema(description = "用户账号或邮箱", example = "user@example.com")
    @Trimmed
    private String identifier;

    @NotBlank
    @Schema(description = "分布式会话状态句柄", example = "S-969965009:1790527266429740")
    private String dsh;

    @Schema(description = "步骤序号 (cid)，账号阶段初始为 0", example = "0")
    private Integer cid = 0;

    @Schema(description = "客户端设备或浏览器指纹信息 (用于风控评估)")
    private String deviceFingerprint;

}
