package com.humolix.volur.notification.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.notification.domain.dto.AdminBroadcastPageRequestDTO;
import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.domain.vo.AdminBroadcastStatisticsVO;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;
import com.humolix.volur.notification.service.admin.AdminBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端全站广播", description = "管理员发布全站系统公告与运营活动/广告弹窗广播接口")
@RestController
@RequestMapping("/api/v1/admin/broadcasts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBroadcastController {

    private final AdminBroadcastService adminBroadcastService;

    @Operation(summary = "获取全站广播统计指标")
    @GetMapping("/stats")
    public ApiResponse<AdminBroadcastStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取广播统计数据成功", adminBroadcastService.getStatistics());
    }

    @Operation(summary = "分页获取广播消息列表")
    @GetMapping
    public ApiResponse<PageResponse<BroadcastMessageVO>> listBroadcasts(AdminBroadcastPageRequestDTO pageRequest) {
        return ApiResponseEntity.ok("获取广播列表成功", adminBroadcastService.listBroadcasts(pageRequest));
    }

    @Operation(summary = "发布全站系统公告")
    @Syslog(module = "NOTIFICATION", value = "发布全站系统公告", recordParams = true)
    @PostMapping("/announcement")
    public ApiResponse<Void> publishAnnouncement(
            @Valid @RequestBody BroadcastMessageDTO broadcastDTO,
            @CurrentUserId(required = false) Long currentUserId) {
        adminBroadcastService.publishAnnouncement(broadcastDTO, currentUserId);
        return ApiResponseEntity.success("全站系统公告发布成功");
    }

    @Operation(summary = "发布运营活动与广告弹窗")
    @Syslog(module = "NOTIFICATION", value = "发布运营活动与广告弹窗", recordParams = true)
    @PostMapping("/promotion")
    public ApiResponse<Void> publishPromotion(
            @Valid @RequestBody BroadcastMessageDTO promotionDTO,
            @CurrentUserId(required = false) Long currentUserId) {
        adminBroadcastService.publishPromotion(promotionDTO, currentUserId);
        return ApiResponseEntity.success("运营活动/广告发布成功");
    }

    @Operation(summary = "删除/撤回指定广播通知")
    @Syslog(module = "NOTIFICATION", value = "删除全站广播通知", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBroadcast(@PathVariable Long id) {
        adminBroadcastService.deleteBroadcast(id);
        return ApiResponseEntity.success("广播通知删除成功");
    }

}
