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
public class RecaptchaChallengeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String challengeType;

    private String recaptchaToken;

    private String publicKey;

    private String backgroundImage;

    private String puzzleImage;

    private Integer puzzleY;

    private String captchaImage;

    private String promptText;

    private Object extraData;

}
