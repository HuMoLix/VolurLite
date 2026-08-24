package com.humolix.volur.verification.service;

import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;

public interface EmailService {

    void sendVerificationEmail(EmailCaptchaRequestDTO request);

    void sendVerificationEmail(String email, String username);

    void sendVerificationEmail(String email, String username, boolean skipExistenceCheck);

    void sendChangePasswordEmail(EmailCaptchaRequestDTO request);

    void sendChangePasswordEmail(String email, String username);

    void sendResetPasswordEmail(String email, String username);

    boolean verifyCode(String email, String code);

    String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request);

    void sendEmail(String to, String subject, String content);

    void testSmtpConnection();

    void preloadTemplate();

}
