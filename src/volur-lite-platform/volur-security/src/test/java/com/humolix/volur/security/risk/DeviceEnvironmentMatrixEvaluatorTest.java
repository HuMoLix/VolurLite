package com.humolix.volur.security.risk;

import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

class DeviceEnvironmentMatrixEvaluatorTest {

    private SystemSettingService systemSettingService;
    private DeviceEnvironmentMatrixEvaluator evaluator;

    @BeforeEach
    void setUp() {
        systemSettingService = Mockito.mock(SystemSettingService.class);
        given(systemSettingService.getBoolean(anyString(), anyBoolean())).willReturn(true);
        evaluator = new DeviceEnvironmentMatrixEvaluator(systemSettingService);
    }

    @Test
    void evaluate_normalDesktopChrome_shouldReturnLow() {
        RequestDeviceParams params = RequestDeviceParams.builder()
                .fingerprint("fp_sample")
                .osFamily("WINDOWS")
                .platform("Win32")
                .renderer("ANGLE (NVIDIA, NVIDIA GeForce RTX 3080 Direct3D11)")
                .vendor("Google Inc. (NVIDIA)")
                .isHeadless(false)
                .screenResolution("1920x1080")
                .colorDepth(24)
                .build();

        RequestHeader header = RequestHeader.builder()
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36")
                .secChUaPlatform("\"Windows\"")
                .secChUaMobile("?0")
                .build();

        RiskLevel level = evaluator.evaluate(params, header);
        Assertions.assertEquals(RiskLevel.LOW, level);
    }

    @Test
    void evaluate_softwareRendererSwiftShader_shouldReturnHigh() {
        RequestDeviceParams params = RequestDeviceParams.builder()
                .fingerprint("fp_sample")
                .osFamily("WINDOWS")
                .renderer("Google SwiftShader")
                .screenResolution("1920x1080")
                .build();

        RiskLevel level = evaluator.evaluate(params, null);
        Assertions.assertEquals(RiskLevel.HIGH, level);
    }

    @Test
    void evaluate_headlessBrowser_shouldReturnHigh() {
        RequestDeviceParams params = RequestDeviceParams.builder()
                .fingerprint("fp_sample")
                .isHeadless(true)
                .screenResolution("1920x1080")
                .build();

        RiskLevel level = evaluator.evaluate(params, null);
        Assertions.assertEquals(RiskLevel.HIGH, level);
    }

    @Test
    void evaluate_crossDimensionInconsistency_windowsWithAppleGpu_shouldReturnHigh() {
        RequestDeviceParams params = RequestDeviceParams.builder()
                .fingerprint("fp_sample")
                .osFamily("WINDOWS")
                .platform("Win32")
                .renderer("Apple M2 Pro")
                .vendor("Apple")
                .screenResolution("1920x1080")
                .build();

        RiskLevel level = evaluator.evaluate(params, null);
        Assertions.assertEquals(RiskLevel.HIGH, level);
    }

    @Test
    void evaluate_physicalScreenAnomaly_800x600_shouldReturnHigh() {
        RequestDeviceParams params = RequestDeviceParams.builder()
                .fingerprint("fp_sample")
                .osFamily("WINDOWS")
                .screenResolution("800x600")
                .build();

        RiskLevel level = evaluator.evaluate(params, null);
        Assertions.assertEquals(RiskLevel.HIGH, level);
    }

}
