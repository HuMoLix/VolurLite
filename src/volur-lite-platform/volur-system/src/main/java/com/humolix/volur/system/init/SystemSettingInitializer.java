package com.humolix.volur.system.init;

import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import com.humolix.volur.system.repository.SystemSettingRepository;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统动态配置自动化迁移与初始化器
 * 负责在系统启动时将数据库中历史遗留扁平/大写键名自动清洗并平滑迁移至标准层级规范（system.<group>.<field_name>）
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SystemSettingInitializer implements ApplicationRunner {

    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingService systemSettingService;

    private static final Map<String, String> MIGRATION_MAPPINGS = Map.ofEntries(
            Map.entry("ENABLE_CAPTCHA", SystemSettingKeys.CAPTCHA_ENABLED),
            Map.entry("REGISTER_ENABLED", SystemSettingKeys.AUTH_REGISTRATION_ENABLED),
            Map.entry("REQUIRE_EMAIL_VERIFICATION", SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION),
            Map.entry("MAX_LOGIN_ATTEMPTS", SystemSettingKeys.AUTH_MAX_LOGIN_ATTEMPTS),
            Map.entry("EMAIL_WHITELIST_SUFFIXES", SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES),
            Map.entry("MAINTENANCE_MODE", SystemSettingKeys.MAINTENANCE_ENABLED),
            Map.entry("MAINTENANCE_NOTICE", SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE),
            Map.entry("PLATFORM_NAME", SystemSettingKeys.GENERAL_PLATFORM_NAME),
            Map.entry("PLATFORM_DESC", SystemSettingKeys.GENERAL_PLATFORM_DESC),
            Map.entry("ICP_NUMBER", SystemSettingKeys.GENERAL_ICP_NUMBER),
            Map.entry("DEFAULT_LOCALE", SystemSettingKeys.GENERAL_DEFAULT_LOCALE),
            Map.entry("FRONTEND_URL", SystemSettingKeys.GENERAL_FRONTEND_URL),
            Map.entry("SMTP_HOST", SystemSettingKeys.MAIL_SMTP_HOST),
            Map.entry("SMTP_PORT", SystemSettingKeys.MAIL_SMTP_PORT),
            Map.entry("SMTP_USERNAME", SystemSettingKeys.MAIL_SMTP_USER),
            Map.entry("SMTP_USER", SystemSettingKeys.MAIL_SMTP_USER),
            Map.entry("SMTP_PASSWORD", SystemSettingKeys.MAIL_SMTP_PASS),
            Map.entry("SMTP_PASS", SystemSettingKeys.MAIL_SMTP_PASS),
            Map.entry("SMTP_FROM", SystemSettingKeys.MAIL_SENDER_NAME),
            Map.entry("SMTP_FROM_ADDRESS", SystemSettingKeys.MAIL_FROM_ADDRESS),
            Map.entry("SMTP_SSL", SystemSettingKeys.MAIL_SMTP_SSL),
            Map.entry("ALIYUN_DM_ACCESS_KEY", SystemSettingKeys.MAIL_ALIYUN_ACCESS_KEY),
            Map.entry("ALIYUN_DM_SECRET_KEY", SystemSettingKeys.MAIL_ALIYUN_SECRET_KEY),
            Map.entry("ALIYUN_DM_ACCOUNT", SystemSettingKeys.MAIL_ALIYUN_ACCOUNT),
            Map.entry("STORAGE_TYPE", SystemSettingKeys.STORAGE_TYPE),
            Map.entry("LOCAL_UPLOAD_DIR", SystemSettingKeys.STORAGE_LOCAL_UPLOAD_DIR),
            Map.entry("OSS_ENDPOINT", SystemSettingKeys.STORAGE_OSS_ENDPOINT),
            Map.entry("OSS_ACCESS_KEY", SystemSettingKeys.STORAGE_OSS_ACCESS_KEY),
            Map.entry("OSS_SECRET_KEY", SystemSettingKeys.STORAGE_OSS_SECRET_KEY),
            Map.entry("OSS_BUCKET", SystemSettingKeys.STORAGE_OSS_BUCKET)
    );

    private static final List<String> OBSOLETE_KEYS_TO_CLEAN = List.of(
            "FLAG_PREFIX",
            "FLAG_SUFFIX",
            "FLAG_TEMPLATE",
            "DYNAMIC_FLAG_TEMPLATE",
            "system.challenge.flag_prefix",
            "system.challenge.flag_suffix",
            "system.challenge.flag_template",
            "system.container.default_duration_minutes",
            "system.container.max_user_containers"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        log.info("[SystemSettingInitializer] 正在执行系统配置项历史版本检查与规范化迁移...");
        int migratedCount = 0;

        for (Map.Entry<String, String> entry : MIGRATION_MAPPINGS.entrySet()) {
            String legacyKey = entry.getKey();
            String canonicalKey = entry.getValue();

            Optional<SystemSettingEntity> legacyEntityOpt = systemSettingRepository.findBySettingKey(legacyKey);
            if (legacyEntityOpt.isPresent()) {
                SystemSettingEntity legacyEntity = legacyEntityOpt.get();
                String value = legacyEntity.getSettingValue();

                Optional<SystemSettingEntity> canonicalOpt = systemSettingRepository.findBySettingKey(canonicalKey);
                if (canonicalOpt.isEmpty()) {
                    SystemSettingEntity newEntity = SystemSettingEntity.builder()
                            .settingKey(canonicalKey)
                            .settingValue(value)
                            .settingGroup(resolveGroup(canonicalKey))
                            .valueType(legacyEntity.getValueType())
                            .isPublic(isPublicSetting(canonicalKey))
                            .description(legacyEntity.getDescription())
                            .build();
                    systemSettingRepository.save(newEntity);
                }

                systemSettingRepository.delete(legacyEntity);
                systemSettingService.evictCache(legacyKey);
                systemSettingService.evictCache(canonicalKey);
                migratedCount++;
                log.info("[SystemSettingInitializer] 成功迁移旧版配置项: {} -> {}", legacyKey, canonicalKey);
            }
        }

        // 清理已下放到竞赛规则的废弃系统级键
        for (String obsoleteKey : OBSOLETE_KEYS_TO_CLEAN) {
            systemSettingRepository.findBySettingKey(obsoleteKey).ifPresent(entity -> {
                systemSettingRepository.delete(entity);
                systemSettingService.evictCache(obsoleteKey);
                log.info("[SystemSettingInitializer] 已清理已下放到比赛规则的废弃全局配置: {}", obsoleteKey);
            });
        }

        ensureDefaultSettings();

        if (migratedCount > 0) {
            log.info("[SystemSettingInitializer] 系统配置项规范化迁移完成，共清理迁移 {} 项历史键", migratedCount);
        }
    }

    private void ensureDefaultSettings() {
        initDefaultIfAbsent(SystemSettingKeys.CAPTCHA_ENABLED, "true", "CAPTCHA", "BOOLEAN", true, "人机验证是否开启");
        initDefaultIfAbsent(SystemSettingKeys.AUTH_REGISTRATION_ENABLED, "true", "AUTH", "BOOLEAN", true, "是否开放用户注册");
        initDefaultIfAbsent(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, "true", "AUTH", "BOOLEAN", true, "注册是否强制邮箱验证");
        initDefaultIfAbsent(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, "", "AUTH", "STRING", false, "允许注册的邮箱域名白名单后缀(逗号分隔，留空不限制)");
        initDefaultIfAbsent(SystemSettingKeys.AUTH_JWT_EXPIRE_SECONDS, "86400", "AUTH", "INTEGER", false, "JWT Access Token 有效期 (秒)");
        initDefaultIfAbsent(SystemSettingKeys.AUTH_JWT_REFRESH_EXPIRATION_MS, "604800000", "AUTH", "INTEGER", false, "JWT Refresh Token 有效期 (毫秒)");
        initDefaultIfAbsent(SystemSettingKeys.AUTH_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS, "2592000000", "AUTH", "INTEGER", false, "记住我 JWT Refresh Token 有效期 (毫秒)");
        initDefaultIfAbsent(SystemSettingKeys.GENERAL_PLATFORM_NAME, "Volur Lite", "GENERAL", "STRING", true, "平台全称");
        initDefaultIfAbsent(SystemSettingKeys.GENERAL_DEFAULT_LOCALE, "zh_CN", "GENERAL", "STRING", true, "系统默认语言");
        initDefaultIfAbsent(SystemSettingKeys.MAINTENANCE_ENABLED, "false", "MAINTENANCE", "BOOLEAN", true, "全站维护模式开关");
        initDefaultIfAbsent(SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE, "系统正在全站维护升级中，暂不可用，请稍后访问。", "MAINTENANCE", "STRING", true, "维护公告提示");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM", "CONTAINER", "STRING", false, "动态靶机容器集群调度类型(DOCKER_SWARM/K3S)");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_PORT_RANGE_START, "20000", "CONTAINER", "INTEGER", false, "靶机动态端口映射起始端口");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_PORT_RANGE_END, "30000", "CONTAINER", "INTEGER", false, "靶机动态端口映射结束端口");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1", "CONTAINER", "STRING", false, "靶机外部可访问域名或IP");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock", "CONTAINER", "STRING", false, "Docker Swarm API 管理地址");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_DOCKER_SWARM_NETWORK, "volur-swarm-network", "CONTAINER", "STRING", false, "Docker Swarm 覆盖网络名称");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_DOCKER_SWARM_CERT_PATH, "", "CONTAINER", "STRING", false, "Docker Swarm TLS 客户端证书目录");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_DOCKER_SWARM_OUTBOUND_ENABLED, "false", "CONTAINER", "BOOLEAN", false, "Docker Swarm 容器外网出网访问开关(默认禁止出网以保证安全隔离)");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_K3S_MASTER_URL, "https://127.0.0.1:6443", "CONTAINER", "STRING", false, "K3s 集群 API Server 地址");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_K3S_NAMESPACE, "volur-challenges", "CONTAINER", "STRING", false, "K3s 靶机命名空间 Namespace");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_K3S_KUBECONFIG_PATH, "/etc/rancher/k3s/k3s.yaml", "CONTAINER", "STRING", false, "K3s kubeconfig 配置文件绝对路径");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_K3S_TOKEN, "", "CONTAINER", "STRING", false, "K3s 集群注册令牌 (Node Token)");
        initDefaultIfAbsent(SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED, "true", "CONTAINER", "BOOLEAN", false, "是否开启多节点资源隔离(题目容器仅调度至Worker资源节点)");
        initDefaultIfAbsent(SystemSettingKeys.STORAGE_TYPE, "LOCAL", "STORAGE", "STRING", false, "文件存储类型(LOCAL/OSS)");
        initDefaultIfAbsent(SystemSettingKeys.STORAGE_LOCAL_UPLOAD_DIR, "uploads/", "STORAGE", "STRING", false, "本地文件上传保存目录");
        initDefaultIfAbsent(SystemSettingKeys.MAIL_WEBHOOK_ENABLED, "false", "MAIL", "BOOLEAN", false, "邮件投递 Webhook 回调追踪开关");
        initDefaultIfAbsent(SystemSettingKeys.MAIL_WEBHOOK_SECRET, "", "MAIL", "STRING", false, "邮件投递 Webhook 验签密钥/Token");
        initDefaultIfAbsent(SystemSettingKeys.MAIL_WEBHOOK_FALLBACK_STRATEGY, "AUTO_DELIVERED", "MAIL", "STRING", false, "Webhook未启用或超时备用策略(AUTO_DELIVERED/KEEP_SENT)");
        initDefaultIfAbsent(SystemSettingKeys.RISK_ENABLED, "true", "RISK", "BOOLEAN", false, "全局风控引擎总开关");
        initDefaultIfAbsent(SystemSettingKeys.RISK_IP_FILTER_ENABLED, "true", "RISK", "BOOLEAN", false, "IP 封禁与拦截总开关");
        initDefaultIfAbsent(SystemSettingKeys.RISK_DEFAULT_BAN_DURATION_MINUTES, "1440", "RISK", "INTEGER", false, "默认封禁时长 (分钟)");
        initDefaultIfAbsent(SystemSettingKeys.RISK_BAN_NOTICE_MESSAGE, "您的 IP 存在异常访问行为，已被系统安全机制拦截。如需解封请联系平台管理员。", "RISK", "STRING", true, "IP封禁拦截提示文案");
        initDefaultIfAbsent(SystemSettingKeys.RISK_BYPASS_LOOPBACK, "true", "RISK", "BOOLEAN", false, "是否放行本地回环及私网穿透 IP");
        initDefaultIfAbsent(SystemSettingKeys.RISK_FAILURES_MEDIUM_THRESHOLD, "5", "RISK", "INTEGER", false, "连续失败升中风险阈值");
        initDefaultIfAbsent(SystemSettingKeys.RISK_FAILURES_HIGH_THRESHOLD, "10", "RISK", "INTEGER", false, "连续失败升高风险阈值");
        initDefaultIfAbsent(SystemSettingKeys.RISK_FAILURES_BAN_THRESHOLD, "20", "RISK", "INTEGER", false, "连续失败触发自动封禁 IP 阈值");
        initDefaultIfAbsent(SystemSettingKeys.RISK_VELOCITY_MEDIUM_THRESHOLD, "12", "RISK", "INTEGER", false, "1分钟突发请求速率超限阈值");
        initDefaultIfAbsent(SystemSettingKeys.RISK_ACCOUNT_FAILURES_MEDIUM_THRESHOLD, "4", "RISK", "INTEGER", false, "单账号全网失败升中风险阈值");
        initDefaultIfAbsent(SystemSettingKeys.RISK_ACCOUNT_FAILURES_HIGH_THRESHOLD, "8", "RISK", "INTEGER", false, "单账号全网失败升高风险阈值");
        initDefaultIfAbsent(SystemSettingKeys.RISK_RECOVERY_STEP_COUNT, "6", "RISK", "INTEGER", false, "单次人机验证通过扣减风险失败计数步长");
        initDefaultIfAbsent(SystemSettingKeys.RISK_FINGERPRINT_ENABLED, "true", "RISK", "BOOLEAN", false, "设备与Canvas指纹多维风控开关");
        initDefaultIfAbsent(SystemSettingKeys.RISK_HEADLESS_PENALTY_ENABLED, "true", "RISK", "BOOLEAN", false, "无头浏览器与自动化脚本严控惩罚开关");
        initDefaultIfAbsent(SystemSettingKeys.RISK_TLS_CHECK_ENABLED, "true", "RISK", "BOOLEAN", false, "原生Nginx TLS握手特征一致性核验开关");
        initDefaultIfAbsent(SystemSettingKeys.RISK_DEVICE_RATE_LIMIT_MAX, "100", "RISK", "INTEGER", false, "单设备跨IP滑动窗口最大请求次数");
        initDefaultIfAbsent(SystemSettingKeys.WEBAUTHN_ENABLED, "true", "SECURITY", "BOOLEAN", true, "通行密钥 (WebAuthn/Passkey) 认证总开关");
        initDefaultIfAbsent(SystemSettingKeys.WEBAUTHN_RP_ID, "localhost", "SECURITY", "STRING", true, "WebAuthn Relying Party 身份标识 (域名或localhost)");
        initDefaultIfAbsent(SystemSettingKeys.WEBAUTHN_RP_NAME, "Volur Lite", "SECURITY", "STRING", true, "WebAuthn 依赖方应用展示名称");
        initDefaultIfAbsent(SystemSettingKeys.WEBAUTHN_ALLOWED_ORIGINS, "", "SECURITY", "STRING", false, "允许的额外 WebAuthn 来源列表 (逗号分隔，留空使用默认)");
        initDefaultIfAbsent(SystemSettingKeys.WEBAUTHN_USER_VERIFICATION, "preferred", "SECURITY", "STRING", false, "WebAuthn 用户验证偏好 (preferred/required/discouraged)");
        initDefaultIfAbsent(SystemSettingKeys.WEBAUTHN_TIMEOUT_SECONDS, "60", "SECURITY", "INTEGER", false, "WebAuthn 认证交互超时时间 (秒)");
        initDefaultIfAbsent(SystemSettingKeys.MAIL_ENABLED, "false", "MAIL", "BOOLEAN", true, "邮件服务总开关");
        initDefaultIfAbsent(SystemSettingKeys.SMS_ENABLED, "false", "SMS", "BOOLEAN", true, "短信服务总开关");
    }

    private void initDefaultIfAbsent(String key, String defaultValue, String group, String type, boolean isPublic, String desc) {
        if (systemSettingRepository.findBySettingKey(key).isEmpty()) {
            SystemSettingEntity entity = SystemSettingEntity.builder()
                    .settingKey(key)
                    .settingValue(defaultValue)
                    .settingGroup(group)
                    .valueType(type)
                    .isPublic(isPublic)
                    .description(desc)
                    .build();
            systemSettingRepository.save(entity);
            systemSettingService.evictCache(key);
        }
    }

    private String resolveGroup(String key) {
        if (key.startsWith("system.general.")) return "GENERAL";
        if (key.startsWith("system.auth.")) return "AUTH";
        if (key.startsWith("system.captcha.")) return "CAPTCHA";
        if (key.startsWith("system.mail.")) return "MAIL";
        if (key.startsWith("system.sms.")) return "SMS";
        if (key.startsWith("system.container.")) return "CONTAINER";
        if (key.startsWith("system.challenge.")) return "CHALLENGE";
        if (key.startsWith("system.maintenance.")) return "MAINTENANCE";
        if (key.startsWith("system.security.")) return "SECURITY";
        if (key.startsWith("system.risk.")) return "RISK";
        if (key.startsWith("system.storage.")) return "STORAGE";
        return "GENERAL";
    }

    private boolean isPublicSetting(String key) {
        return key.startsWith("system.general.")
                || key.startsWith("system.maintenance.")
                || key.startsWith("system.security.webauthn.")
                || SystemSettingKeys.CAPTCHA_ENABLED.equals(key)
                || SystemSettingKeys.AUTH_REGISTRATION_ENABLED.equals(key)
                || SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION.equals(key)
                || SystemSettingKeys.MAIL_ENABLED.equals(key)
                || SystemSettingKeys.SMS_ENABLED.equals(key);
    }
}
