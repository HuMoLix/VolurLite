package com.humolix.volur.notification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.notification.domain.dto.AdminBroadcastPageRequestDTO;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;
import com.humolix.volur.notification.service.admin.AdminBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "全站广播与公告", description = "用户端获取全站公告与活动广播列表及详情接口")
@RestController
@RequestMapping("/api/v1/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    private final AdminBroadcastService adminBroadcastService;

    @Operation(summary = "分页获取全站公告列表")
    @GetMapping
    public ApiResponse<PageResponse<BroadcastMessageVO>> listBroadcasts(AdminBroadcastPageRequestDTO pageRequest) {
        return ApiResponseEntity.ok("获取全站公告成功", adminBroadcastService.listBroadcasts(pageRequest));
    }

    @Operation(summary = "获取指定公告详情")
    @GetMapping("/{id}")
    public ApiResponse<BroadcastMessageVO> getBroadcastDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取公告详情成功", adminBroadcastService.getBroadcastDetail(id));
    }

}
