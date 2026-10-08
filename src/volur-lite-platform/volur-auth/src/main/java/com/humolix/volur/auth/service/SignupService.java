package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.signup.SignUpCompleteRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEmailResendRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEtotpChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpNameRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpPasswordRequestDTO;
import com.humolix.volur.auth.domain.vo.SignUpFlowResponseVO;
import com.humolix.volur.auth.domain.vo.SignUpOptionVO;

import java.util.List;

public interface SignupService {

    SignUpFlowResponseVO name(SignUpNameRequestDTO request);

    SignUpFlowResponseVO email(SignUpEmailRequestDTO request);

    void resendEmailCode(SignUpEmailResendRequestDTO request);

    SignUpFlowResponseVO verifyEtotp(SignUpEtotpChallengeRequestDTO request);

    SignUpFlowResponseVO password(SignUpPasswordRequestDTO request);

    List<SignUpOptionVO> getOptions(String dsh, String tl);

    SignUpFlowResponseVO complete(SignUpCompleteRequestDTO request);

}
