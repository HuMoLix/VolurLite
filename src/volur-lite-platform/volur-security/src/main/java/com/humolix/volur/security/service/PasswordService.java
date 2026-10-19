package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.PasswordChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;

public interface PasswordService {

    FlowResponseVO verifyPassword(PasswordChallengeRequestDTO request);

}
