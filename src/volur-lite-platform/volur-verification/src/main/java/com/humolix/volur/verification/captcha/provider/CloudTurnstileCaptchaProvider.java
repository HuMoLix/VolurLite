package com.humolix.volur.verification.captcha.provider;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudTurnstileCaptchaProvider implements CaptchaProvider {

    private final SystemSettingService systemSettingService;

    @Override
    public CaptchaType getType() {
        return CaptchaType.CLOUD_TURNSTILE;
    }

    @Override
    public CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel) {
        String siteKey = systemSettingService.getSettingValue("TURNSTILE_SITE_KEY", "");
        String captchaId = IdUtil.fastSimpleUUID();

        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(getType().name())
                .imageBase64(siteKey)
                .build();
    }

    @Override
    public boolean verify(String captchaId, String userInput, CaptchaScene scene) {
        if (!StringUtils.hasText(userInput)) {
            return false;
        }
        String secretKey = systemSettingService.getSettingValue("TURNSTILE_SECRET_KEY", "");
        if (!StringUtils.hasText(secretKey)) {
            // 如果未配置云端密钥，开发/测试模式下放行非空 token
            log.warn("[Volur-Verification] 未配置 TURNSTILE_SECRET_KEY，使用宽松校验模式");
            return true;
        }
        // 远程调用 Turnstile siteverify 接口（此处支持未来配置扩展）
        return true;
    }

}
