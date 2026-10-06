package com.humolix.volur.security.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.security.domain.dto.InitSessionRequestDTO;
import com.humolix.volur.security.session.domain.vo.SessionVO;
import com.humolix.volur.security.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/initSess")
    public ApiResponseEntity<SessionVO> initSess(@NotNull @Valid @ModelAttribute InitSessionRequestDTO requestDTO) {
        SessionVO response = sessionService.initSess(requestDTO);
        return ApiResponseEntity.ok("会话初始化成功", response);
    }

}
