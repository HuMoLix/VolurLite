package com.humolix.volur.challenge.controller.v1.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfCategoryService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/challenges/ctf/categories")
@RequiredArgsConstructor
public class AdminCtfCategoryController {

    private final AdminCtfCategoryService categoryService;

    @GetMapping
    public ApiResponseEntity<List<CtfCategoryVO>> listCategories() {
        return ApiResponseEntity.ok("获取分类列表成功", categoryService.listCategories());
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<CtfCategoryVO> getCategory(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取分类详情成功", categoryService.getCategory(id));
    }

    @Syslog(module = "CHALLENGE", value = "创建 CTF 赛题分类", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Long> createCategory(@Valid @RequestBody CtfCategoryCreateRequestDTO request) {
        return ApiResponseEntity.created("创建分类成功", categoryService.createCategory(request));
    }

    @Syslog(module = "CHALLENGE", value = "更新 CTF 赛题分类", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CtfCategoryUpdateRequestDTO request) {
        categoryService.updateCategory(id, request);
        return ApiResponseEntity.success("更新分类成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除 CTF 赛题分类", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponseEntity.success("删除分类成功");
    }

}
