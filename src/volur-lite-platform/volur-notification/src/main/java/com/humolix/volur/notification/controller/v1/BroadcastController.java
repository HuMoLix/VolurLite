package com.humolix.volur.notification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.notification.domain.dto.AdminBroadcastPageRequestDTO;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;
import com.humolix.volur.notification.service.admin.AdminBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    private final AdminBroadcastService adminBroadcastService;

    @GetMapping
    public ApiResponseEntity<PageResponse<BroadcastMessageVO>> listBroadcasts(AdminBroadcastPageRequestDTO pageRequest) {
        return ApiResponseEntity.ok("获取全站公告成功", adminBroadcastService.listBroadcasts(pageRequest));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<BroadcastMessageVO> getBroadcastDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取公告详情成功", adminBroadcastService.getBroadcastDetail(id));
    }

}
