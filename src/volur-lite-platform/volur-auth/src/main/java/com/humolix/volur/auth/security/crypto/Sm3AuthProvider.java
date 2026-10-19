package com.humolix.volur.auth.security.crypto;

import com.humolix.volur.auth.security.entity.UserDetails;
import com.humolix.volur.auth.security.service.UserDetailsService;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class Sm3AuthProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final Sm3PasswordEncoder sm3PasswordEncoder;
    private final UserDetailsChecker detailsChecker = new AccountStatusUserDetailsChecker();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        
        if (credentials == null || !StringUtils.hasText(credentials.toString())) {
            throw new BadCredentialsException("密码不能为空");
        }
        
        String rawPassword = credentials.toString();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        detailsChecker.check(userDetails);

        if (!sm3PasswordEncoder.matches(rawPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
