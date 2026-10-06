package com.humolix.volur.user.service;

public interface UserTrustedDeviceService {

    boolean isTrustedDevice(Long userId, String deviceHash);

    void recordLoginDevice(Long userId, String deviceHash, String osFamily, String browserFamily, String ip);

}
