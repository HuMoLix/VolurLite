package com.humolix.volur.auth.config;

import com.humolix.volur.auth.security.crypto.Sm3AuthProvider;
import com.humolix.volur.auth.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.exception.SecurityAccessDeniedHandler;
import com.humolix.volur.auth.security.exception.SecurityAuthenticationEntryPoint;
import com.humolix.volur.auth.security.filter.MaintenanceModeFilter;
import com.humolix.volur.auth.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Sm3AuthProvider sm3AuthProvider;
    private final Sm3PasswordEncoder sm3PasswordEncoder;
    private final JwtAuthFilter jwtAuthFilter;
    private final MaintenanceModeFilter maintenanceModeFilter;
    private final SecurityAccessDeniedHandler securityAccessDeniedHandler;
    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(sm3AuthProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/password-resets",
                                "/api/v1/auth/reset-password-confirm",
                                "/api/v1/auth/refresh",
                                "/api/v1/captcha/**",
                                "/api/v1/email/**",
                                "/api/v1/verification/**",
                                "/api/v1/rank/**",
                                "/api/v1/broadcasts/**",
                                "/api/v1/competitions",
                                "/api/v1/competitions/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/ws/**", "/ws/**", "/api/v1/terminal/**", "/api/v1/realtime/stream").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/", "/index.html", "/static/**", "/api/v1/uploads/**", "/api/v1/system/settings/public").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(securityAuthenticationEntryPoint)
                        .accessDeniedHandler(securityAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(maintenanceModeFilter, JwtAuthFilter.class);
        return http.build();
    }

}
