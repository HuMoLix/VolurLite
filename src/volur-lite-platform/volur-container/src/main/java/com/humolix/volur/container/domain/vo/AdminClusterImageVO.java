package com.humolix.volur.container.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群 Docker 镜像详情视图 VO")
public class AdminClusterImageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "镜像 ID")
    private Long id;

    @Schema(description = "镜像仓库名")
    private String repository;

    @Schema(description = "镜像标签 Tag")
    private String tag;

    @Schema(description = "完整镜像标识 (repository:tag)")
    private String fullImageName;

    @Schema(description = "镜像摘要 ID (sha256:...)")
    private String imageId;

    @Schema(description = "镜像体积(字节)")
    private Long sizeBytes;

    @Schema(description = "格式化体积字符串 (如 128.5 MB)")
    private String sizeFormatted;

    @Schema(description = "镜像类型 (CHALLENGE / BASE / SYSTEM)")
    private String imageType;

    @Schema(description = "已下载缓存该镜像的节点 IP 列表")
    private List<String> nodeIps;

    @Schema(description = "在线就绪的节点 IP 列表")
    private List<String> onlineNodeIps;

    @Schema(description = "离线/脱机的节点 IP 列表")
    private List<String> offlineNodeIps;

    @Schema(description = "镜像是否在集群中处于离线状态 (所有节点均不可用)")
    private Boolean isOffline;

    @Schema(description = "镜像当前状态 (PULLED / PULLING / FAILED / MIGRATING / OFFLINE)")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "首次拉取入库时间")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近一次更新或同步时间")
    private Instant updatedAt;

}
