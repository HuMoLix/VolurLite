package com.humolix.volur.user.service.impl;

import com.humolix.volur.user.domain.entity.UserTrustedDevice;
import com.humolix.volur.user.repository.UserTrustedDeviceRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserTrustedDeviceServiceImplTest {

    @Mock
    private UserTrustedDeviceRepository repository;

    private UserTrustedDeviceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserTrustedDeviceServiceImpl(repository);
    }

    @Test
    void isTrustedDevice_deviceExistsAndTrusted_shouldReturnTrue() {
        UserTrustedDevice device = UserTrustedDevice.builder()
                .userId(1001L)
                .deviceHash("hash_abc")
                .isTrusted(true)
                .build();

        given(repository.findByUserIdAndDeviceHash(eq(1001L), eq("hash_abc"))).willReturn(Optional.of(device));

        boolean trusted = service.isTrustedDevice(1001L, "hash_abc");
        Assertions.assertTrue(trusted);
    }

    @Test
    void isTrustedDevice_deviceNotFound_shouldReturnFalse() {
        given(repository.findByUserIdAndDeviceHash(eq(1001L), eq("hash_unknown"))).willReturn(Optional.empty());

        boolean trusted = service.isTrustedDevice(1001L, "hash_unknown");
        Assertions.assertFalse(trusted);
    }

    @Test
    void recordLoginDevice_newDevice_shouldSaveNewRecord() {
        given(repository.findByUserIdAndDeviceHash(eq(1001L), eq("hash_new"))).willReturn(Optional.empty());

        service.recordLoginDevice(1001L, "hash_new", "WINDOWS", "CHROME", "192.168.1.1");

        ArgumentCaptor<UserTrustedDevice> captor = ArgumentCaptor.forClass(UserTrustedDevice.class);
        verify(repository).save(captor.capture());

        UserTrustedDevice saved = captor.getValue();
        Assertions.assertEquals(1001L, saved.getUserId());
        Assertions.assertEquals("hash_new", saved.getDeviceHash());
        Assertions.assertEquals("WINDOWS", saved.getOsFamily());
        Assertions.assertEquals("CHROME", saved.getBrowserFamily());
        Assertions.assertEquals("192.168.1.1", saved.getFirstLoginIp());
        Assertions.assertTrue(saved.getIsTrusted());
    }

    @Test
    void recordLoginDevice_existingDevice_shouldUpdateLastLogin() {
        UserTrustedDevice existing = UserTrustedDevice.builder()
                .userId(1001L)
                .deviceHash("hash_old")
                .firstLoginIp("10.0.0.1")
                .isTrusted(true)
                .build();

        given(repository.findByUserIdAndDeviceHash(eq(1001L), eq("hash_old"))).willReturn(Optional.of(existing));

        service.recordLoginDevice(1001L, "hash_old", "MACOS", "SAFARI", "10.0.0.2");

        verify(repository).save(existing);
        Assertions.assertEquals("10.0.0.2", existing.getLastLoginIp());
        Assertions.assertEquals("MACOS", existing.getOsFamily());
    }

}
