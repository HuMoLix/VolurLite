package com.humolix.volur.system.service;

import com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO;
import com.humolix.volur.system.domain.vo.AdminSystemOverviewVO;
import com.humolix.volur.system.domain.vo.SystemSettingVO;

import java.util.List;
import java.util.Map;

public interface SystemSettingService {

    String getSettingValue(String key, String defaultValue);

    default String getSettingValue(String key) {
        return getSettingValue(key, null);
    }

    boolean getBoolean(String key, boolean defaultValue);

    int getInt(String key, int defaultValue);

    long getLong(String key, long defaultValue);

    Map<String, String> getPublicSettingsMap();

    List<SystemSettingVO> getAllSettings();

    AdminSystemOverviewVO getSystemOverview();

    void updateSetting(SystemSettingUpdateDTO dto);

    void updateSettings(List<SystemSettingUpdateDTO> dtoList);

    void evictCache(String key);

    void evictLocalCache(String key);

}
