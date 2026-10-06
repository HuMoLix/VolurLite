-- ==============================================================================
-- Volur Lite 数据库增量平滑升级迁移脚本 (针对 2026-09-21 及历史版本升级至最新版)
-- 适用场景：远程生产环境升级，保留所有历史用户、竞赛、战队、答题记录与静态配置
-- 特性：幂等设计 (Idempotent)，可重复安全执行，无损迁移
-- ==============================================================================

USE `volur_db`;

-- ------------------------------------------------------------------------------
-- 1. 新增数据表：竞赛实时即时通讯/工单聊天记录表 (099526e 新增)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `competition_chat_message` (
  `id` bigint NOT NULL,
  `competition_id` bigint NOT NULL,
  `team_id` bigint NOT NULL,
  `channel_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_id` bigint NOT NULL,
  `sender_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_role` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiver_id` bigint DEFAULT NULL,
  `receiver_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mention_user_ids` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `msg_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_comp_team_chan` (`competition_id`,`team_id`,`channel_type`,`id`),
  KEY `idx_chat_comp_priv` (`competition_id`,`channel_type`,`sender_id`,`receiver_id`),
  KEY `idx_chat_created_at` (`created_at`),
  CONSTRAINT `fk_chat_competition` FOREIGN KEY (`competition_id`) REFERENCES `competition` (`id`),
  CONSTRAINT `fk_chat_team` FOREIGN KEY (`team_id`) REFERENCES `competition_team` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chat_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. 新增数据表：竞赛独立运行操作流水日志表 (最新架构剥离)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `competition_operation_log` (
  `id` bigint NOT NULL,
  `competition_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `team_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `challenge_id` bigint DEFAULT NULL,
  `challenge_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `log_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(1) NOT NULL,
  `operator_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_method` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_uri` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `params` text COLLATE utf8mb4_unicode_ci,
  `result` text COLLATE utf8mb4_unicode_ci,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comp_op_log_comp_created` (`competition_id`,`created_at` DESC),
  KEY `idx_comp_op_log_comp_user` (`competition_id`,`username`),
  KEY `idx_comp_op_log_comp_team` (`competition_id`,`team_name`),
  KEY `idx_comp_op_log_comp_challenge` (`competition_id`,`challenge_title`),
  KEY `idx_comp_op_log_comp_type` (`competition_id`,`log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. 新增数据表：智能风控与 IP 治理规则表 (基于 IP 封禁与白名单机制)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_ip_rule` (
  `id` bigint NOT NULL,
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `expires_at` timestamp NULL DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_risk_ip_rule_ip_type_status` (`ip`,`rule_type`,`status`),
  KEY `idx_risk_ip_rule_status_expires` (`status`,`expires_at`),
  KEY `idx_risk_ip_rule_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. 新增数据表：WebAuthn 通行密钥凭证表 (Passkey Credentials)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_credentials` (
  `credential_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_entity_user_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `public_key` blob NOT NULL,
  `signature_count` bigint NOT NULL DEFAULT 0,
  `uv_initialized` tinyint(1) NOT NULL DEFAULT 0,
  `backup_eligible` tinyint(1) NOT NULL DEFAULT 0,
  `authenticator_transports` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `public_key_credential_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'public-key',
  `backup_state` tinyint(1) NOT NULL DEFAULT 0,
  `attestation_object` longblob,
  `attestation_client_data_json` longblob,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used` timestamp NULL DEFAULT NULL,
  `label` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`credential_id`),
  KEY `idx_user_credentials_user_id` (`user_entity_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 5. 新增数据表：用户受信设备与风控资产表 (User Trusted Device)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_trusted_device` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `device_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `os_family` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `browser_family` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `first_login_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_login_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `first_login_time` timestamp NULL DEFAULT NULL,
  `last_login_time` timestamp NULL DEFAULT NULL,
  `is_trusted` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_hash`),
  KEY `idx_trusted_device_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. 存储过程执行表结构增量与字段迁移 (幂等执行)
-- ------------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `migrate_volur_schema_20260921`;

DELIMITER //
CREATE PROCEDURE `migrate_volur_schema_20260921`()
BEGIN
    -- 3.1 competition_config 表扩充 reg_requirements
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_config' AND COLUMN_NAME = 'reg_requirements'
    ) THEN
        ALTER TABLE `competition_config` ADD COLUMN `reg_requirements` text COLLATE utf8mb4_unicode_ci;
    END IF;

    -- 3.2 competition_team 表结构扩充
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_team' AND COLUMN_NAME = 'description'
    ) THEN
        ALTER TABLE `competition_team` ADD COLUMN `description` varchar(512) COLLATE utf8mb4_unicode_ci;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_team' AND COLUMN_NAME = 'organization'
    ) THEN
        ALTER TABLE `competition_team` ADD COLUMN `organization` varchar(128) COLLATE utf8mb4_unicode_ci;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_team' AND COLUMN_NAME = 'organization_type'
    ) THEN
        ALTER TABLE `competition_team` ADD COLUMN `organization_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'SCHOOL';
    END IF;

    -- 数据回填：将原有队伍口号 team_slogan 迁移回填入 description
    UPDATE `competition_team` SET `description` = `team_slogan` 
    WHERE (`description` IS NULL OR `description` = '') AND `team_slogan` IS NOT NULL;

    -- 数据回填：根据队长成员信息回填战队所属机构/学院 organization
    UPDATE `competition_team` t
    JOIN `competition_member` m ON m.team_id = t.id AND m.role = 'CAPTAIN'
    SET t.organization = m.organization
    WHERE (t.organization IS NULL OR t.organization = '') AND m.organization IS NOT NULL;

    -- 3.3 competition_member 表结构扩充与约束放宽
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_member' AND COLUMN_NAME = 'employee_number'
    ) THEN
        ALTER TABLE `competition_member` ADD COLUMN `employee_number` varchar(64) COLLATE utf8mb4_unicode_ci;
    END IF;

    -- 放宽 user_id 允许为 NULL (支持预填手机号/邮箱邀请未注册队员)
    ALTER TABLE `competition_member` MODIFY COLUMN `user_id` bigint NULL;

    -- 新增多维防重与检索索引
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_member' AND INDEX_NAME = 'idx_comp_phone'
    ) THEN
        ALTER TABLE `competition_member` ADD INDEX `idx_comp_phone` (`competition_id`, `phone`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_member' AND INDEX_NAME = 'idx_comp_idcard'
    ) THEN
        ALTER TABLE `competition_member` ADD INDEX `idx_comp_idcard` (`competition_id`, `id_card`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_member' AND INDEX_NAME = 'idx_comp_email'
    ) THEN
        ALTER TABLE `competition_member` ADD INDEX `idx_comp_email` (`competition_id`, `email`);
    END IF;

    -- 3.4 ctf_challenge 表扩充多附件 attachments JSON 存储
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ctf_challenge' AND COLUMN_NAME = 'attachments'
    ) THEN
        ALTER TABLE `ctf_challenge` ADD COLUMN `attachments` text COLLATE utf8mb4_unicode_ci;
    END IF;

    -- 3.5 ctf_challenge_category 表扩充 icon 图标字段
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ctf_challenge_category' AND COLUMN_NAME = 'icon'
    ) THEN
        ALTER TABLE `ctf_challenge_category` ADD COLUMN `icon` varchar(64) COLLATE utf8mb4_unicode_ci;
    END IF;

    -- 3.6 cluster_image 表：新增 in_registry 并修复 image_type 非空陷阱
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cluster_image' AND COLUMN_NAME = 'in_registry'
    ) THEN
        ALTER TABLE `cluster_image` ADD COLUMN `in_registry` tinyint(1) NOT NULL DEFAULT 0;
    END IF;

    -- 关键修复：放宽旧字段 image_type 允许为 NULL，避免新版无此字段时引发严格模式报错
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cluster_image' AND COLUMN_NAME = 'image_type' AND IS_NULLABLE = 'NO'
    ) THEN
        ALTER TABLE `cluster_image` MODIFY COLUMN `image_type` varchar(32) COLLATE utf8mb4_unicode_ci NULL;
    END IF;

    -- 3.7 email_log 表扩充 message_id 与索引
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_log' AND COLUMN_NAME = 'message_id'
    ) THEN
        ALTER TABLE `email_log` ADD COLUMN `message_id` varchar(128) COLLATE utf8mb4_unicode_ci;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_log' AND INDEX_NAME = 'idx_email_log_message_id'
    ) THEN
        ALTER TABLE `email_log` ADD INDEX `idx_email_log_message_id` (`message_id`);
    END IF;

    -- 3.8 sys_setting 表：确保注册强制邮箱验证默认配置项写入 (system.auth.require_email_verification)
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_setting'
    ) AND NOT EXISTS (
        SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.auth.require_email_verification'
    ) THEN
        INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
        VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'AUTH', 'system.auth.require_email_verification', 'true', 'BOOLEAN', 1, '注册是否强制邮箱验证', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;

    -- 3.9 competition_chat_message 外键平滑升级为 ON DELETE CASCADE (解决管理员与队长删除战队时触发外键阻断)
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_chat_message'
    ) THEN
        SET @fk_chat_team_name = NULL;
        SELECT CONSTRAINT_NAME INTO @fk_chat_team_name
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'competition_chat_message'
          AND COLUMN_NAME = 'team_id'
          AND REFERENCED_TABLE_NAME = 'competition_team'
        LIMIT 1;

        IF @fk_chat_team_name IS NOT NULL THEN
            SET @drop_fk_sql = CONCAT('ALTER TABLE `competition_chat_message` DROP FOREIGN KEY `', @fk_chat_team_name, '`');
            PREPARE stmt_drop FROM @drop_fk_sql;
            EXECUTE stmt_drop;
            DEALLOCATE PREPARE stmt_drop;

            ALTER TABLE `competition_chat_message`
            ADD CONSTRAINT `fk_chat_team` FOREIGN KEY (`team_id`) REFERENCES `competition_team` (`id`) ON DELETE CASCADE;
        END IF;
    END IF;

    -- 3.10 competition_team_invitation 外键平滑升级为 ON DELETE CASCADE
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_team_invitation'
    ) THEN
        SET @fk_inv_team_name = NULL;
        SELECT CONSTRAINT_NAME INTO @fk_inv_team_name
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'competition_team_invitation'
          AND COLUMN_NAME = 'team_id'
          AND REFERENCED_TABLE_NAME = 'competition_team'
        LIMIT 1;

        IF @fk_inv_team_name IS NOT NULL THEN
            SET @drop_inv_sql = CONCAT('ALTER TABLE `competition_team_invitation` DROP FOREIGN KEY `', @fk_inv_team_name, '`');
            PREPARE stmt_drop_inv FROM @drop_inv_sql;
            EXECUTE stmt_drop_inv;
            DEALLOCATE PREPARE stmt_drop_inv;

            ALTER TABLE `competition_team_invitation`
            ADD CONSTRAINT `fk_invitation_team` FOREIGN KEY (`team_id`) REFERENCES `competition_team` (`id`) ON DELETE CASCADE;
        END IF;
    END IF;

    -- 3.11 user_credentials 表：修复 public_key 字段类型为 BLOB (适配 Spring Security WebAuthn 二进制公钥)
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_credentials' AND COLUMN_NAME = 'public_key' AND DATA_TYPE NOT IN ('blob', 'mediumblob', 'longblob')
    ) THEN
        ALTER TABLE `user_credentials` MODIFY COLUMN `public_key` blob NOT NULL;
    END IF;

    -- 3.12 sys_setting 表：初始化 WebAuthn / 通行密钥动态配置项 (幂等写入)
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_setting'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.security.webauthn.enabled') THEN
            INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
            VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'SECURITY', 'system.security.webauthn.enabled', 'true', 'BOOLEAN', 1, '通行密钥 (WebAuthn/Passkey) 认证总开关', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.security.webauthn.rp_id') THEN
            INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
            VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'SECURITY', 'system.security.webauthn.rp_id', 'localhost', 'STRING', 1, 'WebAuthn Relying Party 身份标识 (域名或localhost)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.security.webauthn.rp_name') THEN
            INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
            VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'SECURITY', 'system.security.webauthn.rp_name', 'Volur Lite', 'STRING', 1, 'WebAuthn 依赖方应用展示名称', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.security.webauthn.allowed_origins') THEN
            INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
            VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'SECURITY', 'system.security.webauthn.allowed_origins', '', 'STRING', 0, '允许的额外 WebAuthn 来源列表 (逗号分隔，留空使用默认)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.security.webauthn.user_verification') THEN
            INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
            VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'SECURITY', 'system.security.webauthn.user_verification', 'preferred', 'STRING', 0, 'WebAuthn 用户验证偏好 (preferred/required/discouraged)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM `sys_setting` WHERE `setting_key` = 'system.security.webauthn.timeout_seconds') THEN
            INSERT INTO `sys_setting` (`id`, `setting_group`, `setting_key`, `setting_value`, `value_type`, `is_public`, `description`, `created_at`, `updated_at`)
            VALUES (FLOOR(RAND() * 900000000000000000 + 1000000000000000000), 'SECURITY', 'system.security.webauthn.timeout_seconds', '60', 'INTEGER', 0, 'WebAuthn 认证交互超时时间 (秒)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;
    END IF;

END //
DELIMITER ;

-- 执行迁移存储过程
CALL `migrate_volur_schema_20260921`();

-- 执行完成清理存储过程
DROP PROCEDURE `migrate_volur_schema_20260921`;
