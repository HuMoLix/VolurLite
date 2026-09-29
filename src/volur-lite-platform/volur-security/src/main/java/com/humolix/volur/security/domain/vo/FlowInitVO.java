package com.humolix.volur.security.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * VLIF (Volur Login Interface Framework) 流程会话初始化响应视图 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "VLIF 流程会话初始化响应视图")
public class FlowInitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "后端签发的分布式会话状态句柄 (dsh)", example = "S-969965009:1790527266429740")
    private String dsh;

    @Builder.Default
    @Schema(description = "状态机大模块流名称", example = "VlifWebLogin")
    private String flowName = "VlifWebLogin";

    @Builder.Default
    @Schema(description = "大模块下的具体功能入口标识", example = "WebServiceLogin")
    private String flowEntry = "WebServiceLogin";

    @Builder.Default
    @Schema(description = "初始步骤序号 (cid)", example = "0")
    private Integer cid = 0;

    @Schema(description = "客户端指纹 md5 摘要 (fp)", example = "e10adc3949ba59abbe56e057f20f883e")
    private String fp;

    @Schema(description = "服务端响应时间戳 (毫秒)", example = "1790527266429")
    private Long serverTime;

}
