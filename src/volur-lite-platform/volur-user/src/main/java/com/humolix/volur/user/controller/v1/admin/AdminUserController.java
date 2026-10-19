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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/stats")
    public ApiResponseEntity<AdminUserStatisticsVO> getUserStatistics() {
        return ApiResponseEntity.ok("获取用户统计数据成功", adminUserService.getUserStatistics());
    }

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminUserDetailsVO>> listUsers(AdminUserPageRequestDTO pageRequest) {
        Page<AdminUserDetailsVO> pageResponse = adminUserService.getUserList(pageRequest);
        return ApiResponseEntity.ok("获取用户列表成功", new PageResponse<>(pageResponse));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AdminUserDetailsVO> getUserDetails(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取用户详情成功", adminUserService.getUserDetails(id));
    }

    @PostMapping
    public ApiResponseEntity<Long> createUser(@Valid @RequestBody AdminUserCreateRequestDTO request) {
        return ApiResponseEntity.created("创建用户成功", adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequestDTO request) {
        adminUserService.updateUser(id, request);
        return ApiResponseEntity.success("更新用户成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ApiResponseEntity.success("删除用户成功");
    }

    @PostMapping("/batch/delete")
    public ApiResponseEntity<Void> batchDeleteUsers(@Valid @RequestBody AdminUserBatchRequestDTO request) {
        adminUserService.deleteUsers(request.getIds());
        return ApiResponseEntity.success("批量删除用户成功");
    }

    @PatchMapping("/{id}/status/hidden")
    public ApiResponseEntity<Void> toggleHiddenStatus(
            @PathVariable Long id,
            @RequestParam boolean status) {
        adminUserService.toggleHiddenStatus(id, status);
        return ApiResponseEntity.success("切换隐藏状态成功");
    }

    @PatchMapping("/{id}/status/active")
    public ApiResponseEntity<Void> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean status) {
        adminUserService.toggleUserStatus(id, status);
        return ApiResponseEntity.success("切换用户状态成功");
    }

    @PostMapping("/batch/status")
    public ApiResponseEntity<Void> batchToggleUserStatus(@Valid @RequestBody AdminUserBatchRequestDTO request) {
        adminUserService.toggleUsersStatus(request.getIds(), Boolean.TRUE.equals(request.getStatus()));
        return ApiResponseEntity.success("批量切换用户状态成功");
    }

    @GetMapping("/roles")
    public ApiResponseEntity<List<RoleVO>> listRoles() {
        return ApiResponseEntity.ok("获取角色列表成功", adminUserService.getAllRoles());
    }

}
