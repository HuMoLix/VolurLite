package com.humolix.volur.security.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.security.domain.vo.FlowInitVO;
import com.humolix.volur.security.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/flow")
public class FlowController {

    private final FlowService flowService;

    @GetMapping("/initSess")
    public ApiResponseEntity<FlowInitVO> initSess(
            @RequestParam(required = false, defaultValue = "VlifWebLogin") String flowName,
            @RequestParam(required = false, defaultValue = "WebServiceLogin") String flowEntry,
            @RequestParam(required = false) String fp,
            @RequestParam(required = false) Long t,
            @RequestParam(required = false) String dsh) {
        FlowInitVO response = flowService.initSess(flowName, flowEntry, fp, t, dsh);
        return ApiResponseEntity.ok("会话初始化成功", response);
    }

}
