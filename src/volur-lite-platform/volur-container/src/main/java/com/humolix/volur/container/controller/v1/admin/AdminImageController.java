package com.humolix.volur.container.controller.v1.admin;

import com.humolix.volur.container.domain.dto.AdminImageBackupDTO;
import com.humolix.volur.container.domain.dto.AdminImagePageRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImagePullRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImageTransferDTO;
import com.humolix.volur.container.domain.vo.AdminClusterImageDetailVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageStatisticsVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageVO;
import com.humolix.volur.container.service.admin.AdminImageService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminImageController {

    private final AdminImageService adminImageService;

    @GetMapping("/stats")
    public ApiResponseEntity<AdminClusterImageStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取镜像统计数据成功", adminImageService.getStatistics());
    }

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminClusterImageVO>> listImages(AdminImagePageRequestDTO request) {
        return ApiResponseEntity.ok("获取镜像列表成功", adminImageService.listImages(request));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AdminClusterImageDetailVO> getImageDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取镜像分层详情成功", adminImageService.getImageDetail(id));
    }

    @Syslog(module = "IMAGE", value = "重新扫描加载集群镜像", recordParams = true)
    @PostMapping("/scan")
    public ApiResponseEntity<Void> scanAndSyncImages() {
        adminImageService.loadAndSyncClusterImages();
        return ApiResponseEntity.success("已完成本地与集群子节点 Docker 镜像探测与同步");
    }

    @Syslog(module = "IMAGE", value = "拉取 Docker 镜像", recordParams = true)
    @PostMapping("/pull")
    public ApiResponseEntity<com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO> pullImage(@Valid @RequestBody AdminImagePullRequestDTO request) {
        com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO result = adminImageService.pullImage(request);
        return ApiResponseEntity.ok("镜像拉取与中心入库任务已提交后台执行", result);
    }

    @Syslog(module = "IMAGE", value = "镜像节点转移", recordParams = true)
    @PostMapping("/transfer")
    public ApiResponseEntity<com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO> transferImage(@Valid @RequestBody AdminImageTransferDTO request) {
        com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO result = adminImageService.transferImage(request);
        return ApiResponseEntity.ok("镜像节点转移任务已提交后台执行", result);
    }

    @Syslog(module = "IMAGE", value = "镜像备份", recordParams = true)
    @PostMapping("/backup")
    public ApiResponseEntity<com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO> backupImage(@Valid @RequestBody AdminImageBackupDTO request) {
        com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO result = adminImageService.backupImage(request);
        return ApiResponseEntity.ok("镜像容灾备份任务已提交后台执行", result);
    }

    @Syslog(module = "IMAGE", value = "导出 Docker 镜像")
    @GetMapping("/{id}/export")
    public void exportImage(@PathVariable Long id, HttpServletResponse response) {
        adminImageService.exportImage(id, response);
    }

    @Syslog(module = "IMAGE", value = "导入 Docker 镜像文件")
    @PostMapping("/import")
    public ApiResponseEntity<com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO> importImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "repository", required = false) String repository,
            @RequestParam(value = "tag", required = false) String tag) {
        com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO result = adminImageService.importImage(file, repository, tag);
        return ApiResponseEntity.ok("镜像导入与中心入库任务已提交后台执行", result);
    }

    @Syslog(module = "IMAGE", value = "推送镜像至中心镜像仓库", recordParams = true)
    @PostMapping("/{id}/push-registry")
    public ApiResponseEntity<Void> pushImageToRegistry(@PathVariable Long id) {
        adminImageService.pushImageToRegistry(id);
        return ApiResponseEntity.success("镜像已成功推送至中心镜像仓库");
    }

    @Syslog(module = "IMAGE", value = "删除 Docker 镜像", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @RequestParam(required = false) String targetNodeIp) {
        adminImageService.deleteImage(id, targetNodeIp);
        return ApiResponseEntity.success("镜像已成功清理");
    }

    @Syslog(module = "IMAGE", value = "同步所有题目镜像至资源节点", recordParams = true)
    @PostMapping("/sync-all")
    public ApiResponseEntity<Void> syncAllImagesToResourceNodes() {
        adminImageService.syncAllImagesToResourceNodes();
        return ApiResponseEntity.success("已向所有资源节点广播同步任务");
    }

}
