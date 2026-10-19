package com.humolix.volur.security.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecaptchaVerifyResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean passed;

    private String recaptchaTicket;

}
