package com.humolix.volur.auth.service;

public interface TokenService {

    boolean isUserForceLoggedOut(Long userId, long tokenIat);

    boolean isTokenBlacklisted(String token);

    void blacklistToken(String token, long expirationMs);

    void forceLogoutUser(Long userId);

    void refreshToken();

    void logout();

}
