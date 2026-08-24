package com.humolix.volur.verification.service;

import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;

public interface CaptchaService {

    CaptchaResponseVO generateCaptcha(String oldCaptchaId);

    CaptchaResponseVO generateChallenge(String sceneStr, String typeStr, String oldCaptchaId);

    boolean verifyCaptcha(String captchaId, String code);

    boolean verifyCaptcha(String captchaId, String code, CaptchaScene scene);

    CaptchaTicketVO verifyAndIssueTicket(CaptchaVerifyDTO request);

    boolean verifyTicket(String ticket, CaptchaScene scene);

}
