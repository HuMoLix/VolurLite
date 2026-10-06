package com.humolix.volur.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "volur.security.webauthn")
public class WebAuthnProperties {

    private boolean enabled = true;
    private String rpId = "localhost";
    private String rpName = "Volur Lite";
    private Duration timeout = Duration.ofSeconds(60);
    private String userVerification = "preferred";
    private String residentKey = "preferred";
    private String attestation = "none";
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://localhost:8080",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:8080",
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://localhost:4173",
            "https://localhost:5173",
            "https://localhost:8080",
            "https://localhost:3001"
    ));

    public Set<String> resolveEffectiveAllowedOrigins(String dynamicOrigins, String frontendUrl) {
        Set<String> origins = new HashSet<>(this.allowedOrigins);
        if (StringUtils.hasText(dynamicOrigins)) {
            String[] parts = dynamicOrigins.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (StringUtils.hasText(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }
        if (StringUtils.hasText(frontendUrl)) {
            try {
                URI uri = URI.create(frontendUrl.trim());
                if (uri.getScheme() != null && uri.getHost() != null) {
                    int port = uri.getPort();
                    String origin = port > 0
                            ? uri.getScheme() + "://" + uri.getHost() + ":" + port
                            : uri.getScheme() + "://" + uri.getHost();
                    origins.add(origin);
                }
            } catch (Exception ignored) {
            }
        }
        return origins;
    }

    public String resolveEffectiveRpId(String dynamicRpId) {
        if (StringUtils.hasText(dynamicRpId)) {
            return dynamicRpId.trim();
        }
        return this.rpId;
    }

    public String resolveEffectiveRpName(String dynamicRpName) {
        if (StringUtils.hasText(dynamicRpName)) {
            return dynamicRpName.trim();
        }
        return this.rpName;
    }

    public Duration resolveEffectiveTimeout(Integer dynamicTimeoutSeconds) {
        if (dynamicTimeoutSeconds != null && dynamicTimeoutSeconds > 0) {
            return Duration.ofSeconds(dynamicTimeoutSeconds);
        }
        return this.timeout;
    }

    public String resolveEffectiveUserVerification(String dynamicUserVerification) {
        if (StringUtils.hasText(dynamicUserVerification)) {
            return dynamicUserVerification.trim().toLowerCase();
        }
        return this.userVerification;
    }

}
