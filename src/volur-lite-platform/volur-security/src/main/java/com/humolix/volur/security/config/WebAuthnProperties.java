package com.humolix.volur.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "volur.security.webauthn")
public class WebAuthnProperties {

    private String rpId = "localhost";
    private String rpName = "Volur Lite";
    private List<String> allowedOrigins = List.of(
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
    );

}
