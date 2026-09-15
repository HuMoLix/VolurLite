package com.humolix.volur.challenge.controller.v1.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfCategoryService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端 CTF 赛题分类管理", description = "管理员管理解题赛 (CTF) 方向分类（Web/Pwn/Reverse/Crypto/Misc等）接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/ctf/categories")
@RequiredArgsConstructor
public class AdminCtfCategoryController {

    private final AdminCtfCategoryService categoryService;

    @Operation(summary = "获取所有 CTF 赛题分类列表")
    @GetMapping
    public ApiResponse<List<CtfCategoryVO>> listCategories() {
        return ApiResponseEntity.ok("获取分类列表成功", categoryService.listCategories());
    }

    @Operation(summary = "获取指定 CTF 赛题分类详情")
    @GetMapping("/{id}")
    public ApiResponse<CtfCategoryVO> getCategory(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取分类详情成功", categoryService.getCategory(id));
    }

    @Operation(summary = "创建 CTF 赛题分类")
    @Syslog(module = "CHALLENGE", value = "创建 CTF 赛题分类", recordParams = true)
    @PostMapping
    public ApiResponse<Long> createCategory(@Valid @RequestBody CtfCategoryCreateRequestDTO request) {
        return ApiResponseEntity.created("创建分类成功", categoryService.createCategory(request));
    }

    @Operation(summary = "更新 CTF 赛题分类")
    @Syslog(module = "CHALLENGE", value = "更新 CTF 赛题分类", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CtfCategoryUpdateRequestDTO request) {
        categoryService.updateCategory(id, request);
        return ApiResponseEntity.success("更新分类成功");
    }

    @Operation(summary = "删除 CTF 赛题分类")
    @Syslog(module = "CHALLENGE", value = "删除 CTF 赛题分类", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponseEntity.success("删除分类成功");
    }

}
