package com.humolix.volur.auth.config;

import com.humolix.volur.auth.security.crypto.Sm3AuthProvider;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.exception.SecurityAccessDeniedHandler;
import com.humolix.volur.auth.security.exception.SecurityAuthenticationEntryPoint;
import com.humolix.volur.auth.security.filter.CsrfProtectionFilter;
import com.humolix.volur.auth.security.filter.MaintenanceModeFilter;
import com.humolix.volur.auth.security.filter.RiskIpFilter;
import com.humolix.volur.auth.security.jwt.JwtAuthFilter;
import com.humolix.volur.auth.security.util.SecurityWhitelistMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import com.humolix.volur.framework.common.util.RequestUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Sm3AuthProvider sm3AuthProvider;
    private final Sm3PasswordEncoder sm3PasswordEncoder;
    private final JwtAuthFilter jwtAuthFilter;
    private final CsrfProtectionFilter csrfProtectionFilter;
    private final MaintenanceModeFilter maintenanceModeFilter;
    private final RiskIpFilter riskIpFilter;
    private final SecurityAccessDeniedHandler securityAccessDeniedHandler;
    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;
    private final SecurityWhitelistMatcher whitelistMatcher;

    @Value("${volur.security.cors.allowed-origins:}")
    private List<String> additionalAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return sm3PasswordEncoder;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPER_ADMIN").implies("ADMIN")
                .role("ADMIN").implies("USER")
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            String origin = request.getHeader("Origin");
            if (RequestUtil.isOriginAllowed(origin, request, additionalAllowedOrigins)) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(origin));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                return configuration;
            }
            return null;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .xssProtection(HeadersConfigurer.XXssConfig::disable)
                        .cacheControl(Customizer.withDefaults())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(sm3AuthProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(whitelistMatcher.getPatterns()).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(securityAuthenticationEntryPoint)
                        .accessDeniedHandler(securityAccessDeniedHandler)
                )
                .addFilterBefore(riskIpFilter, CsrfFilter.class)
                .addFilterAfter(csrfProtectionFilter, CsrfFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(maintenanceModeFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
