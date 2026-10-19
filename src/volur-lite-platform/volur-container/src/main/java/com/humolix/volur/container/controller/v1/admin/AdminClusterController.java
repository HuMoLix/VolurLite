package com.humolix.volur.container.controller.v1.admin;

import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.vo.AdminClusterNodeVO;
import com.humolix.volur.container.domain.vo.AdminClusterSettingsVO;
import com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO;
import com.humolix.volur.container.service.admin.AdminClusterService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/clusters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClusterController {

    private final AdminClusterService adminClusterService;

    @GetMapping("/settings")
    public ApiResponseEntity<AdminClusterSettingsVO> getSettings() {
        return ApiResponseEntity.ok("获取集群设置成功", adminClusterService.getSettings());
    }

    @Syslog(module = "CLUSTER", value = "更新集群配置与调度策略", recordParams = true)
    @PutMapping("/settings")
    public ApiResponseEntity<Void> updateSettings(@Valid @RequestBody AdminClusterSettingsDTO request) {
        adminClusterService.updateSettings(request);
        return ApiResponseEntity.success("集群配置已更新");
    }

    @PostMapping("/test-connection")
    public ApiResponseEntity<AdminContainerTestConnectionVO> testConnection(
            @RequestBody(required = false) AdminContainerTestConnectionDTO request) {
        return ApiResponseEntity.ok("集群连通性测试完成", adminClusterService.testConnection(request));
    }

    @GetMapping("/nodes")
    public ApiResponseEntity<List<AdminClusterNodeVO>> listNodes() {
        return ApiResponseEntity.ok("获取集群节点列表成功", adminClusterService.listNodes());
    }

    @Syslog(module = "CLUSTER", value = "添加节点扩展集群", recordParams = true)
    @PostMapping("/nodes")
    public ApiResponseEntity<AdminClusterNodeVO> addNode(@Valid @RequestBody AdminClusterNodeCreateDTO request) {
        return ApiResponseEntity.created("节点添加成功", adminClusterService.addNode(request));
    }

    @Syslog(module = "CLUSTER", value = "修改集群节点信息", recordParams = true)
    @PutMapping("/nodes/{id}")
    public ApiResponseEntity<AdminClusterNodeVO> updateNode(
            @PathVariable Long id,
            @Valid @RequestBody AdminClusterNodeUpdateDTO request) {
        return ApiResponseEntity.ok("节点信息更新成功", adminClusterService.updateNode(id, request));
    }

    @Syslog(module = "CLUSTER", value = "测试节点连通性与存活状态", recordParams = false)
    @PostMapping("/nodes/{id}/test-connection")
    public ApiResponseEntity<AdminContainerTestConnectionVO> testNodeConnection(
            @PathVariable Long id,
            @RequestBody(required = false) AdminClusterNodeUpdateDTO request) {
        return ApiResponseEntity.ok("节点连通性测试完成", adminClusterService.testNodeConnection(id, request));
    }

    @Syslog(module = "CLUSTER", value = "修改节点角色与资源节点分配", recordParams = true)
    @PutMapping("/nodes/{id}/role")
    public ApiResponseEntity<Void> updateNodeRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminClusterNodeRoleUpdateDTO request) {
        adminClusterService.updateNodeRole(id, request);
        return ApiResponseEntity.success("节点设置已更新");
    }

    @Syslog(module = "CLUSTER", value = "移除集群工作节点", recordParams = true)
    @DeleteMapping("/nodes/{id}")
    public ApiResponseEntity<Void> removeNode(@PathVariable Long id) {
        adminClusterService.removeNode(id);
        return ApiResponseEntity.success("节点已成功从集群移除");
    }

    @Syslog(module = "CLUSTER", value = "同步主节点题目镜像至工作节点", recordParams = true)
    @PostMapping("/nodes/{id}/sync-images")
    public ApiResponseEntity<Void> syncImagesToNode(@PathVariable Long id) {
        adminClusterService.syncImagesToNode(id);
        return ApiResponseEntity.success("已向节点下发镜像同步指令");
    }

    @Syslog(module = "CLUSTER", value = "检测节点存活状态", recordParams = true)
    @PostMapping("/nodes/{id}/health-check")
    public ApiResponseEntity<AdminClusterNodeVO> checkNodeHealth(@PathVariable Long id) {
        return ApiResponseEntity.ok("节点存活检测与资源对账完成", adminClusterService.checkNodeHealth(id));
    }

    @Syslog(module = "CLUSTER", value = "全量检测集群节点存活", recordParams = true)
    @PostMapping("/nodes/health-check-all")
    public ApiResponseEntity<List<AdminClusterNodeVO>> checkAllNodesHealth() {
        return ApiResponseEntity.ok("全量节点存活检测与资源对账完成", adminClusterService.checkAllNodesHealth());
    }

    @Syslog(module = "CLUSTER", value = "工作节点加入编排集群", recordParams = true)
    @PostMapping("/nodes/{id}/join-cluster")
    public ApiResponseEntity<AdminClusterNodeVO> joinCluster(@PathVariable Long id) {
        return ApiResponseEntity.ok("工作节点集群入网探测与注册完成", adminClusterService.joinCluster(id));
    }

    @Syslog(module = "CLUSTER", value = "扫描同步 K3s 集群节点", recordParams = false)
    @PostMapping("/nodes/sync-k3s")
    public ApiResponseEntity<List<AdminClusterNodeVO>> syncK3sClusterNodes() {
        return ApiResponseEntity.ok("K3s 集群节点扫描与对账同步完成", adminClusterService.syncK3sClusterNodes());
    }
}
