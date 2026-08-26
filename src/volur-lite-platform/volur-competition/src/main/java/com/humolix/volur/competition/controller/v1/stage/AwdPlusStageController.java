package com.humolix.volur.competition.controller.v1.stage;

import com.humolix.volur.competition.domain.dto.stage.awdp.AwdPlusPatchUploadRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.awdp.AwdPlusTargetStatusVO;
import com.humolix.volur.competition.service.stage.awdp.AwdPlusStageService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "竞赛阶段：AWD+ 进阶攻防模式", description = "AWD+ 进阶攻防靶机状态查询与自动化防御补丁 (Patch) 上传接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/stage/awdp")
@RequiredArgsConstructor
public class AwdPlusStageController {

    private final AwdPlusStageService awdPlusStageService;

    @Operation(summary = "获取 AWD+ 阶段所有靶机与服务状态")
    @GetMapping("/targets")
    public ApiResponse<List<AwdPlusTargetStatusVO>> listTargets(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取进阶攻防靶机列表成功", awdPlusStageService.listTargets(competitionId, userId));
    }

    @Operation(summary = "上传靶机防御补丁 (Patch)")
    @PostMapping("/patch/upload")
    public ApiResponse<Void> uploadPatch(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody AwdPlusPatchUploadRequestDTO request) {
        awdPlusStageService.uploadPatch(competitionId, userId, request);
        return ApiResponseEntity.success("补丁上传成功，沙箱自动化验证中");
    }

}
