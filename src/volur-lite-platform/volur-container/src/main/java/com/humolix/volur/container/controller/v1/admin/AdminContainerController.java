package com.humolix.volur.container.controller.v1.admin;

import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.vo.*;
import com.humolix.volur.container.service.admin.AdminContainerService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/containers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContainerController {

    private final AdminContainerService adminContainerService;

    @GetMapping("/stats")
    public ApiResponseEntity<AdminContainerStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取容器统计数据成功", adminContainerService.getStatistics());
    }

    @GetMapping("/candidate-nodes")
    public ApiResponseEntity<List<AdminClusterNodeVO>> getCandidateNodes() {
        return ApiResponseEntity.ok("获取候选资源节点成功", adminContainerService.getCandidateNodes());
    }

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminContainerInstanceVO>> listInstances(AdminContainerPageRequestDTO request) {
        return ApiResponseEntity.ok("获取容器列表成功", adminContainerService.listInstances(request));
    }

    @Syslog(module = "CONTAINER", value = "管理员手动启动靶机容器", recordParams = true)
    @PostMapping("/start")
    public ApiResponseEntity<AdminContainerInstanceVO> manualStartInstance(
            @Valid @RequestBody AdminContainerManualStartDTO request) {
        return ApiResponseEntity.created("靶机容器启动成功", adminContainerService.manualStartInstance(request));
    }

    @Syslog(module = "CONTAINER", value = "容器内执行交互式命令", recordParams = true)
    @PostMapping("/{id}/exec")
    public ApiResponseEntity<AdminContainerExecResultVO> execContainer(
            @PathVariable Long id,
            @Valid @RequestBody AdminContainerExecDTO request) {
        return ApiResponseEntity.ok("命令执行完成", adminContainerService.execContainer(id, request));
    }

    @GetMapping("/{id}/terminal-info")
    public ApiResponseEntity<AdminContainerTerminalInfoVO> getTerminalInfo(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取终端会话信息成功", adminContainerService.getTerminalInfo(id));
    }

    @Syslog(module = "CONTAINER", value = "管理员强制销毁容器实例", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> stopInstance(@PathVariable Long id) {
        adminContainerService.stopInstance(id);
        return ApiResponseEntity.success("容器实例已停止并释放资源");
    }

    @Syslog(module = "CONTAINER", value = "管理员批量销毁容器实例", recordParams = true)
    @PostMapping("/batch-stop")
    public ApiResponseEntity<Void> batchStop(@Valid @RequestBody AdminContainerBatchStopRequestDTO request) {
        adminContainerService.batchStop(request);
        return ApiResponseEntity.success("批量停止容器实例成功");
    }

    @Syslog(module = "CONTAINER", value = "管理员为容器实例续期", recordParams = true)
    @PutMapping("/{id}/renew")
    public ApiResponseEntity<Void> renewInstance(
            @PathVariable Long id,
            @Valid @RequestBody AdminContainerRenewRequestDTO request) {
        adminContainerService.renewInstance(id, request);
        return ApiResponseEntity.success("延长容器时间成功");
    }

}
