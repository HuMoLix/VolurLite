package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;
import com.humolix.volur.auth.exception.UserOrEmailExistException;
import com.humolix.volur.auth.exception.TokenAlreadyBlacklistedException;
import com.humolix.volur.auth.exception.TokenInvalidException;
import com.humolix.volur.auth.exception.InvalidCaptchaException;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    void register(RegisterRequestDTO request) throws InvalidCaptchaException, UserOrEmailExistException;

    AuthenticateResponseVO authenticate(AuthenticateRequestDTO request) throws InvalidCaptchaException;

    AuthenticateResponseVO refreshToken(String refreshToken);

    void processForgotPassword(ForgotPasswordRequestDTO request);

    void processForgotPassword(String email, String username);

    void resetPasswordByToken(String token, String newPassword);

    void resetPassword(ResetPasswordRequestDTO request);

    void sendVerificationEmail(EmailCaptchaRequestDTO request);

    void sendChangePasswordEmail(EmailCaptchaRequestDTO request);

    String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request);

    void changePassword(ChangePasswordRequestDTO request);

    void logout(HttpServletRequest request) throws TokenAlreadyBlacklistedException, TokenInvalidException;

    void deleteAccount(HttpServletRequest request);

}
