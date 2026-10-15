package com.humolix.volur.system.constant;

/**
 * 系统动态配置项统一键名常量
 * 规范：采用小写点分层级命名（system.<group>.<field_name>），彻底废弃下划线大写与扁平别名
 */
public final class SystemSettingKeys {

    private SystemSettingKeys() {
    }

    // ========== 1. 基础设置 (General) ==========
    public static final String GENERAL_PLATFORM_NAME = "system.general.platform_name";
    public static final String GENERAL_PLATFORM_DESC = "system.general.platform_desc";
    public static final String GENERAL_ICP_NUMBER = "system.general.icp_number";
    public static final String GENERAL_DEFAULT_LOCALE = "system.general.default_locale";
    public static final String GENERAL_FRONTEND_URL = "system.general.frontend_url";

    // ========== 2. 鉴权与注册 (Auth) ==========
    public static final String AUTH_REGISTRATION_ENABLED = "system.auth.registration_enabled";
    public static final String AUTH_REQUIRE_EMAIL_VERIFICATION = "system.auth.require_email_verification";
    public static final String AUTH_MAX_LOGIN_ATTEMPTS = "system.auth.max_login_attempts";
    public static final String AUTH_EMAIL_WHITELIST_SUFFIXES = "system.auth.email_whitelist_suffixes";
    public static final String AUTH_JWT_EXPIRE_SECONDS = "system.auth.jwt_expire_seconds";
    public static final String AUTH_JWT_REFRESH_EXPIRATION_MS = "system.auth.jwt_refresh_expiration_ms";
    public static final String AUTH_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS = "system.auth.jwt_remember_me_refresh_expiration_ms";

    // ========== 3. 人机验证 (Captcha) ==========
    public static final String CAPTCHA_ENABLED = "system.captcha.enabled";
    public static final String CAPTCHA_TOLERANCE_PX = "system.captcha.tolerance_px";
    public static final String CAPTCHA_TICKET_TTL_SECONDS = "system.captcha.ticket_ttl_seconds";
    public static final String CAPTCHA_MAX_ATTEMPTS = "system.captcha.max_attempts";
    public static final String CAPTCHA_FORCE_RESET = "system.captcha.force_reset_captcha";

    // ========== 4. 邮件服务 (Mail) ==========
    public static final String MAIL_ENABLED = "system.mail.enabled";
    public static final String MAIL_SMTP_HOST = "system.mail.smtp_host";
    public static final String MAIL_SMTP_PORT = "system.mail.smtp_port";
    public static final String MAIL_SMTP_USER = "system.mail.smtp_user";
    public static final String MAIL_SMTP_PASS = "system.mail.smtp_pass";
    public static final String MAIL_SMTP_SSL = "system.mail.smtp_ssl";
    public static final String MAIL_FROM_ADDRESS = "system.mail.from_address";
    public static final String MAIL_SENDER_NAME = "system.mail.sender_name";
    public static final String MAIL_ALIYUN_ACCESS_KEY = "system.mail.aliyun_access_key";
    public static final String MAIL_ALIYUN_SECRET_KEY = "system.mail.aliyun_secret_key";
    public static final String MAIL_ALIYUN_ACCOUNT = "system.mail.aliyun_account";
    public static final String MAIL_WEBHOOK_ENABLED = "system.mail.webhook_enabled";
    public static final String MAIL_WEBHOOK_SECRET = "system.mail.webhook_secret";
    public static final String MAIL_WEBHOOK_FALLBACK_STRATEGY = "system.mail.webhook_fallback_strategy";

    // ========== 4.1 短信服务 (SMS) ==========
    public static final String SMS_ENABLED = "system.sms.enabled";

    // ========== 5. 容器基础设施 (Container Infrastructure) ==========
    public static final String CONTAINER_TYPE = "system.container.type";
    public static final String CONTAINER_PORT_RANGE_START = "system.container.port_range_start";
    public static final String CONTAINER_PORT_RANGE_END = "system.container.port_range_end";
    public static final String CONTAINER_PUBLIC_HOST = "system.container.public_host";

    // Docker Swarm 专有配置
    public static final String CONTAINER_DOCKER_SWARM_API_URL = "system.container.docker_swarm.api_url";
    public static final String CONTAINER_DOCKER_SWARM_NETWORK = "system.container.docker_swarm.network";
    public static final String CONTAINER_DOCKER_SWARM_CERT_PATH = "system.container.docker_swarm.cert_path";
    public static final String CONTAINER_DOCKER_SWARM_OUTBOUND_ENABLED = "system.container.docker_swarm.outbound_enabled";

