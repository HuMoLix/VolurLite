package com.humolix.volur.container.controller.v1.admin;

import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.vo.*;
import com.humolix.volur.container.service.admin.AdminContainerService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端容器管理", description = "管理员监控并管理全平台靶机容器实例与集群生命周期")
@RestController
@RequestMapping("/api/v1/admin/containers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContainerController {

    private final AdminContainerService adminContainerService;

    @Operation(summary = "获取容器集群与运行统计数据")
    @GetMapping("/stats")
    public ApiResponse<AdminContainerStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取容器统计数据成功", adminContainerService.getStatistics());
    }

    @Operation(summary = "获取支持部署的候选资源节点列表")
    @GetMapping("/candidate-nodes")
    public ApiResponse<List<AdminClusterNodeVO>> getCandidateNodes() {
        return ApiResponseEntity.ok("获取候选资源节点成功", adminContainerService.getCandidateNodes());
    }

    @Operation(summary = "分页获取容器实例列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminContainerInstanceVO>> listInstances(AdminContainerPageRequestDTO request) {
        return ApiResponseEntity.ok("获取容器列表成功", adminContainerService.listInstances(request));
    }

    @Operation(summary = "管理员手动启动靶机容器")
    @Syslog(module = "CONTAINER", value = "管理员手动启动靶机容器", recordParams = true)
    @PostMapping("/start")
    public ApiResponse<AdminContainerInstanceVO> manualStartInstance(
            @Valid @RequestBody AdminContainerManualStartDTO request) {
        return ApiResponseEntity.created("靶机容器启动成功", adminContainerService.manualStartInstance(request));
    }

    @Operation(summary = "在容器内执行调试 Shell 命令")
    @Syslog(module = "CONTAINER", value = "容器内执行交互式命令", recordParams = true)
    @PostMapping("/{id}/exec")
    public ApiResponse<AdminContainerExecResultVO> execContainer(
            @PathVariable Long id,
            @Valid @RequestBody AdminContainerExecDTO request) {
        return ApiResponseEntity.ok("命令执行完成", adminContainerService.execContainer(id, request));
    }

    @Operation(summary = "获取容器交互终端会话信息")
    @GetMapping("/{id}/terminal-info")
    public ApiResponse<AdminContainerTerminalInfoVO> getTerminalInfo(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取终端会话信息成功", adminContainerService.getTerminalInfo(id));
    }

    @Operation(summary = "强制销毁并释放单个容器实例")
    @Syslog(module = "CONTAINER", value = "管理员强制销毁容器实例", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> stopInstance(@PathVariable Long id) {
        adminContainerService.stopInstance(id);
        return ApiResponseEntity.success("容器实例已停止并释放资源");
    }

    @Operation(summary = "批量销毁容器实例")
    @Syslog(module = "CONTAINER", value = "管理员批量销毁容器实例", recordParams = true)
    @PostMapping("/batch-stop")
    public ApiResponse<Void> batchStop(@Valid @RequestBody AdminContainerBatchStopRequestDTO request) {
        adminContainerService.batchStop(request);
        return ApiResponseEntity.success("批量停止容器实例成功");
    }

    @Operation(summary = "延长容器实例运行时间")
    @Syslog(module = "CONTAINER", value = "管理员为容器实例续期", recordParams = true)
    @PutMapping("/{id}/renew")
    public ApiResponse<Void> renewInstance(
            @PathVariable Long id,
            @Valid @RequestBody AdminContainerRenewRequestDTO request) {
        adminContainerService.renewInstance(id, request);
        return ApiResponseEntity.success("延长容器时间成功");
    }

}
