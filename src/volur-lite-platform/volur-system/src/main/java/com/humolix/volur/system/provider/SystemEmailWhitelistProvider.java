package com.humolix.volur.system.provider;

import com.humolix.volur.framework.common.validation.provider.EmailWhitelistProvider;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 基于系统配置服务的邮箱白名单后缀提供者实现
 */
@Component
@RequiredArgsConstructor
public class SystemEmailWhitelistProvider implements EmailWhitelistProvider {

    private final SystemSettingService systemSettingService;

    @Override
    public String getEmailWhitelistSuffixes() {
        return systemSettingService.getSettingValue(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, "");
    }
}
