package com.humolix.volur.security.session.validator;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SessionDeviceBaselineValidator {

    private final RiskEvaluationEngine riskEvaluationEngine;

    public void validateBaseline(Session session) {
        if (session == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "会话不存在或已失效");
        }

        RequestDeviceParams baselineParams = session.getRequestDeviceParams();
        if (baselineParams == null) {
            return;
        }

        String rawToken = RequestUtil.getDeviceFingerprint();
        String currentDid = RequestUtil.getDeviceId();
        String currentFp = rawToken;

        if (StringUtils.hasText(rawToken)) {
            RiskEvaluationEngine.ParsedClientProfile profile = riskEvaluationEngine.parseDeviceToken(rawToken);
            if (profile != null) {
                if (StringUtils.hasText(profile.deviceHash())) {
                    currentFp = profile.deviceHash();
                }
                if (!StringUtils.hasText(currentDid) && StringUtils.hasText(profile.deviceId())) {
                    currentDid = profile.deviceId();
                }
            }
        }

        String baselineFp = baselineParams.getFingerprint();
        String baselineHash = baselineParams.getDeviceHash();
        String baselineDid = baselineParams.getDeviceId();

        if (StringUtils.hasText(currentFp) && StringUtils.hasText(baselineFp)) {
            boolean matched = currentFp.equals(baselineFp)
                    || (StringUtils.hasText(baselineHash) && currentFp.equals(baselineHash));

            if (!matched && StringUtils.hasText(currentDid)) {
                if ((StringUtils.hasText(baselineDid) && currentDid.equals(baselineDid))
                        || currentDid.replace("-", "").equalsIgnoreCase(baselineFp)
                        || (StringUtils.hasText(baselineHash) && currentDid.replace("-", "").equalsIgnoreCase(baselineHash))) {
                    matched = true;
                }
            }

            if (!matched) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "检测到设备环境异常突变，会话已终止");
            }
        }

        String currentIp = RequestUtil.getClientIp();
        String baselineIp = baselineParams.getIp();
        String currentUa = RequestUtil.getUserAgent();
        RequestHeader baselineHeader = session.getRequestHeader();
        String baselineUa = baselineHeader != null ? baselineHeader.getUserAgent() : null;

        if (StringUtils.hasText(currentIp) && StringUtils.hasText(baselineIp) && !currentIp.equals(baselineIp)) {
            if (StringUtils.hasText(currentUa) && StringUtils.hasText(baselineUa)) {
                if (!isSameBrowserFamily(currentUa, baselineUa)) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "会话安全基线校验失败，检测到异构环境访问");
                }
            }
        }
    }

    private boolean isSameBrowserFamily(String ua1, String ua2) {
        String family1 = extractBrowserFamily(ua1);
        String family2 = extractBrowserFamily(ua2);
        return family1.equals(family2);
    }

    private String extractBrowserFamily(String ua) {
        String lower = ua.toLowerCase();
        if (lower.contains("edg/")) {
            return "EDGE";
        }
        if (lower.contains("firefox") || lower.contains("fxios")) {
            return "FIREFOX";
        }
        if (lower.contains("safari") && !lower.contains("chrome") && !lower.contains("crios") && !lower.contains("android")) {
            return "SAFARI";
        }
        if (lower.contains("chrome") || lower.contains("crios")) {
            return "CHROME";
        }
        return "OTHER";
    }

}
