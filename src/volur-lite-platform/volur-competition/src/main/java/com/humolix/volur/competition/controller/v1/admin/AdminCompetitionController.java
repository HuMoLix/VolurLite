package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.CompetitionCreateRequestDTO;
import com.humolix.volur.competition.domain.dto.CompetitionUpdateRequestDTO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionListVO;
import com.humolix.volur.competition.service.admin.AdminCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端竞赛管理", description = "管理员管理赛事创建、编辑、删除与封榜控制接口")
@RestController
@RequestMapping("/api/v1/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final AdminCompetitionService competitionService;

    @Operation(summary = "分页获取竞赛列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminCompetitionListVO>> listCompetitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取竞赛列表成功", competitionService.listCompetitions(page, size, keyword));
    }

    @Operation(summary = "获取指定竞赛管理详情")
    @GetMapping("/{id}")
    public ApiResponse<AdminCompetitionDetailVO> getCompetition(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取竞赛详情成功", competitionService.getCompetition(id));
    }

    @Operation(summary = "创建新竞赛")
    @PostMapping
    public ApiResponse<Long> createCompetition(
            @Valid @RequestBody CompetitionCreateRequestDTO request,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.created("创建竞赛成功", competitionService.createCompetition(request, userId));
    }

    @Operation(summary = "更新竞赛基本信息与规则")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateCompetition(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionUpdateRequestDTO request) {
        competitionService.updateCompetition(id, request);
        return ApiResponseEntity.success("更新竞赛成功");
    }

    @Operation(summary = "删除竞赛")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCompetition(@PathVariable Long id) {
        competitionService.deleteCompetition(id);
        return ApiResponseEntity.success("删除竞赛成功");
    }

    @Operation(summary = "设置竞赛天梯榜封榜/解封状态")
    @PostMapping("/{id}/freeze")
    public ApiResponse<Void> setFreezeScoreboard(
            @PathVariable Long id,
            @RequestParam boolean frozen) {
        competitionService.setScoreboardFrozen(id, frozen);
        return ApiResponseEntity.success(frozen ? "封榜成功" : "解封榜单成功");
    }

}
