package com.humolix.volur.auth.security.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.framework.security.UserIdentity;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserDetails implements org.springframework.security.core.userdetails.UserDetails, UserIdentity, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    @JsonCreator
    public UserDetails(
            @JsonProperty("user")
            User user) {
        this.user = Objects.requireNonNull(user, "User entity must not be null");
        this.authorities = calculateAuthorities(user);
    }

    private Collection<? extends GrantedAuthority> calculateAuthorities(User user) {
        if (user.getRole() == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        String roleName = user.getRole().name();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    @NullMarked
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    @NullMarked
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != null && user.getStatus();
    }

    @Override
    @NullMarked
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetails that = (UserDetails) o;
        return Objects.equals(getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId());
    }

}
