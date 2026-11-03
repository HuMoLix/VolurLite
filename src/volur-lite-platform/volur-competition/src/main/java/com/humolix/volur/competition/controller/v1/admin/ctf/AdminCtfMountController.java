package com.humolix.volur.competition.controller.v1.admin.ctf;

import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeBatchMountRequestDTO;
import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeMountRequestDTO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.service.admin.ctf.AdminCtfMountService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/ctf/mount")
@RequiredArgsConstructor
public class AdminCtfMountController {

    private final AdminCtfMountService mountService;

    @GetMapping
    public ApiResponseEntity<List<CtfCompetitionChallengeVO>> listMountedChallenges(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取已挂载赛题成功", mountService.listMountedChallenges(competitionId));
    }

    @Syslog(module = "COMPETITION", value = "挂载题目到竞赛", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Void> mountChallenge(
            @PathVariable Long competitionId,
            @Valid @RequestBody CtfChallengeMountRequestDTO request) {
        mountService.mountChallenge(competitionId, request);
        return ApiResponseEntity.success("挂载赛题成功");
    }

    @Syslog(module = "COMPETITION", value = "批量挂载题目到竞赛", recordParams = true)
    @PostMapping("/batch")
    public ApiResponseEntity<Void> batchMountChallenges(
            @PathVariable Long competitionId,
            @Valid @RequestBody CtfChallengeBatchMountRequestDTO request) {
        mountService.batchMountChallenges(competitionId, request);
        return ApiResponseEntity.success("批量挂载赛题成功");
    }

    @Syslog(module = "COMPETITION", value = "更新已挂载赛题配置", recordParams = true)
    @PutMapping("/{competitionChallengeId}")
    public ApiResponseEntity<Void> updateMountedChallenge(
            @PathVariable Long competitionId,
            @PathVariable Long competitionChallengeId,
            @RequestBody CtfChallengeMountRequestDTO request) {
        mountService.updateMountedChallenge(competitionId, competitionChallengeId, request);
        return ApiResponseEntity.success("更新赛题配置成功");
    }

    @Syslog(module = "COMPETITION", value = "从竞赛卸载赛题", recordParams = true)
    @DeleteMapping("/{competitionChallengeId}")
    public ApiResponseEntity<Void> unmountChallenge(
            @PathVariable Long competitionId,
            @PathVariable Long competitionChallengeId) {
        mountService.unmountChallenge(competitionId, competitionChallengeId);
        return ApiResponseEntity.success("卸载赛题成功");
    }

}
