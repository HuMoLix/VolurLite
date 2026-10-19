package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    void register(RegisterRequestDTO request);

    AuthenticateResponseVO authenticate(AuthenticateRequestDTO request);

    void processForgotPassword(ForgotPasswordRequestDTO request);

    void processForgotPassword(String email, String username);

    void resetPasswordByToken(String token, String newPassword);

    void resetPassword(ResetPasswordRequestDTO request);

    void sendChangePasswordEmail(EmailCaptchaRequestDTO request);

    String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request);

    void changePassword(ChangePasswordRequestDTO request);

    void sendDeleteAccountEmail(EmailCaptchaRequestDTO request);

    void deleteAccount(DeleteAccountRequestDTO request, HttpServletRequest httpRequest);

}

