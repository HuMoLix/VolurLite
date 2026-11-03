package com.humolix.volur.user.service.impl;

import com.humolix.volur.user.domain.entity.UserTrustedDevice;
import com.humolix.volur.user.repository.UserTrustedDeviceRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTrustedDeviceServiceImpl implements UserTrustedDeviceService {

    private final UserTrustedDeviceRepository userTrustedDeviceRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isTrustedDevice(Long userId, String deviceHash) {
        if (userId == null || !StringUtils.hasText(deviceHash)) {
            return false;
        }
        return userTrustedDeviceRepository.findByUserIdAndDeviceHash(userId, deviceHash.trim())
                .map(device -> Boolean.TRUE.equals(device.getIsTrusted()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void recordLoginDevice(Long userId, String deviceHash, String osFamily, String browserFamily, String ip) {
        if (userId == null || !StringUtils.hasText(deviceHash)) {
            return;
        }

        String cleanHash = deviceHash.trim();
        Instant now = Instant.now();
        Optional<UserTrustedDevice> existingOpt = userTrustedDeviceRepository.findByUserIdAndDeviceHash(userId, cleanHash);

        if (existingOpt.isPresent()) {
            UserTrustedDevice existing = existingOpt.get();
            existing.setLastLoginIp(ip);
            existing.setLastLoginTime(now);
            if (StringUtils.hasText(osFamily)) {
                existing.setOsFamily(osFamily);
            }
            if (StringUtils.hasText(browserFamily)) {
                existing.setBrowserFamily(browserFamily);
            }
            userTrustedDeviceRepository.save(existing);
            return;
        }

        UserTrustedDevice newDevice = UserTrustedDevice.builder()
                .userId(userId)
                .deviceHash(cleanHash)
                .osFamily(osFamily)
                .browserFamily(browserFamily)
                .firstLoginIp(ip)
                .lastLoginIp(ip)
                .firstLoginTime(now)
                .lastLoginTime(now)
                .isTrusted(true)
                .build();

        userTrustedDeviceRepository.save(newDevice);
    }

}
