package com.humolix.volur.verification.domain.bo;

import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaTicketPayloadBO implements Serializable {

    private CaptchaScene scene;
    private CaptchaType verifiedType;
    private String ip;
    private long verifiedAt;

}
