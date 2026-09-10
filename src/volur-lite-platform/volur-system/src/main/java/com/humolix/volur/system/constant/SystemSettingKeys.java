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

    // ========== 3. 人机验证 (Captcha) ==========
    public static final String CAPTCHA_ENABLED = "system.captcha.enabled";
    public static final String CAPTCHA_TOLERANCE_PX = "system.captcha.tolerance_px";
    public static final String CAPTCHA_TICKET_TTL_SECONDS = "system.captcha.ticket_ttl_seconds";
    public static final String CAPTCHA_MAX_ATTEMPTS = "system.captcha.max_attempts";
    public static final String CAPTCHA_FORCE_RESET = "system.captcha.force_reset_captcha";
    public static final String CAPTCHA_TURNSTILE_SITE_KEY = "system.captcha.turnstile_site_key";
    public static final String CAPTCHA_TURNSTILE_SECRET_KEY = "system.captcha.turnstile_secret_key";

    // ========== 4. 邮件服务 (Mail) ==========
    public static final String MAIL_SMTP_HOST = "system.mail.smtp_host";
    public static final String MAIL_SMTP_PORT = "system.mail.smtp_port";
    public static final String MAIL_SMTP_USER = "system.mail.smtp_user";
    public static final String MAIL_SMTP_PASS = "system.mail.smtp_pass";
    public static final String MAIL_SMTP_SSL = "system.mail.smtp_ssl";
    public static final String MAIL_SENDER_NAME = "system.mail.sender_name";
    public static final String MAIL_ALIYUN_ACCESS_KEY = "system.mail.aliyun_access_key";
    public static final String MAIL_ALIYUN_SECRET_KEY = "system.mail.aliyun_secret_key";
    public static final String MAIL_ALIYUN_ACCOUNT = "system.mail.aliyun_account";

    // ========== 5. 容器基础设施 (Container Infrastructure) ==========
    public static final String CONTAINER_TYPE = "system.container.type";
    public static final String CONTAINER_PORT_RANGE_START = "system.container.port_range_start";
    public static final String CONTAINER_PORT_RANGE_END = "system.container.port_range_end";
    public static final String CONTAINER_PUBLIC_HOST = "system.container.public_host";

    // Docker Swarm 专有配置
    public static final String CONTAINER_DOCKER_SWARM_API_URL = "system.container.docker_swarm.api_url";
    public static final String CONTAINER_DOCKER_SWARM_NETWORK = "system.container.docker_swarm.network";
    public static final String CONTAINER_DOCKER_SWARM_CERT_PATH = "system.container.docker_swarm.cert_path";

    // K3s 专有配置
    public static final String CONTAINER_K3S_MASTER_URL = "system.container.k3s.master_url";
    public static final String CONTAINER_K3S_NAMESPACE = "system.container.k3s.namespace";
    public static final String CONTAINER_K3S_KUBECONFIG_PATH = "system.container.k3s.kubeconfig_path";

    // 集群资源隔离与镜像调度
    public static final String CONTAINER_RESOURCE_ISOLATION_ENABLED = "system.container.resource_isolation_enabled";
    public static final String CONTAINER_AUTO_MIGRATE_IMAGES_ENABLED = "system.container.auto_migrate_images_enabled";

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

}
