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
@Schema(description = "管理端容器集群连通性测试结果视图")
public class AdminContainerTestConnectionVO {

    @Builder.Default
    @Schema(description = "是否连通成功")
    private boolean connected = false;

    @Schema(description = "诊断或提示信息")
    private String message;

    @Schema(description = "容器编排集群版本")
    private String clusterVersion;

    @Builder.Default
    @Schema(description = "活跃节点数")
    private Integer nodeCount = 0;

    @Schema(description = "通信延迟（毫秒）")
    private Long latencyMs;

}
