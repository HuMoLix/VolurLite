package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryEmailOtpRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryResetRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoveryEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.vo.PasswordRecoveryOptionsVO;
import com.humolix.volur.auth.domain.vo.RecoveryFlowResponseVO;
import com.humolix.volur.auth.domain.vo.UsernameRecoveryOptionsVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.service.EmailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceImplTest {

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private TransitionLockManager transitionLockManager;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FlowCryptoService flowCryptoService;

    private RecoveryServiceImpl recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new RecoveryServiceImpl(
                systemSettingService,
                sessionManager,
                transitionLockManager,
                emailService,
                userRepository,
                passwordEncoder,
                flowCryptoService
        );
    }

    @Test
    void getUsernameRecoveryOptions_whenBothEnabledAndConfigured_shouldReturnBothMethods() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(true);
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn("noreply@volur.com");
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.SMS_ENABLED), eq(false))).willReturn(true);

        UsernameRecoveryOptionsVO result = recoveryService.getUsernameRecoveryOptions();

        Assertions.assertEquals(2, result.getMethods().size());
        Assertions.assertEquals("EMAIL", result.getMethods().get(0).getMethod());
        Assertions.assertEquals("PHONE", result.getMethods().get(1).getMethod());
    }

    @Test
    void getUsernameRecoveryOptions_whenMailEnabledButNotConfigured_shouldExcludeEmail() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(true);
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn("");
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.SMS_ENABLED), eq(false))).willReturn(false);

        UsernameRecoveryOptionsVO result = recoveryService.getUsernameRecoveryOptions();

        Assertions.assertTrue(result.getMethods().isEmpty());
    }

    @Test
    void getUsernameRecoveryOptions_whenBothDisabled_shouldReturnEmptyMethods() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(false);
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn(null);
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.SMS_ENABLED), eq(false))).willReturn(false);

        UsernameRecoveryOptionsVO result = recoveryService.getUsernameRecoveryOptions();

        Assertions.assertTrue(result.getMethods().isEmpty());
    }

    @Test
    void selectRecoveryMethod_whenEmailValid_shouldAdvanceToRecaptchaAndReturnVO() {
        UsernameRecoverySelectRequestDTO request = UsernameRecoverySelectRequestDTO.builder()
                .dsh("DSH_123")
                .cid(0)
                .method("EMAIL")
                .build();
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn("noreply@volur.com");
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(true);

        Session session = Session.builder().dsh("DSH_123").build();
        given(sessionManager.recoverySession(any(), any(), any(), any(), any())).willReturn(session);
        given(transitionLockManager.createTransLock(eq("DSH_123"), eq(1), eq("recaptcha"), any())).willReturn("TL_RECAPTCHA");

        RecoveryFlowResponseVO response = recoveryService.selectRecoveryMethod(request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals("DSH_123", response.getDsh());
        Assertions.assertEquals("TL_RECAPTCHA", response.getTl());
        Assertions.assertEquals("recaptcha", response.getNextStep());
        Assertions.assertEquals("CHALLENGE", response.getStatus());
    }

    @Test
    void submitRecoveryEmail_whenUserExists_shouldSendEmailAndReturnSuccess() {
        UsernameRecoveryEmailRequestDTO request = UsernameRecoveryEmailRequestDTO.builder()
                .dsh("DSH_123")
                .email("test@volur.com")
                .tl("TL_EMAIL")
                .cid(1)
                .build();
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn("noreply@volur.com");
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(true);

        User mockUser = new User();
        mockUser.setUsername("alice");
        mockUser.setEmail("test@volur.com");
        given(userRepository.findByEmail(eq("test@volur.com"))).willReturn(Optional.of(mockUser));

        RecoveryFlowResponseVO response = recoveryService.submitRecoveryEmail(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("DSH_123", response.getDsh());
        Assertions.assertEquals("SUCCESS", response.getStatus());
        verify(emailService).sendRecoverUsernameEmail(eq("test@volur.com"), eq("alice"));
        verify(transitionLockManager).burnAndCompleteSession(eq("DSH_123"), eq("TL_EMAIL"), eq(1), eq("email"));
    }

    @Test
    void submitRecoveryEmail_whenUserNotFound_shouldNotSendEmailAndReturnSuccess() {
        UsernameRecoveryEmailRequestDTO request = UsernameRecoveryEmailRequestDTO.builder()
                .dsh("DSH_123")
                .email("notfound@volur.com")
                .tl("TL_EMAIL")
                .cid(1)
                .build();
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn("noreply@volur.com");
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(true);
        given(userRepository.findByEmail(eq("notfound@volur.com"))).willReturn(Optional.empty());

        RecoveryFlowResponseVO response = recoveryService.submitRecoveryEmail(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("DSH_123", response.getDsh());
        Assertions.assertEquals("SUCCESS", response.getStatus());
        verify(emailService, never()).sendRecoverUsernameEmail(any(), any());
        verify(transitionLockManager).burnAndCompleteSession(eq("DSH_123"), eq("TL_EMAIL"), eq(1), eq("email"));
    }

    @Test
    void getPasswordRecoveryOptions_whenUserExists_shouldReturnMaskedEmailAndMethods() {
        SigninFlow flow = SigninFlow.create();
        flow.setIdentifier("bob");
        Session session = Session.builder().dsh("DSH_PWD").flow(flow).build();
        given(sessionManager.getSession("DSH_PWD")).willReturn(session);

        User user = new User();
        user.setUsername("bob");
        user.setEmail("bob@volur.com");
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user));

        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAIL_FROM_ADDRESS))).willReturn("noreply@volur.com");
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.MAIL_ENABLED), eq(false))).willReturn(true);
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.SMS_ENABLED), eq(false))).willReturn(false);

        PasswordRecoveryOptionsVO vo = recoveryService.getPasswordRecoveryOptions("DSH_PWD");

        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getMaskedEmail());
        Assertions.assertEquals(1, vo.getMethods().size());
        Assertions.assertEquals("EMAIL", vo.getMethods().get(0).getMethod());
    }

    @Test
    void verifyPasswordRecoveryEmailOtp_whenCodeValid_shouldAdvanceToReset() {
        PasswordRecoveryEmailOtpRequestDTO request = PasswordRecoveryEmailOtpRequestDTO.builder()
                .dsh("DSH_PWD")
                .tl("TL_OTP")
                .cid(2)
                .code("123456")
                .build();

        SigninFlow flow = SigninFlow.create();
        flow.setIdentifier("bob");
        Session session = Session.builder().dsh("DSH_PWD").flow(flow).build();
        given(sessionManager.getSession("DSH_PWD")).willReturn(session);

        User user = new User();
        user.setUsername("bob");
        user.setEmail("bob@volur.com");
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user));
        given(emailService.verifyCode(eq("bob@volur.com"), eq("123456"))).willReturn(true);
        given(transitionLockManager.nextTransLock(eq("DSH_PWD"), eq("TL_OTP"), eq(2), eq("emailotp"), eq(3), eq("reset"), eq("bob"))).willReturn("TL_RESET");

        RecoveryFlowResponseVO response = recoveryService.verifyPasswordRecoveryEmailOtp(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("TL_RESET", response.getTl());
        Assertions.assertEquals("reset", response.getNextStep());
        verify(sessionManager).setMetadata(eq("DSH_PWD"), eq("emailVerified"), eq(true));
    }

    @Test
    void submitPasswordReset_whenValid_shouldUpdatePasswordAndBurnSession() {
        PasswordRecoveryResetRequestDTO request = PasswordRecoveryResetRequestDTO.builder()
                .dsh("DSH_PWD")
                .tl("TL_RESET")
                .cid(3)
                .password("NewPass1234!")
                .confirmPassword("NewPass1234!")
                .build();

        SigninFlow flow = SigninFlow.create();
        flow.setIdentifier("bob");
        Session session = Session.builder().dsh("DSH_PWD").flow(flow).build();
        given(sessionManager.getSession("DSH_PWD")).willReturn(session);
        given(sessionManager.getMetadata("DSH_PWD", "emailVerified")).willReturn(true);

        User user = new User();
        user.setUsername("bob");
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPass1234!")).willReturn("ENCODED_PASS");

        RecoveryFlowResponseVO response = recoveryService.submitPasswordReset(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("SUCCESS", response.getStatus());
        verify(userRepository).save(user);
        verify(transitionLockManager).burnAndCompleteSession(eq("DSH_PWD"), eq("TL_RESET"), eq(3), eq("reset"));
    }

}
