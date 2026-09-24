package com.humolix.volur.user.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.user.domain.dto.AdminUserBatchRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserPageRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import com.humolix.volur.user.domain.vo.AdminUserStatisticsVO;
import com.humolix.volur.user.domain.vo.RoleVO;
import com.humolix.volur.user.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端用户管理", description = "管理员管理用户列表、增删改查、状态切换与角色管理接口")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "获取用户统计数据")
    @GetMapping("/stats")
    public ApiResponse<AdminUserStatisticsVO> getUserStatistics() {
        return ApiResponseEntity.ok("获取用户统计数据成功", adminUserService.getUserStatistics());
    }

    @Operation(summary = "分页获取用户列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminUserDetailsVO>> listUsers(AdminUserPageRequestDTO pageRequest) {
        Page<AdminUserDetailsVO> pageResponse = adminUserService.getUserList(pageRequest);
        return ApiResponseEntity.ok("获取用户列表成功", new PageResponse<>(pageResponse));
    }

    @Operation(summary = "获取指定用户详情")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailsVO> getUserDetails(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取用户详情成功", adminUserService.getUserDetails(id));
    }

    @Operation(summary = "管理员创建新用户")
    @PostMapping
    public ApiResponse<Long> createUser(@Valid @RequestBody AdminUserCreateRequestDTO request) {
        return ApiResponseEntity.created("创建用户成功", adminUserService.createUser(request));
    }

    @Operation(summary = "管理员更新指定用户信息")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequestDTO request) {
        adminUserService.updateUser(id, request);
        return ApiResponseEntity.success("更新用户成功");
    }

    @Operation(summary = "管理员删除指定用户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ApiResponseEntity.success("删除用户成功");
    }

    @Operation(summary = "批量删除用户")
    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDeleteUsers(@Valid @RequestBody AdminUserBatchRequestDTO request) {
        adminUserService.deleteUsers(request.getIds());
        return ApiResponseEntity.success("批量删除用户成功");
    }

    @Operation(summary = "切换用户隐藏状态")
    @PatchMapping("/{id}/status/hidden")
    public ApiResponse<Void> toggleHiddenStatus(
            @PathVariable Long id,
            @RequestParam boolean status) {
        adminUserService.toggleHiddenStatus(id, status);
        return ApiResponseEntity.success("切换隐藏状态成功");
    }

    @Operation(summary = "切换用户启用/禁用状态")
    @PatchMapping("/{id}/status/active")
    public ApiResponse<Void> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean status) {
        adminUserService.toggleUserStatus(id, status);
        return ApiResponseEntity.success("切换用户状态成功");
    }

    @Operation(summary = "批量切换用户启用/禁用状态")
    @PostMapping("/batch/status")
    public ApiResponse<Void> batchToggleUserStatus(@Valid @RequestBody AdminUserBatchRequestDTO request) {
        adminUserService.toggleUsersStatus(request.getIds(), Boolean.TRUE.equals(request.getStatus()));
        return ApiResponseEntity.success("批量切换用户状态成功");
    }

    @Operation(summary = "获取所有可用系统角色列表")
    @GetMapping("/roles")
    public ApiResponse<List<RoleVO>> listRoles() {
        return ApiResponseEntity.ok("获取角色列表成功", adminUserService.getAllRoles());
    }

}
