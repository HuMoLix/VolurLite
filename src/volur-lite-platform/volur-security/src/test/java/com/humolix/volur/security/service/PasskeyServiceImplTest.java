package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.impl.PasskeyServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasskeyServiceImplTest {

    private TransitionLockManager transitionLockManager;
    private WebAuthnRelyingPartyOperations rpOperations;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private PasskeyServiceImpl passkeyService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        rpOperations = Mockito.mock(WebAuthnRelyingPartyOperations.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        objectMapper = JsonMapper.builder()
                .addModule(new WebauthnJacksonModule())
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        passkeyService = new PasskeyServiceImpl(
                transitionLockManager,
                rpOperations,
                redisTemplate,
                objectMapper
        );
    }

    @Test
    void getCreationOptions_success_returnsOptionsAndCachesInRedis() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-OPTIONS";

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .identifier("passkeyuser")
                .currentStep("pt/options")
                .build();

        given(transitionLockManager.getSession(dsh)).willReturn(session);
        given(transitionLockManager.getMetadata(dsh, "username")).willReturn("passkeyuser");

        PublicKeyCredentialCreationOptions options = PublicKeyCredentialCreationOptions.builder()
                .rp(PublicKeyCredentialRpEntity.builder().id("localhost").name("Volur Lite").build())
                .user(ImmutablePublicKeyCredentialUserEntity.builder()
                        .id(new Bytes("123".getBytes(StandardCharsets.UTF_8)))
                        .name("passkeyuser")
                        .displayName("passkeyuser")
                        .build())
                .challenge(new Bytes(new byte[32]))
                .pubKeyCredParams(List.of(PublicKeyCredentialParameters.ES256))
                .build();
        given(rpOperations.createPublicKeyCredentialCreationOptions(any())).willReturn(options);

        PublicKeyCredentialCreationOptions result = passkeyService.getCreationOptions(dsh, tl, "windows_password_manager");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("localhost", result.getRp().getId());
        verify(valueOperations).set(eq("vredis:security:passkey:creation:" + dsh), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void registerPasskey_success_registersAndReturnsVO() throws Exception {
        String dsh = "S-100200:1790000";
        String tl = "ADG-OPTIONS";

        PublicKeyCredentialCreationOptions options = PublicKeyCredentialCreationOptions.builder()
                .rp(PublicKeyCredentialRpEntity.builder().id("localhost").name("Volur Lite").build())
                .user(ImmutablePublicKeyCredentialUserEntity.builder()
                        .id(new Bytes("123".getBytes(StandardCharsets.UTF_8)))
                        .name("passkeyuser")
                        .displayName("passkeyuser")
                        .build())
                .challenge(new Bytes(new byte[32]))
                .pubKeyCredParams(List.of(PublicKeyCredentialParameters.ES256))
                .build();

        byte[] serialized;
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
            oos.writeObject(options);
            oos.flush();
            serialized = bos.toByteArray();
        }
        String base64 = java.util.Base64.getEncoder().encodeToString(serialized);
        given(valueOperations.get("vredis:security:passkey:creation:" + dsh)).willReturn(base64);

        Map<String, Object> credentialMap = Map.of(
                "id", "ARs287-test-id",
                "rawId", "ARs287-test-id",
                "response", Map.of(
                        "attestationObject", "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVjESZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NBAAAAAAAA",
                        "clientDataJSON", "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUEiLCJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0OjUxNzMifQ",
                        "transports", List.of("internal")
                ),
                "type", "public-key",
                "clientExtensionResults", Map.of()
        );

        PasskeyRegisterRequestDTO requestDTO = new PasskeyRegisterRequestDTO("我的指纹密钥", credentialMap);

        CredentialRecord mockRecord = mock(CredentialRecord.class);
        given(mockRecord.getCredentialId()).willReturn(new Bytes("ARs287-test-id".getBytes(StandardCharsets.UTF_8)));

        given(rpOperations.registerCredential(any())).willReturn(mockRecord);

        PasskeyRegisterResponseVO response = passkeyService.registerPasskey(dsh, tl, requestDTO);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isSuccess());
        verify(redisTemplate).delete("vredis:security:passkey:creation:" + dsh);
        verify(transitionLockManager).setMetadata(dsh, "passkeyRegistered", true);
    }

    @Test
    void creationOptions_serializationRoundTrip_success() throws Exception {
        PublicKeyCredentialCreationOptions options = PublicKeyCredentialCreationOptions.builder()
                .rp(PublicKeyCredentialRpEntity.builder().id("localhost").name("Volur Lite").build())
                .user(ImmutablePublicKeyCredentialUserEntity.builder()
                        .id(new Bytes("123".getBytes(StandardCharsets.UTF_8)))
                        .name("passkeyuser")
                        .displayName("passkeyuser")
                        .build())
                .challenge(new Bytes(new byte[32]))
                .pubKeyCredParams(List.of(PublicKeyCredentialParameters.ES256))
                .timeout(Duration.ofMinutes(2))
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .build())
                .build();

        byte[] serialized;
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
            oos.writeObject(options);
            oos.flush();
            serialized = bos.toByteArray();
        }
        Assertions.assertTrue(serialized.length > 0);

        PublicKeyCredentialCreationOptions deserialized;
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(serialized);
             java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bis)) {
            ois.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(
                    "org.springframework.security.web.webauthn.**;java.base/*;!*"
            ));
            deserialized = (PublicKeyCredentialCreationOptions) ois.readObject();
        }

        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals("localhost", deserialized.getRp().getId());
        Assertions.assertEquals("passkeyuser", deserialized.getUser().getName());
        Assertions.assertNotNull(deserialized.getChallenge());
    }

}
