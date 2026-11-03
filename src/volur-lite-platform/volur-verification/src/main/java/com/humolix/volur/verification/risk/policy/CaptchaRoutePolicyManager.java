package com.humolix.volur.verification.risk.policy;

import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 前端组件与路由路径到人机验证基准策略对照管理器
 */
@Component
public class CaptchaRoutePolicyManager {

    @Data
    @Builder
    public static class RoutePolicy {
        private final CaptchaScene scene;
        private final CaptchaType baseType;
    }

    private static final Map<String, RoutePolicy> ROUTE_MAP = new LinkedHashMap<>();

    static {
        // 1. 用户登录组件/路径：基准为图形验证码 (IMAGE / LINE)
        RoutePolicy loginPolicy = RoutePolicy.builder()
                .scene(CaptchaScene.LOGIN)
                .baseType(CaptchaType.LINE)
                .build();
        ROUTE_MAP.put("/auth/login", loginPolicy);
        ROUTE_MAP.put("/login", loginPolicy);

        // 2. 用户注册组件/路径：基准为行为式滑块拼图 (SLIDER)
        RoutePolicy registerPolicy = RoutePolicy.builder()
                .scene(CaptchaScene.REGISTER)
                .baseType(CaptchaType.SLIDE_PUZZLE)
                .build();
        ROUTE_MAP.put("/auth/register", registerPolicy);
        ROUTE_MAP.put("/register", registerPolicy);

        // 3. 忘记密码与重置密码组件/路径：基准为行为式滑块拼图 (SLIDER)
        RoutePolicy resetPolicy = RoutePolicy.builder()
                .scene(CaptchaScene.RESET_PASSWORD)
                .baseType(CaptchaType.SLIDE_PUZZLE)
                .build();
        ROUTE_MAP.put("/auth/forgot", resetPolicy);
        ROUTE_MAP.put("/forgot", resetPolicy);
        ROUTE_MAP.put("/auth/reset-password", resetPolicy);
        ROUTE_MAP.put("/reset-password", resetPolicy);

        // 4. 个人中心与安全修改等敏感操作路径：基准为滑块拼图 (SLIDER)
        RoutePolicy profilePolicy = RoutePolicy.builder()
                .scene(CaptchaScene.CHANGE_PASSWORD)
                .baseType(CaptchaType.SLIDE_PUZZLE)
                .build();
        ROUTE_MAP.put("/profile", profilePolicy);
    }

    /**
     * 根据解密后的请求路径解析基准验证策略
     *
     * @param path 解密出的原始路由路径 (如 "/auth/login")
     * @return 路由对应的场景与基准验证码类型
     */
    public RoutePolicy resolvePolicy(String path) {
        if (path == null) {
            return defaultPolicy();
        }
        String normalized = path.trim().toLowerCase();
        for (Map.Entry<String, RoutePolicy> entry : ROUTE_MAP.entrySet()) {
            if (normalized.equals(entry.getKey()) || normalized.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return defaultPolicy();
    }

    private RoutePolicy defaultPolicy() {
        return RoutePolicy.builder()
                .scene(CaptchaScene.DEFAULT)
                .baseType(CaptchaType.LINE)
                .build();
    }

}
