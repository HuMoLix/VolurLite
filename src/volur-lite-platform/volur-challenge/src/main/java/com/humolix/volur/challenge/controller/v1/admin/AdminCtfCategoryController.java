package com.humolix.volur.challenge.controller.v1.admin;

import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCategoryCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfChallengeCategoryResponseVO;
import com.humolix.volur.challenge.service.admin.stage.ctf.AdminCtfCategoryService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories/ctf")
public class AdminCtfCategoryController {

    private final AdminCtfCategoryService categoryService;

    @GetMapping("/list")
    public ApiResponse<List<CtfChallengeCategoryResponseVO>> listCategories() {
        return ApiResponseEntity.ok("获取全部分类列表成功", categoryService.listCategories());
    }

    @GetMapping
    public ApiResponse<Page<CtfChallengeCategoryResponseVO>> pageCategories(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("分页获取分类列表成功", categoryService.pageCategories(page, size, keyword));
    }

    @PostMapping
    public ApiResponse<CtfChallengeCategoryResponseVO> createCategory(
            @Valid @RequestBody CtfChallengeCategoryCreateRequestDTO request) {
        return ApiResponseEntity.created("创建分类成功", categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CtfChallengeCategoryResponseVO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CtfChallengeCategoryUpdateRequestDTO request) {
        return ApiResponseEntity.ok("更新分类成功", categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponseEntity.success("删除分类成功");
    }

}