    // K3s 专有配置
    public static final String CONTAINER_K3S_MASTER_URL = "system.container.k3s.master_url";
    public static final String CONTAINER_K3S_NAMESPACE = "system.container.k3s.namespace";
    public static final String CONTAINER_K3S_KUBECONFIG_PATH = "system.container.k3s.kubeconfig_path";
    public static final String CONTAINER_K3S_TOKEN = "system.container.k3s.token";

    // 集群资源隔离与镜像调度
    public static final String CONTAINER_RESOURCE_ISOLATION_ENABLED = "system.container.resource_isolation_enabled";


    // ========== 6. 运维与风控 (Maintenance & Security) ==========
    public static final String MAINTENANCE_ENABLED = "system.maintenance.enabled";
    public static final String MAINTENANCE_NOTICE_MESSAGE = "system.maintenance.notice_message";
    public static final String SECURITY_RATE_LIMIT_WINDOW = "system.security.rate_limit_window";
    public static final String SECURITY_RATE_LIMIT_MAX_REQUESTS = "system.security.rate_limit_max_requests";

    // ========== 7. 文件存储 (Storage) ==========
    public static final String STORAGE_TYPE = "system.storage.type";
    public static final String STORAGE_LOCAL_UPLOAD_DIR = "system.storage.local_upload_dir";
    public static final String STORAGE_OSS_ENDPOINT = "system.storage.oss_endpoint";
    public static final String STORAGE_OSS_ACCESS_KEY = "system.storage.oss_access_key";
    public static final String STORAGE_OSS_SECRET_KEY = "system.storage.oss_secret_key";
    public static final String STORAGE_OSS_BUCKET = "system.storage.oss_bucket";

    // ========== 8. 智能风控与 IP 治理 (Risk & IP Governance) ==========
    public static final String RISK_ENABLED = "system.risk.enabled";
    public static final String RISK_IP_FILTER_ENABLED = "system.risk.ip_filter_enabled";
    public static final String RISK_DEFAULT_BAN_DURATION_MINUTES = "system.risk.default_ban_duration_minutes";
    public static final String RISK_BAN_NOTICE_MESSAGE = "system.risk.ban_notice_message";
    public static final String RISK_BYPASS_LOOPBACK = "system.risk.bypass_loopback";

    // 策略与阈值配置
    public static final String RISK_FAILURES_MEDIUM_THRESHOLD = "system.risk.policy.failures_medium_threshold";
    public static final String RISK_FAILURES_HIGH_THRESHOLD = "system.risk.policy.failures_high_threshold";
    public static final String RISK_FAILURES_BAN_THRESHOLD = "system.risk.policy.failures_ban_threshold";
    public static final String RISK_VELOCITY_MEDIUM_THRESHOLD = "system.risk.policy.velocity_medium_threshold";
    public static final String RISK_ACCOUNT_FAILURES_MEDIUM_THRESHOLD = "system.risk.policy.account_failures_medium_threshold";
    public static final String RISK_ACCOUNT_FAILURES_HIGH_THRESHOLD = "system.risk.policy.account_failures_high_threshold";
    public static final String RISK_RECOVERY_STEP_COUNT = "system.risk.policy.recovery_step_count";

    // 设备环境指纹与原生 TLS 协议风控
    public static final String RISK_FINGERPRINT_ENABLED = "system.risk.fingerprint.enabled";
    public static final String RISK_HEADLESS_PENALTY_ENABLED = "system.risk.headless_penalty.enabled";
    public static final String RISK_TLS_CHECK_ENABLED = "system.risk.tls_check.enabled";
    public static final String RISK_DEVICE_RATE_LIMIT_MAX = "system.risk.device_rate_limit_max";

    public static final String WEBAUTHN_ENABLED = "system.security.webauthn.enabled";
    public static final String WEBAUTHN_RP_ID = "system.security.webauthn.rp_id";
    public static final String WEBAUTHN_RP_NAME = "system.security.webauthn.rp_name";
    public static final String WEBAUTHN_ALLOWED_ORIGINS = "system.security.webauthn.allowed_origins";
    public static final String WEBAUTHN_USER_VERIFICATION = "system.security.webauthn.user_verification";
    public static final String WEBAUTHN_TIMEOUT_SECONDS = "system.security.webauthn.timeout_seconds";

}
