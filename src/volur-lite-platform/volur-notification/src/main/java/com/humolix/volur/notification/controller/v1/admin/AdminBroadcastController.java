package com.humolix.volur.notification.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.service.RealtimePushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端全站广播", description = "管理员发布全站系统公告与运营活动/广告弹窗广播接口")
@RestController
@RequestMapping("/api/v1/admin/broadcasts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBroadcastController {

    private final RealtimePushService realtimePushService;

    @Operation(summary = "发布全站系统公告")
    @PostMapping("/announcement")
    public ApiResponse<Void> publishAnnouncement(@Valid @RequestBody BroadcastMessageDTO broadcastDTO) {
        realtimePushService.publishAnnouncement(broadcastDTO);
        return ApiResponseEntity.success("全站系统公告发布成功");
    }

    @Operation(summary = "发布运营活动与广告弹窗")
    @PostMapping("/promotion")
    public ApiResponse<Void> publishPromotion(@Valid @RequestBody BroadcastMessageDTO promotionDTO) {
        realtimePushService.publishPromotion(promotionDTO);
        return ApiResponseEntity.success("运营活动/广告发布成功");
    }

}
