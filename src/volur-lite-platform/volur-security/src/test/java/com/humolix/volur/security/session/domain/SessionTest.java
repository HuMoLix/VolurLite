package com.humolix.volur.security.session.domain;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.flow.domain.Flow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class SessionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void session_propertiesAndNestedFlow_correct() {
        RequestHeader header = RequestHeader.builder()
                .userAgent("Mozilla/5.0")
                .accept("text/html,application/xhtml+xml")
                .acceptLanguage("zh-CN,zh;q=0.9,en;q=0.8")
                .secChUa("\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\"")
                .secChUaPlatform("\"Windows\"")
                .origin("https://volur.local")
                .headers(Map.of("x-custom-trace", "trace-999"))
                .build();

        RequestDeviceParams deviceParams = RequestDeviceParams.of("127.0.0.1", "fp-123456");

        Flow flow = Flow.builder()
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("init")
                .cid(0)
                .identifier("admin")
                .activeTL("ADG-INIT-TL")
                .completedSteps(List.of("start"))
                .build();

        Session session = Session.builder()
                .dsh("S-100000000:1000000")
                .requestHeader(header)
                .requestDeviceParams(deviceParams)
                .riskLevel("LOW")
                .flow(flow)
                .metadata(Map.of("k1", "v1"))
                .build();

        Assertions.assertEquals("S-100000000:1000000", session.getDsh());
        Assertions.assertNotNull(session.getRequestHeader());
        Assertions.assertEquals("Mozilla/5.0", session.getRequestHeader().getUserAgent());
        Assertions.assertEquals("zh-CN,zh;q=0.9,en;q=0.8", session.getRequestHeader().getAcceptLanguage());
        Assertions.assertEquals("\"Windows\"", session.getRequestHeader().getSecChUaPlatform());
        Assertions.assertEquals("trace-999", session.getRequestHeader().getHeader("x-custom-trace"));
        Assertions.assertNotNull(session.getRequestDeviceParams());
        Assertions.assertEquals("127.0.0.1", session.getRequestDeviceParams().getIp());
        Assertions.assertEquals("fp-123456", session.getRequestDeviceParams().getFingerprint());
        Assertions.assertEquals("LOW", session.getRiskLevel());
        Assertions.assertEquals("v1", session.getMetadata().get("k1"));
        Assertions.assertNotNull(session.getCreatedAt());
        Assertions.assertNotNull(session.getUpdatedAt());

        Assertions.assertNotNull(session.getFlow());
        Assertions.assertEquals("VlifWebLogin", session.getFlow().getFlowName());
        Assertions.assertEquals("WebServiceLogin", session.getFlow().getFlowEntry());
        Assertions.assertEquals("init", session.getFlow().getCurrentStep());
        Assertions.assertEquals(0, session.getFlow().getCid());
        Assertions.assertEquals("admin", session.getFlow().getIdentifier());
        Assertions.assertEquals("ADG-INIT-TL", session.getFlow().getActiveTL());
        Assertions.assertEquals(List.of("start"), session.getFlow().getCompletedSteps());
    }

    @Test
    void session_jsonSerializationDeserializationRoundtrip() throws Exception {
        RequestHeader header = RequestHeader.fromUserAgent("Mozilla/5.0");
        RequestDeviceParams deviceParams = RequestDeviceParams.of("10.0.0.1", "fp-serialization");

        Flow flow = Flow.builder()
                .flowName("VlifWebSignUp")
                .flowEntry("WebSignUp")
                .currentStep("email")
                .cid(1)
                .identifier("alice")
                .activeTL("ADG-TEST-TL")
                .completedSteps(List.of("init", "name"))
                .build();

        Session session = Session.builder()
                .dsh("S-999999999:88888888")
                .requestHeader(header)
                .requestDeviceParams(deviceParams)
                .riskLevel("MEDIUM")
                .flow(flow)
                .metadata(Map.of("testKey", "testVal"))
                .build();

        String json = objectMapper.writeValueAsString(session);
        Assertions.assertNotNull(json);

        Session restored = objectMapper.readValue(json, Session.class);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(session.getDsh(), restored.getDsh());
        Assertions.assertEquals("MEDIUM", restored.getRiskLevel());
        Assertions.assertEquals("testVal", restored.getMetadata().get("testKey"));
        Assertions.assertEquals("10.0.0.1", restored.getRequestDeviceParams().getIp());
        Assertions.assertEquals("fp-serialization", restored.getRequestDeviceParams().getFingerprint());

        Assertions.assertNotNull(restored.getFlow());
        Assertions.assertEquals("VlifWebSignUp", restored.getFlow().getFlowName());
        Assertions.assertEquals("WebSignUp", restored.getFlow().getFlowEntry());
        Assertions.assertEquals("email", restored.getFlow().getCurrentStep());
        Assertions.assertEquals(1, restored.getFlow().getCid());
        Assertions.assertEquals("alice", restored.getFlow().getIdentifier());
        Assertions.assertEquals("ADG-TEST-TL", restored.getFlow().getActiveTL());
        Assertions.assertEquals(List.of("init", "name"), restored.getFlow().getCompletedSteps());
    }

    @Test
    void session_defaultsInitialized() {
        Session session = Session.builder().build();
        Assertions.assertNotNull(session.getMetadata());
        Assertions.assertTrue(session.getMetadata().isEmpty());
        Assertions.assertNotNull(session.getCreatedAt());
        Assertions.assertNotNull(session.getUpdatedAt());
    }

}
