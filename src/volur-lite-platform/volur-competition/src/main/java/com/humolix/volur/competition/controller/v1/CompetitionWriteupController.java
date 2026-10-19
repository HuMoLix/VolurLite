package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.dto.CompetitionWriteupSubmitDTO;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupTemplateVO;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupVO;
import com.humolix.volur.competition.service.CompetitionWriteupService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/writeups")
@RequiredArgsConstructor
public class CompetitionWriteupController {

    private final CompetitionWriteupService writeupService;

    @GetMapping("/my")
    public ApiResponseEntity<List<CompetitionWriteupVO>> getMyWriteups(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取队伍 Writeup 记录成功", writeupService.getMyWriteups(competitionId, userId));
    }

    @PostMapping
    public ApiResponseEntity<CompetitionWriteupVO> submitWriteup(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CompetitionWriteupSubmitDTO dto) {
        return ApiResponseEntity.ok("提交 Write Up 成功", writeupService.submitWriteup(competitionId, userId, dto));
    }

    @GetMapping("/template")
    public ApiResponseEntity<CompetitionWriteupTemplateVO> getTemplate(
            @PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取 Writeup 模板成功", writeupService.getTemplate(competitionId));
    }

}
