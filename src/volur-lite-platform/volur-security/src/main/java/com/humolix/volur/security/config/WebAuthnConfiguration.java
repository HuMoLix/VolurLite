package com.humolix.volur.security.config;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.webauthn.UserPublicKeyCredentialUserEntityRepository;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

import java.time.Duration;
import java.util.Set;

@Configuration
public class WebAuthnConfiguration {

    @Bean
    public UserCredentialRepository userCredentialRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcUserCredentialRepository(jdbcTemplate);
    }

    @Bean
    public WebauthnJacksonModule webauthnJacksonModule() {
        return new WebauthnJacksonModule();
    }

    @Bean
    public WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            UserPublicKeyCredentialUserEntityRepository userEntityRepository,
            UserCredentialRepository userCredentialRepository,
            WebAuthnProperties properties,
            SystemSettingService systemSettingService
    ) {
        return new DynamicWebAuthnRelyingPartyOperations(
                userEntityRepository,
                userCredentialRepository,
                properties,
                systemSettingService
        );
    }

    @RequiredArgsConstructor
    public static class DynamicWebAuthnRelyingPartyOperations implements WebAuthnRelyingPartyOperations {

        private final UserPublicKeyCredentialUserEntityRepository userEntityRepository;
        private final UserCredentialRepository userCredentialRepository;
        private final WebAuthnProperties properties;
        private final SystemSettingService systemSettingService;

        private void assertEnabled() {
            boolean enabled = systemSettingService.getBoolean(SystemSettingKeys.WEBAUTHN_ENABLED, properties.isEnabled());
            if (!enabled) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "通行密钥 (WebAuthn) 认证服务已停用");
            }
        }

        private WebAuthnRelyingPartyOperations createDelegate() {
            assertEnabled();

            String rpId = properties.resolveEffectiveRpId(
                    systemSettingService.getSettingValue(SystemSettingKeys.WEBAUTHN_RP_ID)
            );
            String rpName = properties.resolveEffectiveRpName(
                    systemSettingService.getSettingValue(SystemSettingKeys.WEBAUTHN_RP_NAME)
            );
            String dynamicOrigins = systemSettingService.getSettingValue(SystemSettingKeys.WEBAUTHN_ALLOWED_ORIGINS);
            String frontendUrl = systemSettingService.getSettingValue(SystemSettingKeys.GENERAL_FRONTEND_URL);
            Set<String> allowedOrigins = properties.resolveEffectiveAllowedOrigins(dynamicOrigins, frontendUrl);

            int timeoutSeconds = systemSettingService.getInt(
                    SystemSettingKeys.WEBAUTHN_TIMEOUT_SECONDS,
                    (int) properties.getTimeout().toSeconds()
            );
            Duration timeout = properties.resolveEffectiveTimeout(timeoutSeconds);

            PublicKeyCredentialRpEntity rpEntity = PublicKeyCredentialRpEntity.builder()
                    .id(rpId)
                    .name(rpName)
                    .build();

            Webauthn4JRelyingPartyOperations operations = new Webauthn4JRelyingPartyOperations(
                    userEntityRepository,
                    userCredentialRepository,
                    rpEntity,
                    allowedOrigins
            );
            operations.setCustomizeCreationOptions(builder -> builder.timeout(timeout));
            operations.setCustomizeRequestOptions(builder -> builder.timeout(timeout));
            return operations;
        }

        @Override
        public PublicKeyCredentialCreationOptions createPublicKeyCredentialCreationOptions(
                PublicKeyCredentialCreationOptionsRequest request
        ) {
            return createDelegate().createPublicKeyCredentialCreationOptions(request);
        }

        @Override
        public CredentialRecord registerCredential(RelyingPartyRegistrationRequest request) {
            return createDelegate().registerCredential(request);
        }

        @Override
        public PublicKeyCredentialRequestOptions createCredentialRequestOptions(
                PublicKeyCredentialRequestOptionsRequest request
        ) {
            return createDelegate().createCredentialRequestOptions(request);
        }

        @Override
        public PublicKeyCredentialUserEntity authenticate(RelyingPartyAuthenticationRequest request) {
            return createDelegate().authenticate(request);
        }

    }

}
