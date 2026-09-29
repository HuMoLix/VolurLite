package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;

import com.humolix.volur.auth.domain.vo.ChallengeOptionVO;

import java.util.List;

public interface SigninService {

    SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request);

    SignInFlowResponseVO challenge(SignInChallengeRequestDTO request);

    List<ChallengeOptionVO> getChallengeSelection(String dsh, String currentType);

}
