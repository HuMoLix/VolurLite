package com.humolix.volur.container.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端容器命令执行响应结果视图")
public class AdminContainerExecResultVO {

    @Schema(description = "标准输出 (stdout)")
    private String output;

    @Schema(description = "错误输出 (stderr)")
    private String errorOutput;

    @Builder.Default
    @Schema(description = "进程退出码")
    private Integer exitCode = 0;

    @Schema(description = "命令执行后工作目录")
    private String workingDir;

    @Builder.Default
    @Schema(description = "耗时（毫秒）")
    private Long executionTimeMs = 0L;

    @Schema(description = "实际运行容器短 ID (例如 c0afab5eb70b)")
    private String containerShortId;

    @Schema(description = "当前容器内执行用户 (例如 ctf 或 root)")
    private String currentUser;

    @Schema(description = "终端交互提示符 (例如 ctf@c0afab5eb70b:/app$)")
    private String prompt;

}
