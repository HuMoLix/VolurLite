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
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final AdminCompetitionService competitionService;

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminCompetitionListVO>> listCompetitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取竞赛列表成功", competitionService.listCompetitions(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AdminCompetitionDetailVO> getCompetition(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取竞赛详情成功", competitionService.getCompetition(id));
    }

    @Syslog(module = "COMPETITION", value = "创建竞赛", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Long> createCompetition(
            @Valid @RequestBody CompetitionCreateRequestDTO request,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.created("创建竞赛成功", competitionService.createCompetition(request, userId));
    }

    @Syslog(module = "COMPETITION", value = "更新竞赛信息", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateCompetition(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionUpdateRequestDTO request) {
        competitionService.updateCompetition(id, request);
        return ApiResponseEntity.success("更新竞赛成功");
    }

    @Syslog(module = "COMPETITION", value = "删除竞赛", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        competitionService.deleteCompetition(id);
        return ApiResponseEntity.success("删除竞赛成功");
    }

    @Syslog(module = "COMPETITION", value = "设置竞赛封榜状态", recordParams = true)
    @PostMapping("/{id}/freeze")
    public ApiResponseEntity<Void> setFreezeScoreboard(
            @PathVariable Long id,
            @RequestParam boolean frozen) {
        competitionService.setScoreboardFrozen(id, frozen);
        return ApiResponseEntity.success(frozen ? "封榜成功" : "解封榜单成功");
    }

    @Syslog(module = "COMPETITION", value = "归档竞赛", recordParams = true)
    @PostMapping("/{id}/archive")
    public ApiResponseEntity<Void> archiveCompetition(@PathVariable Long id) {
        competitionService.archiveCompetition(id);
        return ApiResponseEntity.success("归档竞赛成功");
    }

    @Syslog(module = "COMPETITION", value = "取消归档竞赛", recordParams = true)
    @PostMapping("/{id}/unarchive")
    public ApiResponseEntity<Void> unarchiveCompetition(@PathVariable Long id) {
        competitionService.unarchiveCompetition(id);
        return ApiResponseEntity.success("取消归档成功");
    }

}
