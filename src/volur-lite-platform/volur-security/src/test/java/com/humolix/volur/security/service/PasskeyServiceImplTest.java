package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.PasskeyAssertionVerifyRequestDTO;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyCredentialVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.service.impl.PasskeyServiceImpl;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasskeyServiceImplTest {

    private SessionManager sessionManager;
    private TransitionLockManager transitionLockManager;
    private WebAuthnRelyingPartyOperations rpOperations;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private UserRepository userRepository;
    private UserCredentialRepository userCredentialRepository;
    private UserTrustedDeviceService userTrustedDeviceService;
    private RiskEvaluationEngine riskEvaluationEngine;
    private SystemSettingService systemSettingService;
    private PasskeyServiceImpl passkeyService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sessionManager = Mockito.mock(SessionManager.class);
        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        rpOperations = Mockito.mock(WebAuthnRelyingPartyOperations.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        sessionDeviceBaselineValidator = Mockito.mock(SessionDeviceBaselineValidator.class);
        userRepository = Mockito.mock(UserRepository.class);
        userCredentialRepository = Mockito.mock(UserCredentialRepository.class);
        userTrustedDeviceService = Mockito.mock(UserTrustedDeviceService.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);

        given(systemSettingService.getBoolean(eq(SystemSettingKeys.WEBAUTHN_ENABLED), anyBoolean())).willReturn(true);
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.WEBAUTHN_USER_VERIFICATION), anyString())).willReturn("preferred");

        objectMapper = JsonMapper.builder()
                .addModule(new WebauthnJacksonModule())
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        passkeyService = new PasskeyServiceImpl(
                sessionManager,
                transitionLockManager,
                rpOperations,
                redisTemplate,
                objectMapper,
                sessionDeviceBaselineValidator,
                userRepository,
                userCredentialRepository,
                userTrustedDeviceService,
                riskEvaluationEngine,
                systemSettingService
        );
    }

    @Test
    void getCreationOptions_success_returnsOptionsAndCachesInRedis() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-OPTIONS";

        Flow flow = Flow.builder()
                .identifier("passkeyuser")
                .currentStep("pt/options")
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(sessionManager.getMetadata(dsh, "username")).willReturn("passkeyuser");

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
        verify(sessionManager).setMetadata(dsh, "passkeyRegistered", true);
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

    @Test
    void getAssertionOptions_success_returnsRequestOptionsAndCachesInRedis() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-ASSERTION";

        Flow flow = Flow.builder()
                .identifier("passkeyuser")
                .currentStep("pk")
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        User user = new User();
        user.setId(1001L);
        user.setUsername("passkeyuser");

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(userRepository.findByUsername("passkeyuser")).willReturn(Optional.of(user));

        PublicKeyCredentialDescriptor descriptor = PublicKeyCredentialDescriptor.builder()
                .id(new Bytes(new byte[16]))
                .build();
        PublicKeyCredentialRequestOptions options = PublicKeyCredentialRequestOptions.builder()
                .rpId("localhost")
                .challenge(new Bytes(new byte[32]))
                .allowCredentials(List.of(descriptor))
                .build();
        given(rpOperations.createCredentialRequestOptions(any())).willReturn(options);

        PublicKeyCredentialRequestOptions result = passkeyService.getAssertionOptions(dsh, tl);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("localhost", result.getRpId());
        verify(valueOperations).set(eq("vredis:security:passkey:assertion:" + dsh), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void getAssertionOptions_whenNoCredentials_throwsBadRequest() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-ASSERTION";

        Flow flow = Flow.builder()
                .identifier("passkeyuser")
                .currentStep("pk")
                .cid(1)
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        User user = new User();
        user.setId(1001L);
        user.setUsername("passkeyuser");

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(userRepository.findByUsername("passkeyuser")).willReturn(Optional.of(user));

        PublicKeyCredentialRequestOptions options = PublicKeyCredentialRequestOptions.builder()
                .rpId("localhost")
                .challenge(new Bytes(new byte[32]))
                .allowCredentials(List.of())
                .build();
        given(rpOperations.createCredentialRequestOptions(any())).willReturn(options);

        Assertions.assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class, () ->
                passkeyService.getAssertionOptions(dsh, tl)
        );
    }

    @Test
    void verifyPasskeyAssertion_success_authenticatesAndReturnsVO() throws Exception {
        String dsh = "S-100200:1790000";
        String tl = "ADG-ASSERTION";

        Flow flow = Flow.builder()
                .identifier("passkeyuser")
                .currentStep("pk")
                .cid(1)
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        User user = new User();
        user.setId(1001L);
        user.setUsername("passkeyuser");

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(userRepository.findByUsername("passkeyuser")).willReturn(Optional.of(user));

        PublicKeyCredentialRequestOptions options = PublicKeyCredentialRequestOptions.builder()
                .rpId("localhost")
                .challenge(new Bytes(new byte[32]))
                .build();

        byte[] serialized;
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
            oos.writeObject(options);
            oos.flush();
            serialized = bos.toByteArray();
        }
        String base64 = java.util.Base64.getEncoder().encodeToString(serialized);
        given(valueOperations.get("vredis:security:passkey:assertion:" + dsh)).willReturn(base64);

        String dummyAuthData = java.util.Base64.getEncoder().encodeToString(new byte[37]);
        String dummySig = java.util.Base64.getEncoder().encodeToString("dummy_signature".getBytes(StandardCharsets.UTF_8));
        String dummyClientData = java.util.Base64.getEncoder().encodeToString("{\"type\":\"webauthn.get\",\"challenge\":\"AAAA\",\"origin\":\"http://localhost:5173\"}".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> credentialMap = Map.of(
                "id", "ARs287-assertion-id",
                "rawId", java.util.Base64.getEncoder().encodeToString("ARs287-assertion-id".getBytes(StandardCharsets.UTF_8)),
                "response", Map.of(
                        "authenticatorData", dummyAuthData,
                        "clientDataJSON", dummyClientData,
                        "signature", dummySig
                ),
                "type", "public-key",
                "clientExtensionResults", Map.of()
        );

        PasskeyAssertionVerifyRequestDTO request = PasskeyAssertionVerifyRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(1)
                .credential(credentialMap)
                .rememberMe(true)
                .build();

        given(transitionLockManager.nextTransLock(eq(dsh), eq(tl), eq(1), eq("pk"), eq(2), eq("authenticate"), eq("passkeyuser")))
                .willReturn("ADG-NEXT-TL");

        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(Long.BYTES).putLong(1001L);
        PublicKeyCredentialUserEntity userEntity = ImmutablePublicKeyCredentialUserEntity.builder()
                .id(new Bytes(buffer.array()))
                .name("passkeyuser")
                .displayName("passkeyuser")
                .build();
        given(rpOperations.authenticate(any())).willReturn(userEntity);

        FlowResponseVO response = passkeyService.verifyPasskeyAssertion(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("authenticate", response.getNextStep());
        Assertions.assertEquals("ADG-NEXT-TL", response.getTl());
        verify(redisTemplate).delete("vredis:security:passkey:assertion:" + dsh);
        verify(rpOperations).authenticate(any());
    }

    @Test
    void verifyPasskeyAssertion_whenUserMismatch_throwsUnauthorized() throws Exception {
        String dsh = "S-100200:1790000";
        String tl = "ADG-ASSERTION";

        Flow flow = Flow.builder()
                .identifier("passkeyuser")
                .currentStep("pk")
                .cid(1)
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        User user = new User();
        user.setId(1001L);
        user.setUsername("passkeyuser");

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(userRepository.findByUsername("passkeyuser")).willReturn(Optional.of(user));

        PublicKeyCredentialRequestOptions options = PublicKeyCredentialRequestOptions.builder()
                .rpId("localhost")
                .challenge(new Bytes(new byte[32]))
                .build();

        byte[] serialized;
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
            oos.writeObject(options);
            oos.flush();
            serialized = bos.toByteArray();
        }
        String base64 = java.util.Base64.getEncoder().encodeToString(serialized);
        given(valueOperations.get("vredis:security:passkey:assertion:" + dsh)).willReturn(base64);

        Map<String, Object> credentialMap = Map.of(
                "id", "ARs287-assertion-id",
                "rawId", java.util.Base64.getEncoder().encodeToString("ARs287-assertion-id".getBytes(StandardCharsets.UTF_8)),
                "response", Map.of(
                        "authenticatorData", java.util.Base64.getEncoder().encodeToString(new byte[37]),
                        "clientDataJSON", java.util.Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8)),
                        "signature", java.util.Base64.getEncoder().encodeToString("sig".getBytes(StandardCharsets.UTF_8))
                ),
                "type", "public-key",
                "clientExtensionResults", Map.of()
        );

        PasskeyAssertionVerifyRequestDTO request = PasskeyAssertionVerifyRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(1)
                .credential(credentialMap)
                .rememberMe(true)
                .build();

        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(Long.BYTES).putLong(2002L);
        PublicKeyCredentialUserEntity otherUserEntity = ImmutablePublicKeyCredentialUserEntity.builder()
                .id(new Bytes(buffer.array()))
                .name("otheruser")
                .displayName("otheruser")
                .build();
        given(rpOperations.authenticate(any())).willReturn(otherUserEntity);

        Assertions.assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class, () ->
                passkeyService.verifyPasskeyAssertion(request)
        );
    }

    @Test
    void hasPasskey_whenHasCredentials_returnsTrue() {
        given(userCredentialRepository.findByUserId(any())).willReturn(List.of(Mockito.mock(CredentialRecord.class)));
        Assertions.assertTrue(passkeyService.hasPasskey(1001L));
    }

    @Test
    void hasPasskey_whenNoCredentials_returnsFalse() {
        given(userCredentialRepository.findByUserId(any())).willReturn(List.of());
        Assertions.assertFalse(passkeyService.hasPasskey(1001L));
    }

    @Test
    void getSupportedAuthenticators_disabled_returnsEmptyList() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.WEBAUTHN_ENABLED), anyBoolean())).willReturn(false);

        List<PasskeyAuthenticatorOptionVO> list = passkeyService.getSupportedAuthenticators();

        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    void getCreationOptions_disabled_throwsServiceUnavailable() {
        given(systemSettingService.getBoolean(eq(SystemSettingKeys.WEBAUTHN_ENABLED), anyBoolean())).willReturn(false);

        Assertions.assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class, () ->
                passkeyService.getCreationOptions("dsh", "tl", "windows_hello")
        );
    }

    @Test
    void listCredentials_whenValidUser_returnsList() {
        CredentialRecord mockRecord = Mockito.mock(CredentialRecord.class);
        Bytes credId = new Bytes("test-cred-id".getBytes(StandardCharsets.UTF_8));
        given(mockRecord.getCredentialId()).willReturn(credId);
        given(mockRecord.getLabel()).willReturn("My Passkey");
        given(userCredentialRepository.findByUserId(any())).willReturn(List.of(mockRecord));

        List<PasskeyCredentialVO> list = passkeyService.listCredentials(1001L);

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("My Passkey", list.get(0).getLabel());
        Assertions.assertEquals(credId.toBase64UrlString(), list.get(0).getCredentialId());
    }

    @Test
    void listCredentials_whenNullUser_throwsUnauthorized() {
        Assertions.assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class, () ->
                passkeyService.listCredentials(null)
        );
    }

    @Test
    void deleteCredential_whenFound_deletesSuccessfully() {
        CredentialRecord mockRecord = Mockito.mock(CredentialRecord.class);
        Bytes credId = new Bytes("test-cred-id".getBytes(StandardCharsets.UTF_8));
        given(mockRecord.getCredentialId()).willReturn(credId);
        given(userCredentialRepository.findByUserId(any())).willReturn(List.of(mockRecord));

        passkeyService.deleteCredential(1001L, credId.toBase64UrlString());

        verify(userCredentialRepository).delete(credId);
    }

    @Test
    void deleteCredential_whenNotFound_throwsNotFound() {
        given(userCredentialRepository.findByUserId(any())).willReturn(List.of());

        Assertions.assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class, () ->
                passkeyService.deleteCredential(1001L, "non-existent")
        );
    }

}
