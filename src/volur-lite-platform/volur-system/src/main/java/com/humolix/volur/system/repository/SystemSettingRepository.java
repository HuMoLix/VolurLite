package com.humolix.volur.system.repository;

import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Long> {

    Optional<SystemSettingEntity> findBySettingKey(String settingKey);

    List<SystemSettingEntity> findByIsPublicTrue();

    List<SystemSettingEntity> findBySettingGroup(String settingGroup);

    boolean existsBySettingKey(String settingKey);

}
