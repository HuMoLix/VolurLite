package com.humolix.volur.security.domain.session;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDeviceParams implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String ip;

    private String deviceId;

    @NotBlank(message = "设备指纹不能为空")
    private String fingerprint;

    @NotBlank(message = "设备哈希不能为空")
    private String deviceHash;

    private String deviceToken;

    private String canvasHash;

    private String browserFamily;

    private String osFamily;

    private String renderer;

    private String vendor;

    @Builder.Default
    private Boolean isHeadless = false;

    private String screenResolution;

    private Integer colorDepth;

    private String timezone;

    private String language;

    private String platform;

    @Builder.Default
    private Map<String, Object> extraParams = new HashMap<>();

    public static RequestDeviceParams of(String ip, String fingerprint) {
        return RequestDeviceParams.builder()
                .ip(ip)
                .fingerprint(fingerprint)
                .deviceHash(fingerprint)
                .build();
    }

    public static RequestDeviceParams of(String ip, String fingerprint, String deviceId) {
        return RequestDeviceParams.builder()
                .ip(ip)
                .fingerprint(fingerprint)
                .deviceHash(fingerprint)
                .deviceId(deviceId)
                .build();
    }

}
