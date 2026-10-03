package com.humolix.volur.security.config;

import com.humolix.volur.security.webauthn.UserPublicKeyCredentialUserEntityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

import java.util.HashSet;
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
            WebAuthnProperties properties
    ) {
        PublicKeyCredentialRpEntity rpEntity = PublicKeyCredentialRpEntity.builder()
                .id(properties.getRpId())
                .name(properties.getRpName())
                .build();

        Set<String> allowedOrigins = new HashSet<>(properties.getAllowedOrigins());

        return new Webauthn4JRelyingPartyOperations(
                userEntityRepository,
                userCredentialRepository,
                rpEntity,
                allowedOrigins
        );
    }

}
