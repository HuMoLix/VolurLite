package com.humolix.volur.competition.controller.v1.admin.ctf;

import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionSubmissionVO;
import com.humolix.volur.competition.service.admin.ctf.AdminCtfSubmissionService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/ctf/submissions")
@RequiredArgsConstructor
public class AdminCtfSubmissionController {

    private final AdminCtfSubmissionService submissionService;

    @GetMapping
    public ApiResponseEntity<PageResponse<CtfCompetitionSubmissionVO>> listSubmissions(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long challengeId,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取提交记录成功", submissionService.listSubmissions(competitionId, page, size, challengeId, keyword));
    }

}
