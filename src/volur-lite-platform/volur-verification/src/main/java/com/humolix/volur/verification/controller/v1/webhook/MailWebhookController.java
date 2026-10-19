package com.humolix.volur.verification.controller.v1.webhook;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.verification.service.MailWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification/webhook/mail")
@RequiredArgsConstructor
public class MailWebhookController {

    private final MailWebhookService mailWebhookService;

    @PostMapping
    public ApiResponseEntity<Void> handleWebhook(
            @RequestBody(required = false) String payload,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String headerSecret,
            @RequestHeader(value = "X-Volur-Secret", required = false) String altHeaderSecret,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "secret", required = false) String querySecret,
            @RequestParam(value = "token", required = false) String queryToken) {

        String providedSecret = null;
        if (StringUtils.hasText(headerSecret)) {
            providedSecret = headerSecret;
        } else if (StringUtils.hasText(altHeaderSecret)) {
            providedSecret = altHeaderSecret;
        } else if (StringUtils.hasText(authHeader)) {
            if (authHeader.startsWith("Bearer ")) {
                providedSecret = authHeader.substring(7).trim();
            } else {
                providedSecret = authHeader.trim();
            }
        } else if (StringUtils.hasText(querySecret)) {
            providedSecret = querySecret;
        } else if (StringUtils.hasText(queryToken)) {
            providedSecret = queryToken;
        }

        mailWebhookService.processWebhook(payload, providedSecret);
        return ApiResponseEntity.success("Webhook 事件处理成功");
    }

}
