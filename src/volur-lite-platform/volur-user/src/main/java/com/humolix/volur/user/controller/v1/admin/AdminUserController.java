package com.humolix.volur.user.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.user.domain.dto.AdminUserBatchRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.entity.Role;
import com.humolix.volur.user.service.admin.AdminUserService;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<Page<AdminUserDetailsVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取用户列表成功", adminUserService.getUserList(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailsVO> getUserDetails(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取用户详情成功", adminUserService.getUserDetails(id));
    }

    @PostMapping
    public ApiResponse<Long> createUser(@Valid @RequestBody AdminUserCreateRequestDTO request) {
        return ApiResponseEntity.created("创建用户成功", adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequestDTO request) {
        adminUserService.updateUser(id, request);
        return ApiResponseEntity.success("更新用户成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ApiResponseEntity.success("删除用户成功");
    }

    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDeleteUsers(@Valid @RequestBody AdminUserBatchRequestDTO request) {
        adminUserService.deleteUsers(request.getIds());
        return ApiResponseEntity.success("批量删除用户成功");
    }

    @PatchMapping("/{id}/status/hidden")
    public ApiResponse<Void> toggleHiddenStatus(
            @PathVariable Long id,
            @RequestParam Boolean status) {
        adminUserService.toggleHiddenStatus(id, status);
        return ApiResponseEntity.success("切换隐藏状态成功");
    }

    @PatchMapping("/{id}/status/active")
    public ApiResponse<Void> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean status) {
        adminUserService.toggleUserStatus(id, status);
        return ApiResponseEntity.success("切换用户状态成功");
    }

    @PostMapping("/batch/status")
    public ApiResponse<Void> batchToggleUserStatus(@Valid @RequestBody AdminUserBatchRequestDTO request) {
        adminUserService.toggleUsersStatus(request.getIds(), request.getStatus());
        return ApiResponseEntity.success("批量切换用户状态成功");
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> listRoles() {
        return ApiResponseEntity.ok("获取角色列表成功", adminUserService.getAllRoles());
    }

}
