package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.vo.CompetitionAboutVO;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.domain.vo.CompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.competition.service.CompetitionService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.api.SliceResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public ApiResponseEntity<PageResponse<CompetitionSummaryVO>> listCompetitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取竞赛列表成功", competitionService.listCompetitions(page, size, keyword));
    }

    @GetMapping("/slice")
    public ApiResponseEntity<SliceResponse<CompetitionSummaryVO>> listCompetitionSlice(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CompetitionType type) {
        return ApiResponseEntity.ok("获取竞赛切片列表成功", competitionService.listCompetitionSlice(page, size, keyword, type));
    }

    @GetMapping("/by-slug/{slug}")
    public ApiResponseEntity<CompetitionDetailVO> getCompetitionBySlug(@PathVariable String slug) {
        CompetitionDetailVO detail = competitionService.getCompetitionBySlug(slug);
        if (detail == null || detail.getStatus() == CompetitionStatus.DRAFT) {
            return ApiResponseEntity.notFound("竞赛不存在");
        }
        return ApiResponseEntity.ok("获取竞赛详情成功", detail);
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<CompetitionDetailVO> getCompetitionById(@PathVariable Long id) {
        CompetitionDetailVO detail = competitionService.getCompetitionById(id);
        if (detail == null || detail.getStatus() == CompetitionStatus.DRAFT) {
            return ApiResponseEntity.notFound("竞赛不存在");
        }
        return ApiResponseEntity.ok("获取竞赛详情成功", detail);
    }

    @GetMapping("/{id}/about")
    public ApiResponseEntity<CompetitionAboutVO> getCompetitionAbout(@PathVariable Long id) {
        CompetitionAboutVO about = competitionService.getCompetitionAbout(id);
        if (about == null || about.getStatus() == CompetitionStatus.DRAFT) {
            return ApiResponseEntity.notFound("竞赛介绍不存在");
        }
        return ApiResponseEntity.ok("获取竞赛介绍成功", about);
    }

    @GetMapping("/{id}/announcements")
    public ApiResponseEntity<List<CompetitionAnnouncementVO>> listAnnouncements(
            @PathVariable Long id,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取竞赛公告成功", competitionService.listAnnouncements(id, userId));
    }

}
