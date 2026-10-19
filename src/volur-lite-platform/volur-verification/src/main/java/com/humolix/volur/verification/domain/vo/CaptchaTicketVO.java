package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaTicketVO {

    private String ticket;

    private String scene;

    @Builder.Default
    private Long expireSeconds = 300L;

}
