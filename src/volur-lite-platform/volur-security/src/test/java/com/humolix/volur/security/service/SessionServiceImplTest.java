package com.humolix.volur.security.service;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.security.domain.dto.InitSessionRequestDTO;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.session.domain.vo.SessionVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.service.impl.FlowCryptoServiceImpl;
import com.humolix.volur.security.service.impl.SessionServiceImpl;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humolix.volur.framework.common.exception.BusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private TransitionLockManager transitionLockManager;

    @Mock
    private com.humolix.volur.security.risk.DeviceEnvironmentMatrixEvaluator deviceEnvironmentMatrixEvaluator;

    private FlowCryptoService flowCryptoService;
    private ObjectMapper objectMapper;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        flowCryptoService = new FlowCryptoServiceImpl();
        objectMapper = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sessionService = new SessionServiceImpl(sessionManager, transitionLockManager, flowCryptoService, deviceEnvironmentMatrixEvaluator, objectMapper, validator);
    }

    @Test
    void testInitSess_WithoutEncryptedData_ThrowsException() {
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String fp = "fingerprint_123";
        Long t = System.currentTimeMillis();

        InitSessionRequestDTO dto = new InitSessionRequestDTO(flowName, flowEntry, fp, t, null);

        assertThatThrownBy(() -> sessionService.initSess(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备参数解密或解析失败");
    }

    @Test
    void testInitSess_WithCorruptedEncryptedData_ThrowsException() {
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String fp = "fingerprint_123";
        Long t = System.currentTimeMillis();
        String corruptedData = "$V1$GCM$" + System.currentTimeMillis() + "$QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWY=";

        InitSessionRequestDTO dto = new InitSessionRequestDTO(flowName, flowEntry, fp, t, corruptedData);

        assertThatThrownBy(() -> sessionService.initSess(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void testInitSess_WithEncryptedData_PopulatesFullDeviceParams() throws Exception {
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String fp = "fingerprint_999";
        Long t = System.currentTimeMillis();
        String dsh = "S-200000:17900000";

        RequestDeviceParams payloadParams = RequestDeviceParams.builder()
                .fingerprint(fp)
                .deviceHash("hash_abc")
                .deviceToken("token_xyz")
                .browserFamily("CHROME")
                .osFamily("WINDOWS")
                .renderer("ANGLE (NVIDIA RTX 4090)")
                .vendor("Google Inc.")
                .isHeadless(false)
                .screenResolution("1920x1080")
                .colorDepth(24)
                .timezone("Asia/Shanghai")
                .language("zh-CN")
                .platform("Win32")
                .build();

        String rawJson = objectMapper.writeValueAsString(payloadParams);
        String encryptedData = flowCryptoService.encrypt(rawJson, flowName, flowEntry, null);

        InitSessionRequestDTO dto = new InitSessionRequestDTO(flowName, flowEntry, fp, t, encryptedData);

        Flow flow = Flow.builder()
                .flowName(flowName)
                .flowEntry(flowEntry)
                .cid(0)
                .build();

        Session mockSession = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        ArgumentCaptor<RequestDeviceParams> captor = ArgumentCaptor.forClass(RequestDeviceParams.class);
        given(sessionManager.createSession(eq(flowName), eq(flowEntry), nullable(RequestHeader.class), captor.capture()))
                .willReturn(mockSession);
        given(deviceEnvironmentMatrixEvaluator.evaluate(any(), any()))
                .willReturn(com.humolix.volur.verification.risk.enums.RiskLevel.LOW);
        given(transitionLockManager.createTransLock(eq(dsh), eq(0), eq("init"), isNull()))
                .willReturn("ADG-ACTIVE-TL");

        SessionVO result = sessionService.initSess(dto);

        assertThat(result).isNotNull();
        assertThat(result.getDsh()).isEqualTo(dsh);
        assertThat(result.getTl()).isEqualTo("ADG-ACTIVE-TL");

        RequestDeviceParams captured = captor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getFingerprint()).isEqualTo(fp);
        assertThat(captured.getDeviceToken()).isEqualTo("token_xyz");
        assertThat(captured.getBrowserFamily()).isEqualTo("CHROME");
        assertThat(captured.getOsFamily()).isEqualTo("WINDOWS");
        assertThat(captured.getRenderer()).isEqualTo("ANGLE (NVIDIA RTX 4090)");
        assertThat(captured.getVendor()).isEqualTo("Google Inc.");
        assertThat(captured.getScreenResolution()).isEqualTo("1920x1080");
        assertThat(captured.getColorDepth()).isEqualTo(24);
        assertThat(captured.getTimezone()).isEqualTo("Asia/Shanghai");

        verify(sessionManager).saveSession(mockSession);
    }

    @Test
    void testInitSess_WithMissingFingerprintInDecryptedPayload_ThrowsException() throws Exception {
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String fp = "fingerprint_xyz";
        Long t = System.currentTimeMillis();

        RequestDeviceParams payloadParams = RequestDeviceParams.builder()
                .deviceHash("hash_only")
                .build();

        String rawJson = objectMapper.writeValueAsString(payloadParams);
        String encryptedData = flowCryptoService.encrypt(rawJson, flowName, flowEntry, null);

        InitSessionRequestDTO dto = new InitSessionRequestDTO(flowName, flowEntry, fp, t, encryptedData);

        assertThatThrownBy(() -> sessionService.initSess(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备指纹不能为空");
    }

}
