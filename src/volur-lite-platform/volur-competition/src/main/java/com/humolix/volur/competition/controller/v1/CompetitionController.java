package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.vo.CompetitionAboutVO;
import com.humolix.volur.competition.domain.vo.CompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.competition.service.CompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "竞赛信息管理", description = "竞赛公开列表、比赛主页详情与赛事介绍查询接口")
@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @Operation(summary = "分页获取竞赛列表")
    @GetMapping
    public ApiResponse<PageResponse<CompetitionSummaryVO>> listCompetitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取竞赛列表成功", competitionService.listCompetitions(page, size, keyword));
    }

    @Operation(summary = "根据 Slug 获取竞赛主页详情")
    @GetMapping("/by-slug/{slug}")
    public ApiResponse<CompetitionDetailVO> getCompetitionBySlug(@PathVariable String slug) {
        CompetitionDetailVO detail = competitionService.getCompetitionBySlug(slug);
        if (detail == null || detail.getStatus() == CompetitionStatus.DRAFT) {
            return ApiResponseEntity.ok(null);
        }
        return ApiResponseEntity.ok("获取竞赛详情成功", detail);
    }

    @Operation(summary = "根据 ID 获取竞赛主页详情")
    @GetMapping("/{id}")
    public ApiResponse<CompetitionDetailVO> getCompetitionById(@PathVariable Long id) {
        CompetitionDetailVO detail = competitionService.getCompetitionById(id);
        if (detail == null || detail.getStatus() == CompetitionStatus.DRAFT) {
            return ApiResponseEntity.ok(null);
        }
        return ApiResponseEntity.ok("获取竞赛详情成功", detail);
    }

    @Operation(summary = "获取竞赛关于与规则介绍")
    @GetMapping("/{id}/about")
    public ApiResponse<CompetitionAboutVO> getCompetitionAbout(@PathVariable Long id) {
        CompetitionAboutVO about = competitionService.getCompetitionAbout(id);
        if (about == null || about.getStatus() == CompetitionStatus.DRAFT) {
            return ApiResponseEntity.ok(null);
        }
        return ApiResponseEntity.ok("获取竞赛介绍成功", about);
    }

    @Operation(summary = "获取竞赛专属公告列表")
    @GetMapping("/{id}/announcements")
    public ApiResponse<java.util.List<com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO>> listAnnouncements(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取竞赛公告成功", competitionService.listAnnouncements(id));
    }

}
