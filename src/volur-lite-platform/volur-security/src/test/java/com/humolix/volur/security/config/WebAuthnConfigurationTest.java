package com.humolix.volur.security.config;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.webauthn.UserPublicKeyCredentialUserEntityRepository;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class WebAuthnConfigurationTest {

    private UserPublicKeyCredentialUserEntityRepository userEntityRepository;
    private UserCredentialRepository userCredentialRepository;
    private WebAuthnProperties properties;
    private SystemSettingService systemSettingService;
    private WebAuthnRelyingPartyOperations rpOperations;

    @BeforeEach
    void setUp() {
        userEntityRepository = Mockito.mock(UserPublicKeyCredentialUserEntityRepository.class);
        userCredentialRepository = Mockito.mock(UserCredentialRepository.class);
        properties = new WebAuthnProperties();
        systemSettingService = Mockito.mock(SystemSettingService.class);

        WebAuthnConfiguration configuration = new WebAuthnConfiguration();
        rpOperations = configuration.webAuthnRelyingPartyOperations(
                userEntityRepository,
                userCredentialRepository,
                properties,
                systemSettingService
        );
    }

    @Test
    void testResolveEffectiveAllowedOrigins() {
        Set<String> origins = properties.resolveEffectiveAllowedOrigins(
                "https://custom.domain.com, https://another.domain.com",
                "https://frontend.volur.io:8443/login"
        );

        Assertions.assertTrue(origins.contains("http://localhost:5173"));
        Assertions.assertTrue(origins.contains("https://custom.domain.com"));
        Assertions.assertTrue(origins.contains("https://another.domain.com"));
        Assertions.assertTrue(origins.contains("https://frontend.volur.io:8443"));
    }

    @Test
    void testResolveEffectiveProperties() {
        Assertions.assertEquals("custom-rp", properties.resolveEffectiveRpId("custom-rp"));
        Assertions.assertEquals("localhost", properties.resolveEffectiveRpId(""));
        Assertions.assertEquals("Custom Name", properties.resolveEffectiveRpName("Custom Name"));
        Assertions.assertEquals("Volur Lite", properties.resolveEffectiveRpName(null));
        Assertions.assertEquals(Duration.ofSeconds(120), properties.resolveEffectiveTimeout(120));
        Assertions.assertEquals(Duration.ofSeconds(60), properties.resolveEffectiveTimeout(null));
        Assertions.assertEquals("required", properties.resolveEffectiveUserVerification("REQUIRED"));
    }

    @Test
    void testDynamicOperations_whenDisabled_throwsServiceUnavailable() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.WEBAUTHN_ENABLED), anyBoolean()))
                .willReturn(false);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "testuser", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        ImmutablePublicKeyCredentialCreationOptionsRequest request =
                new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication);

        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> rpOperations.createPublicKeyCredentialCreationOptions(request)
        );

        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void testDynamicOperations_whenEnabled_customizesRpAndTimeout() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.WEBAUTHN_ENABLED), anyBoolean()))
                .willReturn(true);
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.WEBAUTHN_RP_ID)))
                .willReturn("custom.volur.local");
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.WEBAUTHN_RP_NAME)))
                .willReturn("Custom RP Platform");
        given(systemSettingService.getInt(eq(SystemSettingKeys.WEBAUTHN_TIMEOUT_SECONDS), eq(60)))
                .willReturn(90);

        PublicKeyCredentialUserEntity mockUser = Mockito.mock(PublicKeyCredentialUserEntity.class);
        given(userEntityRepository.findByUsername("testuser")).willReturn(mockUser);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "testuser", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        ImmutablePublicKeyCredentialCreationOptionsRequest request =
                new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication);

        PublicKeyCredentialCreationOptions options = rpOperations.createPublicKeyCredentialCreationOptions(request);

        Assertions.assertNotNull(options);
        Assertions.assertEquals("custom.volur.local", options.getRp().getId());
        Assertions.assertEquals("Custom RP Platform", options.getRp().getName());
        Assertions.assertEquals(Duration.ofSeconds(90), options.getTimeout());
    }

}
