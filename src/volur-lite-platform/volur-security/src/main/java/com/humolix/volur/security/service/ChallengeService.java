package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.ChallengeSelectRequestDTO;
import com.humolix.volur.security.domain.dto.EmailOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.PasswordChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.ReCAPTCHAChallengeSubmitDTO;
import com.humolix.volur.security.domain.dto.SMSOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.ChallengeOptionVO;
import com.humolix.volur.security.domain.vo.SignInFlowResponseVO;

import java.util.List;

public interface ChallengeService {

    SignInFlowResponseVO verifyPassword(PasswordChallengeRequestDTO request);

    SignInFlowResponseVO verifyEmailOTP(EmailOTPChallengeRequestDTO request);

    void sendEmailOTP(String dsh, String tl);

    SignInFlowResponseVO verifySMSOTP(SMSOTPChallengeRequestDTO request);

    SignInFlowResponseVO verifyReCAPTCHA(ReCAPTCHAChallengeSubmitDTO request);

    SignInFlowResponseVO selectChallenge(ChallengeSelectRequestDTO request);

    List<ChallengeOptionVO> getChallengeSelection(String dsh, String currentType);

}
