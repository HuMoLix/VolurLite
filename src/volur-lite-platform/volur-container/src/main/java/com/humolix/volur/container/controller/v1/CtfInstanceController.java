package com.humolix.volur.container.controller.v1;

import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "赛题靶机容器管理", description = "动态靶机容器启动、销毁与租期延长接口")
@RestController
@RequestMapping("/api/v1/challenges/instances")
@RequiredArgsConstructor
public class CtfInstanceController {

    private final CtfInstanceService instanceService;

    @Operation(summary = "启动靶机容器")
    @Syslog(module = "CONTAINER", value = "启动靶机容器", recordParams = true)
    @PostMapping("/{challengeId}/start")
    public ApiResponse<ChallengeInstanceResponseVO> startInstance(
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        ChallengeInstanceResponseVO vo = instanceService.startInstance(challengeId, userId);
        return ApiResponseEntity.ok("容器启动成功", vo);
    }

    @Operation(summary = "销毁/停止靶机容器")
    @Syslog(module = "CONTAINER", value = "销毁靶机容器", recordParams = true)
    @PostMapping("/{challengeId}/stop")
    public ApiResponse<Void> stopInstance(
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        instanceService.stopInstance(challengeId, userId);
        return ApiResponseEntity.success("容器销毁成功");
    }

    @Operation(summary = "延长靶机容器租期")
    @Syslog(module = "CONTAINER", value = "延长靶机容器租期", recordParams = true)
    @PostMapping("/{challengeId}/extend")
    public ApiResponse<ChallengeInstanceResponseVO> extendInstance(
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        ChallengeInstanceResponseVO vo = instanceService.extendInstance(challengeId, userId);
        return ApiResponseEntity.ok("容器延期成功", vo);
    }

}
