package com.humolix.volur.framework.security;

import com.humolix.volur.framework.security.util.SecurityContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("测试未登录匿名上下文默认值 (id=0, username=anonymous, authenticated=false)")
    void testAnonymousContext() {
        CurrentUserSchema schema = SecurityContextUtil.getCurrentUserSchema();

        assertThat(schema).isNotNull();
        assertThat(schema.getId()).isEqualTo(0L);
        assertThat(schema.getUsername()).isEqualTo("anonymous");
        assertThat(schema.isAuthenticated()).isFalse();
        assertThat(schema.getIp()).isNotBlank();
        assertThat(schema.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("测试已认证登录用户的上下文获取")
    void testAuthenticatedContext() {
        UserIdentity identity = new UserIdentity() {
            @Override
            public Long getUserId() {
                return 8848L;
            }

            @Override
            public String getUsername() {
                return "test-admin";
            }
        };

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                identity,
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        CurrentUserSchema schema = SecurityContextUtil.getCurrentUserSchema();

        assertThat(schema.getId()).isEqualTo(8848L);
        assertThat(schema.getUsername()).isEqualTo("test-admin");
        assertThat(schema.isAuthenticated()).isTrue();
        assertThat(schema.getRoles()).containsExactly("ROLE_ADMIN");
        assertThat(SecurityContextUtil.getCurrentUserId()).isEqualTo(8848L);
        assertThat(SecurityContextUtil.isAuthenticated()).isTrue();
    }
}
