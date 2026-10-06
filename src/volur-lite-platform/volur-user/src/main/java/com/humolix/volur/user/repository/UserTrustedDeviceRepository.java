package com.humolix.volur.user.repository;

import com.humolix.volur.user.domain.entity.UserTrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTrustedDeviceRepository extends JpaRepository<UserTrustedDevice, Long> {

    Optional<UserTrustedDevice> findByUserIdAndDeviceHash(Long userId, String deviceHash);

    List<UserTrustedDevice> findAllByUserId(Long userId);

}
