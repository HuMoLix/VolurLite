package com.humolix.volur.auth.security;

import com.humolix.volur.auth.config.SecurityConfig;
import com.humolix.volur.auth.security.crypto.Sm3AuthProvider;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.exception.SecurityAccessDeniedHandler;
import com.humolix.volur.auth.security.exception.SecurityAuthenticationEntryPoint;
import com.humolix.volur.auth.security.filter.CsrfProtectionFilter;
import com.humolix.volur.auth.security.filter.MaintenanceModeFilter;
import com.humolix.volur.auth.security.filter.RiskIpFilter;
import com.humolix.volur.auth.security.jwt.JwtAuthFilter;
import com.humolix.volur.auth.security.util.SecurityWhitelistMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityFilterChainTest {

    @Mock
    private Sm3AuthProvider sm3AuthProvider;

    @Mock
    private Sm3PasswordEncoder sm3PasswordEncoder;

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private CsrfProtectionFilter csrfProtectionFilter;

    @Mock
    private MaintenanceModeFilter maintenanceModeFilter;

    @Mock
    private RiskIpFilter riskIpFilter;

    @Mock
    private SecurityAccessDeniedHandler securityAccessDeniedHandler;

    @Mock
    private SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    @Mock
    private SecurityWhitelistMatcher whitelistMatcher;

    @Test
    void shouldBuildSecurityFilterChainWithoutOrderExceptions() throws Exception {
        when(whitelistMatcher.getPatterns()).thenReturn(new String[]{"/api/v1/auth/**"});

        SecurityConfig securityConfig = new SecurityConfig(
                sm3AuthProvider,
                sm3PasswordEncoder,
                jwtAuthFilter,
                csrfProtectionFilter,
                maintenanceModeFilter,
                riskIpFilter,
                securityAccessDeniedHandler,
                securityAuthenticationEntryPoint,
                whitelistMatcher
        );

        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <T> T postProcess(T object) {
                return object;
            }
        };

        AuthenticationManagerBuilder authBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        org.springframework.context.support.GenericApplicationContext context = new org.springframework.context.support.GenericApplicationContext();
        context.registerBean("corsConfigurationSource", org.springframework.web.cors.CorsConfigurationSource.class, securityConfig::corsConfigurationSource);
        context.refresh();
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, context);

        HttpSecurity http = new HttpSecurity(objectPostProcessor, authBuilder, sharedObjects);
        SecurityFilterChain chain = securityConfig.securityFilterChain(http);

        assertThat(chain).isNotNull();
        assertThat(chain.getFilters()).isNotEmpty();

        int riskIndex = chain.getFilters().indexOf(riskIpFilter);
        int csrfIndex = chain.getFilters().indexOf(csrfProtectionFilter);
        int jwtIndex = chain.getFilters().indexOf(jwtAuthFilter);
        int maintenanceIndex = chain.getFilters().indexOf(maintenanceModeFilter);

        assertThat(riskIndex).isGreaterThanOrEqualTo(0);
        assertThat(riskIndex).isLessThan(csrfIndex);
        assertThat(csrfIndex).isLessThan(jwtIndex);
        assertThat(jwtIndex).isLessThan(maintenanceIndex);
    }

}
