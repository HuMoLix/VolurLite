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
@Schema(description = "管理端容器交互终端会话详情视图")
public class AdminContainerTerminalInfoVO {

    @Schema(description = "容器是否就绪可执行命令")
    private Boolean ready;

    @Schema(description = "实际运行容器短 ID (例如 c0afab5eb70b)")
    private String containerShortId;

    @Schema(description = "容器内当前登录用户名 (例如 ctf 或 root)")
    private String currentUser;

    @Schema(description = "容器当前工作目录 (例如 /app)")
    private String workingDir;

    @Schema(description = "终端交互提示符 (例如 ctf@c0afab5eb70b:/app$)")
    private String prompt;

    @Schema(description = "底层调度任务当前状态 (例如 Running, Preparing)")
    private String taskState;

    @Schema(description = "就绪状态提示说明")
    private String message;

}
