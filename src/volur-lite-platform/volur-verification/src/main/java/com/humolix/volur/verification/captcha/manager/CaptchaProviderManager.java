package com.humolix.volur.verification.captcha.manager;

import com.humolix.volur.verification.captcha.provider.CaptchaProvider;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CaptchaProviderManager {

    private final Map<CaptchaType, CaptchaProvider> providerMap = new EnumMap<>(CaptchaType.class);

    public CaptchaProviderManager(List<CaptchaProvider> providers) {
        for (CaptchaProvider provider : providers) {
            providerMap.put(provider.getType(), provider);
        }
    }

    public CaptchaProvider getProvider(CaptchaType type) {
        if (type == null) {
            type = CaptchaType.LINE;
        }
        CaptchaProvider provider = providerMap.get(type);
        if (provider == null) {
            provider = providerMap.get(CaptchaType.LINE);
        }
        return provider;
    }

}
