package com.humolix.volur;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.humolix.volur")
@MapperScan("com.humolix.volur.**.mapper")
public class VolurLiteApplication {

    private static final Logger log = LoggerFactory.getLogger(VolurLiteApplication.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ConfigurableApplicationContext context = SpringApplication.run(VolurLiteApplication.class, args);
        Environment env = context.getEnvironment();
        
        logStartupBanner(env, System.currentTimeMillis() - startTime);
    }

    private static void logStartupBanner(Environment env, long elapsedMs) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        if (!contextPath.endsWith("/")) {
            contextPath += "/";
        }

        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Unable to resolve local host address: {}", e.getMessage());
        }

        String activeProfiles = String.join(", ", env.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = "default";
        }

        log.info("""
                
                --------------------------------------------------------------------------------
                \tApplication '{}' is running successfully!
                \tActive Profile(s) : {}
                \tStartup Duration  : {} ms
                \tLocal URL         : {}://localhost:{}{}
                \tExternal URL      : {}://{}:{}{}
                \tDoc (Knife4j)     : {}://localhost:{}{}doc.html
                \tSwagger UI        : {}://localhost:{}{}swagger-ui/index.html
                --------------------------------------------------------------------------------
                """,
                env.getProperty("spring.application.name", "volur-server"),
                activeProfiles,
                elapsedMs,
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                protocol, serverPort, contextPath,
                protocol, serverPort, contextPath
        );
    }
}
