package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;

public interface SigninService {

    SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request);

    SignInFlowResponseVO challenge(SignInChallengeRequestDTO request);

}
