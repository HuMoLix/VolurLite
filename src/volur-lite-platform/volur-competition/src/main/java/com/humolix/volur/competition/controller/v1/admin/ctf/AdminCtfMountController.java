package com.humolix.volur.competition.controller.v1.admin.ctf;

import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeBatchMountRequestDTO;
import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeMountRequestDTO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.service.admin.ctf.AdminCtfMountService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端 CTF 赛题挂载", description = "管理员在比赛中挂载、批量挂载与卸载题库题目接口")
@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/ctf/mount")
@RequiredArgsConstructor
public class AdminCtfMountController {

    private final AdminCtfMountService mountService;

    @Operation(summary = "获取当前比赛已挂载的 CTF 赛题列表")
    @GetMapping
    public ApiResponse<List<CtfCompetitionChallengeVO>> listMountedChallenges(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取已挂载赛题成功", mountService.listMountedChallenges(competitionId));
    }

    @Operation(summary = "单个挂载题库题目到竞赛")
    @Syslog(module = "COMPETITION", value = "挂载题目到竞赛", recordParams = true)
    @PostMapping
    public ApiResponse<Void> mountChallenge(
            @PathVariable Long competitionId,
            @Valid @RequestBody CtfChallengeMountRequestDTO request) {
        mountService.mountChallenge(competitionId, request);
        return ApiResponseEntity.success("挂载赛题成功");
    }

    @Operation(summary = "批量挂载题库题目到竞赛")
    @Syslog(module = "COMPETITION", value = "批量挂载题目到竞赛", recordParams = true)
    @PostMapping("/batch")
    public ApiResponse<Void> batchMountChallenges(
            @PathVariable Long competitionId,
            @Valid @RequestBody CtfChallengeBatchMountRequestDTO request) {
        mountService.batchMountChallenges(competitionId, request);
        return ApiResponseEntity.success("批量挂载赛题成功");
    }

    @Operation(summary = "更新已挂载赛题配置")
    @Syslog(module = "COMPETITION", value = "更新已挂载赛题配置", recordParams = true)
    @PutMapping("/{competitionChallengeId}")
    public ApiResponse<Void> updateMountedChallenge(
            @PathVariable Long competitionId,
            @PathVariable Long competitionChallengeId,
            @RequestBody CtfChallengeMountRequestDTO request) {
        mountService.updateMountedChallenge(competitionId, competitionChallengeId, request);
        return ApiResponseEntity.success("更新赛题配置成功");
    }

    @Operation(summary = "从竞赛中卸载赛题")
    @Syslog(module = "COMPETITION", value = "从竞赛卸载赛题", recordParams = true)
    @DeleteMapping("/{competitionChallengeId}")
    public ApiResponse<Void> unmountChallenge(
            @PathVariable Long competitionId,
            @PathVariable Long competitionChallengeId) {
        mountService.unmountChallenge(competitionId, competitionChallengeId);
        return ApiResponseEntity.success("卸载赛题成功");
    }

}
