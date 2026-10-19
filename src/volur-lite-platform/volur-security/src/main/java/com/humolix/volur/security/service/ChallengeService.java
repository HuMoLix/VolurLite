package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.ChallengeSelectRequestDTO;
import com.humolix.volur.security.domain.dto.EmailOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.ReCAPTCHAChallengeSubmitDTO;
import com.humolix.volur.security.domain.dto.SMSOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.ChallengeOptionVO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;

import java.util.List;

public interface ChallengeService {

    FlowResponseVO verifyEmailOTP(EmailOTPChallengeRequestDTO request);

    void sendEmailOTP(String dsh, String tl);

    FlowResponseVO verifySMSOTP(SMSOTPChallengeRequestDTO request);

    FlowResponseVO verifyReCAPTCHA(ReCAPTCHAChallengeSubmitDTO request);

    FlowResponseVO selectChallenge(ChallengeSelectRequestDTO request);

    List<ChallengeOptionVO> getChallengeSelection(String dsh, String currentType);

}
