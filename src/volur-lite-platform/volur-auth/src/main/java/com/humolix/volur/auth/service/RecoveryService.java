package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryEmailOtpRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryResetRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoveryEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.vo.PasswordRecoveryOptionsVO;
import com.humolix.volur.auth.domain.vo.RecoveryFlowResponseVO;
import com.humolix.volur.auth.domain.vo.UsernameRecoveryOptionsVO;

public interface RecoveryService {

    UsernameRecoveryOptionsVO getUsernameRecoveryOptions();

    RecoveryFlowResponseVO selectRecoveryMethod(UsernameRecoverySelectRequestDTO request);

    RecoveryFlowResponseVO submitRecoveryEmail(UsernameRecoveryEmailRequestDTO request);

    PasswordRecoveryOptionsVO getPasswordRecoveryOptions(String dsh);

    RecoveryFlowResponseVO selectPasswordRecoveryMethod(PasswordRecoverySelectRequestDTO request);

    void sendPasswordRecoveryEmailOtp(String dsh, String tl);

    RecoveryFlowResponseVO verifyPasswordRecoveryEmailOtp(PasswordRecoveryEmailOtpRequestDTO request);

    RecoveryFlowResponseVO submitPasswordReset(PasswordRecoveryResetRequestDTO request);

}
