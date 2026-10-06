package com.humolix.volur.security.risk;

import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DeviceEnvironmentMatrixEvaluator {

    private static final Set<String> SOFTWARE_RENDERER_KEYWORDS = Set.of(
            "swiftshader",
            "llvmpipe",
            "mesa offscreen",
            "virtualbox",
            "vmware",
            "parallels",
            "softpipe",
            "software rasterizer",
            "lavapipe"
    );

    private static final Set<String> AUTOMATION_UA_KEYWORDS = Set.of(
            "headlesschrome",
            "phantomjs",
            "puppeteer",
            "playwright",
            "selenium"
    );

    private final SystemSettingService systemSettingService;

    public RiskLevel evaluate(RequestDeviceParams deviceParams, RequestHeader requestHeader) {
        if (!isRiskEnabled()) {
            return RiskLevel.LOW;
        }

        if (deviceParams == null) {
            return RiskLevel.MEDIUM;
        }

        if (Boolean.TRUE.equals(deviceParams.getIsHeadless())) {
            return RiskLevel.HIGH;
        }

        if (isSoftwareOrVirtualRenderer(deviceParams.getRenderer(), deviceParams.getVendor())) {
            return RiskLevel.HIGH;
        }

        if (isAutomationUserAgent(requestHeader)) {
            return RiskLevel.HIGH;
        }

        if (hasCrossDimensionInconsistency(deviceParams, requestHeader)) {
            return RiskLevel.HIGH;
        }

        if (hasPhysicalScreenAnomaly(deviceParams.getScreenResolution())) {
            return RiskLevel.HIGH;
        }

        if (hasAbnormalColorDepth(deviceParams.getColorDepth())) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    private boolean isRiskEnabled() {
        return systemSettingService == null || systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true);
    }

    private boolean isSoftwareOrVirtualRenderer(String renderer, String vendor) {
        if (StringUtils.hasText(renderer)) {
            String lowerRenderer = renderer.toLowerCase();
            for (String keyword : SOFTWARE_RENDERER_KEYWORDS) {
                if (lowerRenderer.contains(keyword)) {
                    return true;
                }
            }
        }

        if (StringUtils.hasText(vendor)) {
            String lowerVendor = vendor.toLowerCase();
            if (lowerVendor.contains("brian paul") || lowerVendor.contains("mesa project")
                    || lowerVendor.contains("vmware") || lowerVendor.contains("parallels")) {
                return true;
            }
        }

        return false;
    }

    private boolean isAutomationUserAgent(RequestHeader header) {
        if (header == null || !StringUtils.hasText(header.getUserAgent())) {
            return false;
        }
        String lowerUa = header.getUserAgent().toLowerCase();
        for (String keyword : AUTOMATION_UA_KEYWORDS) {
            if (lowerUa.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCrossDimensionInconsistency(RequestDeviceParams params, RequestHeader header) {
        String osFamily = params.getOsFamily() != null ? params.getOsFamily().trim().toUpperCase() : "";
        String platform = params.getPlatform() != null ? params.getPlatform().trim().toLowerCase() : "";
        String renderer = params.getRenderer() != null ? params.getRenderer().trim().toLowerCase() : "";
        String vendor = params.getVendor() != null ? params.getVendor().trim().toLowerCase() : "";

        if (isWindowsEnvironment(osFamily, platform)) {
            if (renderer.contains("apple") || renderer.contains("metal") || vendor.contains("apple")) {
                return true;
            }
        }

        if (isMacEnvironment(osFamily, platform)) {
            if (renderer.contains("direct3d") || renderer.contains("nvidia") || vendor.contains("nvidia")) {
                return true;
            }
        }

        if (header != null && StringUtils.hasText(header.getSecChUaPlatform())) {
            String chPlatform = header.getSecChUaPlatform().replace("\"", "").trim().toLowerCase();
            if (chPlatform.contains("windows") && (osFamily.equals("MACOS") || osFamily.equals("LINUX"))) {
                return true;
            }
            if (chPlatform.contains("macos") && (osFamily.equals("WINDOWS") || osFamily.equals("LINUX"))) {
                return true;
            }
            if (chPlatform.contains("linux") && !chPlatform.contains("android") && (osFamily.equals("WINDOWS") || osFamily.equals("MACOS"))) {
                return true;
            }
        }

        if (header != null && "?1".equals(header.getSecChUaMobile())) {
            return osFamily.equals("WINDOWS") || osFamily.equals("MACOS");
        }

        return false;
    }

    private boolean isWindowsEnvironment(String osFamily, String platform) {
        return osFamily.contains("WINDOWS") || platform.contains("win");
    }

    private boolean isMacEnvironment(String osFamily, String platform) {
        return osFamily.contains("MAC") || platform.contains("mac");
    }

    private boolean hasPhysicalScreenAnomaly(String resolution) {
        if (!StringUtils.hasText(resolution)) {
            return false;
        }

        String trimmed = resolution.trim().toLowerCase();
        if ("0x0".equals(trimmed) || "800x600".equals(trimmed)) {
            return true;
        }

        String[] parts = trimmed.split("x");
        if (parts.length == 2) {
            try {
                int width = Integer.parseInt(parts[0].trim());
                int height = Integer.parseInt(parts[1].trim());
                return width <= 0 || height <= 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        return false;
    }

    private boolean hasAbnormalColorDepth(Integer colorDepth) {
        return colorDepth != null && (colorDepth <= 8 || colorDepth > 48);
    }

}
