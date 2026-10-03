package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;

public interface RecaptchaService {

    RecaptchaChallengeVO getRecaptchaChallenge(String dsh, String tl);

    RecaptchaVerifyResponseVO verifyRecaptcha(String dsh, String tl, RecaptchaVerifyRequestDTO request);

    boolean validateAndBurnTicket(String dsh, String ticket);

}
