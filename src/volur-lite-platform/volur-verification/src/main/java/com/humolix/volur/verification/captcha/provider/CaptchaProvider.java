package com.humolix.volur.verification.captcha.provider;

import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;

public interface CaptchaProvider {

    CaptchaType getType();

    CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel);

    boolean verify(String captchaId, String userInput, CaptchaScene scene);

}
