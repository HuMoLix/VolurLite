package com.humolix.volur.container.controller.v1;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.container.vo.CtfTrainingChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/challenges/instances")
public class CtfInstanceController {

    private final CtfInstanceService instanceService;

    @PostMapping("/{challengeId}/start")
    public ApiResponse<CtfTrainingChallengeInstanceResponseVO> startInstance(@PathVariable Long challengeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CtfTrainingChallengeInstanceResponseVO vo = instanceService.startTrainingInstance(challengeId, userId);
        return ApiResponseEntity.ok("容器启动成功", vo);
    }

    @PostMapping("/{challengeId}/stop")
    public ApiResponse<Void> stopInstance(@PathVariable Long challengeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        instanceService.stopTrainingInstance(challengeId, userId);
        return ApiResponseEntity.success("容器销毁成功");
    }

    @PostMapping("/{challengeId}/extend")
    public ApiResponse<CtfTrainingChallengeInstanceResponseVO> extendInstance(@PathVariable Long challengeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CtfTrainingChallengeInstanceResponseVO vo = instanceService.extendTrainingInstance(challengeId, userId);
        return ApiResponseEntity.ok("容器延期成功", vo);
    }

}
