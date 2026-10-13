package com.humolix.volur.competition.controller.v1.awdp;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.awdp.AwdPlusPatchUploadRequestDTO;
import com.humolix.volur.competition.domain.vo.awdp.AwdPlusTargetStatusVO;
import com.humolix.volur.competition.service.awdp.AwdPlusCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/awdp")
@RequiredArgsConstructor
public class AwdPlusCompetitionController {

    private final AwdPlusCompetitionService awdPlusCompetitionService;

    @GetMapping("/targets")
    public ApiResponseEntity<List<AwdPlusTargetStatusVO>> listTargets(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取进阶攻防靶机列表成功", awdPlusCompetitionService.listTargets(competitionId, userId));
    }

    @CompetitionLog(logType = "DEFENSE", action = "上传 AWD+ 靶机防御补丁", recordParams = true)
    @PostMapping("/patch/upload")
    public ApiResponseEntity<Void> uploadPatch(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody AwdPlusPatchUploadRequestDTO request) {
        awdPlusCompetitionService.uploadPatch(competitionId, userId, request);
        return ApiResponseEntity.success("补丁上传成功，沙箱自动化验证中");
    }

}
