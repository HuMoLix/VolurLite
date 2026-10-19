package com.humolix.volur.security.session;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.session.generator.DeviceSessionHashGenerator;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.session.domain.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DeviceSessionHashGenerator deviceSessionHashGenerator;

    private ObjectMapper objectMapper;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sessionManager = new SessionManager(redisTemplate, deviceSessionHashGenerator, objectMapper);
    }

    @Test
    void createSession_shouldInitializeSessionAndSaveRedis() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(deviceSessionHashGenerator.generate()).willReturn("S-123456789:987654321");

        RequestHeader requestHeader = RequestHeader.fromUserAgent("Mozilla/5.0");
        RequestDeviceParams deviceParams = RequestDeviceParams.of("127.0.0.1", "fp-123");

        Session session = sessionManager.createSession("VlifWebLogin", "WebServiceLogin", requestHeader, deviceParams);

        Assertions.assertNotNull(session);
        Assertions.assertEquals("S-123456789:987654321", session.getDsh());
        Assertions.assertNotNull(session.getFlow());
        Assertions.assertEquals("VlifWebLogin", session.getFlow().getFlowName());
        Assertions.assertEquals("WebServiceLogin", session.getFlow().getFlowEntry());
        Assertions.assertEquals("init", session.getFlow().getCurrentStep());
        Assertions.assertEquals(0, session.getFlow().getCid());
        Assertions.assertEquals(requestHeader, session.getRequestHeader());
        Assertions.assertEquals(deviceParams, session.getRequestDeviceParams());

        verify(valueOperations).set(eq("vredis:security:flow:session:" + session.getDsh()), any(), eq(Duration.ofMinutes(15)));
    }

    @Test
    void recoverySession_shouldUpdateExistingSessionAndSaveRedis() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String dsh = "S-123456789:987654321";

        Flow existingFlow = Flow.builder()
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("init")
                .cid(0)
                .build();

        Session existing = Session.builder()
                .dsh(dsh)
                .flow(existingFlow)
                .createdAt(1000L)
                .updatedAt(1000L)
                .build();

        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(objectMapper.writeValueAsString(existing));

        RequestHeader newHeader = RequestHeader.fromUserAgent("Chrome/130");
        RequestDeviceParams newParams = RequestDeviceParams.of("192.168.1.1", "fp-456");

        Session recovered = sessionManager.recoverySession(dsh, "VlifWebSignUp", "WebSignUp", newHeader, newParams);

        Assertions.assertEquals(dsh, recovered.getDsh());
        Assertions.assertEquals("VlifWebSignUp", recovered.getFlow().getFlowName());
        Assertions.assertEquals("WebSignUp", recovered.getFlow().getFlowEntry());
        Assertions.assertEquals(newHeader, recovered.getRequestHeader());
        Assertions.assertEquals(newParams, recovered.getRequestDeviceParams());
        Assertions.assertTrue(recovered.getUpdatedAt() >= 1000L);

        verify(valueOperations).set(eq("vredis:security:flow:session:" + dsh), any(), eq(Duration.ofMinutes(15)));
    }

    @Test
    void createSession_withInvalidFlowOrEntry_shouldThrowBadRequest() {
        RequestHeader requestHeader = RequestHeader.fromUserAgent("Mozilla/5.0");
        RequestDeviceParams deviceParams = RequestDeviceParams.of("127.0.0.1", "fp-123");

        Assertions.assertThrows(BusinessException.class, () ->
                sessionManager.createSession("VlifWebLogin", "WebSignUp", requestHeader, deviceParams)
        );
        Assertions.assertThrows(BusinessException.class, () ->
                sessionManager.createSession("UnknownFlow", "WebServiceLogin", requestHeader, deviceParams)
        );
    }

    @Test
    void recoverySession_withInvalidFlowOrEntry_shouldThrowBadRequest() throws Exception {
        String dsh = "S-123456789:987654321";
        Flow existingFlow = Flow.builder()
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("init")
                .cid(0)
                .build();
        Session existing = Session.builder()
                .dsh(dsh)
                .flow(existingFlow)
                .build();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(objectMapper.writeValueAsString(existing));

        Assertions.assertThrows(BusinessException.class, () ->
                sessionManager.recoverySession(dsh, "VlifWebLogin", "WebSignUp", null, null)
        );

        Assertions.assertThrows(BusinessException.class, () ->
                sessionManager.recoverySession(dsh, "InvalidFlow", "WebServiceLogin", null, null)
        );
    }

    @Test
    void recoverySession_withMissingDsh_shouldThrowUnauthorized() {
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () ->
                sessionManager.recoverySession(null, null, null, null, null)
        );
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void getSession_whenAbsent_shouldThrowUnauthorized() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("vredis:security:flow:session:absent-dsh")).willReturn(null);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () ->
                sessionManager.getSession("absent-dsh")
        );
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void setMetadataAndGetMetadata_shouldPersistAndRetrieve() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String dsh = "S-999:888";

        Session existing = Session.builder()
                .dsh(dsh)
                .flow(Flow.builder().build())
                .build();

        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(objectMapper.writeValueAsString(existing));

        sessionManager.setMetadata(dsh, "email", "test@volur.local");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("vredis:security:flow:session:" + dsh), captor.capture(), eq(Duration.ofMinutes(15)));

        Session saved = objectMapper.readValue(captor.getValue(), Session.class);
        Assertions.assertEquals("test@volur.local", saved.getMetadata().get("email"));
    }

    @Test
    void destroySession_shouldDeleteKeyFromRedis() {
        sessionManager.destroySession("S-123");
        verify(redisTemplate).delete("vredis:security:flow:session:S-123");
    }

}
