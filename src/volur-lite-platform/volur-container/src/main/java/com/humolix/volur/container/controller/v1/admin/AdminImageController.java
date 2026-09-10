package com.humolix.volur.container.controller.v1.admin;

import com.humolix.volur.container.domain.dto.AdminImageBackupDTO;
import com.humolix.volur.container.domain.dto.AdminImagePageRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImagePullRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImageTransferDTO;
import com.humolix.volur.container.domain.dto.AdminImageTypeUpdateDTO;
import com.humolix.volur.container.domain.vo.AdminClusterImageDetailVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageStatisticsVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageVO;
import com.humolix.volur.container.service.admin.AdminImageService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端镜像管理", description = "Docker 镜像查看、分层详情、拉取、节点调度、转移备份与全节点同步")
@RestController
@RequestMapping("/api/v1/admin/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminImageController {

    private final AdminImageService adminImageService;

    @Operation(summary = "获取镜像概览统计数据")
    @GetMapping("/statistics")
    public ApiResponse<AdminClusterImageStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取镜像统计数据成功", adminImageService.getStatistics());
    }

    @Operation(summary = "分页获取 Docker 镜像列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminClusterImageVO>> listImages(AdminImagePageRequestDTO request) {
        return ApiResponseEntity.ok("获取镜像列表成功", adminImageService.listImages(request));
    }

    @Operation(summary = "获取 Docker 镜像深度分层详情 (包含 Layers 与 Inspect 信息)")
    @GetMapping("/{id}")
    public ApiResponse<AdminClusterImageDetailVO> getImageDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取镜像分层详情成功", adminImageService.getImageDetail(id));
    }

    @Operation(summary = "修改 Docker 镜像所属类别")
    @Syslog(module = "IMAGE", value = "修改镜像类别", recordParams = true)
    @PutMapping("/{id}/type")
    public ApiResponse<Void> updateImageType(
            @PathVariable Long id,
            @Valid @RequestBody AdminImageTypeUpdateDTO request) {
        adminImageService.updateImageType(id, request);
        return ApiResponseEntity.success("镜像类别修改成功");
    }

    @Operation(summary = "重新扫描并同步加载本地及集群子节点 Docker 镜像")
    @Syslog(module = "IMAGE", value = "重新扫描加载集群镜像", recordParams = true)
    @PostMapping("/scan")
    public ApiResponse<Void> scanAndSyncImages() {
        adminImageService.loadAndSyncClusterImages();
        return ApiResponseEntity.success("已完成本地与集群子节点 Docker 镜像探测与同步");
    }

    @Operation(summary = "拉取/下载 Docker 镜像至指定节点")
    @Syslog(module = "IMAGE", value = "拉取 Docker 镜像", recordParams = true)
    @PostMapping("/pull")
    public ApiResponse<Void> pullImage(@Valid @RequestBody AdminImagePullRequestDTO request) {
        adminImageService.pullImage(request);
        return ApiResponseEntity.success("镜像拉取任务已提交并就绪");
    }

    @Operation(summary = "镜像跨节点转移")
    @Syslog(module = "IMAGE", value = "镜像节点转移", recordParams = true)
    @PostMapping("/transfer")
    public ApiResponse<com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO> transferImage(@Valid @RequestBody AdminImageTransferDTO request) {
        com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO result = adminImageService.transferImage(request);
        return ApiResponseEntity.ok("镜像节点转移任务已提交后台执行", result);
    }

    @Operation(summary = "镜像容灾副本备份或本地归档导出")
    @Syslog(module = "IMAGE", value = "镜像备份", recordParams = true)
    @PostMapping("/backup")
    public ApiResponse<com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO> backupImage(@Valid @RequestBody AdminImageBackupDTO request) {
        com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO result = adminImageService.backupImage(request);
        return ApiResponseEntity.ok("镜像容灾备份任务已提交后台执行", result);
    }

    @Operation(summary = "导出 Docker 镜像归档文件 (.tar) 并提供浏览器直接流式下载")
    @Syslog(module = "IMAGE", value = "导出 Docker 镜像")
    @GetMapping("/{id}/export")
    public void exportImage(@PathVariable Long id, HttpServletResponse response) {
        adminImageService.exportImage(id, response);
    }

    @Operation(summary = "从指定节点删除镜像或全量删除",
            description = "从指定的一个或多个集群节点删除 Docker 镜像副本；若 targetNodeIp 留空或为 ALL，则从所有节点彻底物理删除并清除数据库记录")
    @Syslog(module = "IMAGE", value = "删除 Docker 镜像", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long id,
            @Parameter(description = "目标节点 IP (可传单个 IP、多个逗号分隔 IP，或留空/传 ALL 进行全节点全量清理)")
            @RequestParam(required = false) String targetNodeIp) {
        adminImageService.deleteImage(id, targetNodeIp);
        return ApiResponseEntity.success("镜像已成功清理");
    }

    @Operation(summary = "一键将主节点所有题目镜像同步至所有资源节点")
    @Syslog(module = "IMAGE", value = "同步所有题目镜像至资源节点", recordParams = true)
    @PostMapping("/sync-all")
    public ApiResponse<Void> syncAllImagesToResourceNodes() {
        adminImageService.syncAllImagesToResourceNodes();
        return ApiResponseEntity.success("已向所有资源节点广播同步任务");
    }

}
