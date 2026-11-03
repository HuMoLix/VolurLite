package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.ChallengeSelectRequestDTO;
import com.humolix.volur.security.domain.vo.ChallengeOptionVO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;

    @Syslog(module = "SECURITY", value = "选择安全挑战方式", logType = "AUTH", recordParams = false)
    @PostMapping("/select")
    public ApiResponseEntity<FlowResponseVO> selectChallenge(
            @Valid @RequestBody ChallengeSelectRequestDTO request) {
        FlowResponseVO response = challengeService.selectChallenge(request);
        return ApiResponseEntity.ok("切换安全挑战方式成功", response);
    }

    @Syslog(module = "SECURITY", value = "获取备选核验方式", logType = "AUTH", recordParams = false)
    @GetMapping("/selection")
    public ApiResponseEntity<List<ChallengeOptionVO>> getChallengeSelection(
            @RequestParam(value = "dsh", required = false) String dsh,
            @RequestParam(value = "currentType", required = false) String currentType) {
        List<ChallengeOptionVO> response = challengeService.getChallengeSelection(dsh, currentType);
        return ApiResponseEntity.ok("获取可选安全挑战方式成功", response);
    }

}
