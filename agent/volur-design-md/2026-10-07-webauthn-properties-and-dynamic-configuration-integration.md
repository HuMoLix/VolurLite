# WebAuthn 配置补齐与平台系统动态配置深度集成方案

## 1. 变更背景与架构动机

在 Volur Lite 的通行密钥（Passkey / WebAuthn）认证体系中：
1. **外部化配置缺失问题**：原本的 `WebAuthnProperties` 仅具备静态内存默认值，未在主配置文件 `application.yml` 中声明对应属性块，无法利用容器环境变量（如 `VOLUR_WEBAUTHN_RP_ID`）进行部署期动态注入。
2. **生产域与合法源（Allowed Origins）硬编码隐患**：原配置将 `rpId` 固化为 `"localhost"`，且白名单仅包含开发回环地址。在实际生产环境部署到指定域名（或跨端访问）时，将直接引发浏览器 WebAuthn 规范中的 `SecurityError`（RP ID 域不匹配）或服务端验签阶段的 `Origin not allowed` 异常。
3. **平台动态配置（SystemSetting）未接入**：缺乏运行时动态启停 Passkey 功能、在线变更 RP ID/名称、按需追加合法来源源地址及调整认证超时的能力。
4. **快速失败与零 Mock 铁律对齐**：当动态配置关闭通行密钥服务时，必须支持运行时即时感知并快速抛出 503 `SERVICE_UNAVAILABLE`，杜绝隐式绕过。

---

## 2. 核心改动点

### 2.1 `WebAuthnProperties` 策略属性补齐与动态解析
- 在 [`WebAuthnProperties.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/config/WebAuthnProperties.java) 中补齐以下关键策略字段：
  - `enabled` (boolean, 默认 `true`)：通行密钥功能总开关；
  - `timeout` (`Duration`, 默认 `60s`)：客户端硬件交互超时时间；
  - `userVerification` (`String`, 默认 `"preferred"`)：用户验证级别偏好；
  - `residentKey` (`String`, 默认 `"preferred"`)：常驻密钥要求；
  - `attestation` (`String`, 默认 `"none"`)：凭据注册见证数据偏好。
- 提供安全合并方法：
  - `resolveEffectiveAllowedOrigins(dynamicOrigins, frontendUrl)`：合并默认开发回环来源、动态设置中配置的逗号分隔白名单，以及系统配置的前端基准访问 URL；
  - `resolveEffectiveRpId`、`resolveEffectiveRpName`、`resolveEffectiveTimeout`、`resolveEffectiveUserVerification`：实现动态配置优先、静态属性兜底的双层解析逻辑。

### 2.2 统一动态配置键名与默认项初始化
- **常量定义**：在 [`SystemSettingKeys.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java) 中增加标准点分层级键名：
  - `system.security.webauthn.enabled`
  - `system.security.webauthn.rp_id`
  - `system.security.webauthn.rp_name`
  - `system.security.webauthn.allowed_origins`
  - `system.security.webauthn.user_verification`
  - `system.security.webauthn.timeout_seconds`
- **初始化与公开属性判定**：在 [`SystemSettingInitializer.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java) 的 `ensureDefaultSettings()` 中幂等注册默认记录，并在 `isPublicSetting()` 中纳管 `system.security.webauthn.` 前缀。

### 2.3 动态委托核心代理 (`DynamicWebAuthnRelyingPartyOperations`)
- 在 [`WebAuthnConfiguration.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/config/WebAuthnConfiguration.java) 中实现 `DynamicWebAuthnRelyingPartyOperations`：
  - 实现 Spring Security `WebAuthnRelyingPartyOperations` 接口；
  - 每次执行凭据注册选项生成、凭据入库、断言挑战生成及硬件验签时，实时校验 `system.security.webauthn.enabled` 状态，未开启直接抛出 `BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "通行密钥 (WebAuthn) 认证服务已停用")`；
  - 动态装配带有最新 `rpEntity`、`allowedOrigins` 及 `timeout` 的底层 `Webauthn4JRelyingPartyOperations` 代理实例，实现零停机热更新。

### 2.4 业务服务与环境矩阵联动 (`PasskeyServiceImpl`)
- 在 [`PasskeyServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasskeyServiceImpl.java) 中注入 `SystemSettingService`；
- 在获取支持认证器列表（`getSupportedAuthenticators`）前进行服务开关判定，禁用时返回空列表；
- 在 `resolveCriteria` 中结合动态配置的 `system.security.webauthn.user_verification` 动态决策 `UserVerificationRequirement`。

### 2.5 数据库增量平滑升级迁移脚本
- 在 [`scripts/upgrade/upgrade_to_latest.sql`](file:///H:/HuMoLix/github/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 迁移存储过程 `migrate_volur_schema_20260921` 中增加 **3.12** 步骤：
  - 使用 `IF NOT EXISTS (SELECT 1 FROM sys_setting WHERE setting_key = ...)` 幂等插入 WebAuthn 的 6 项默认配置行，确保生产环境执行无损升级。

### 2.6 主配置文件与规范文档同步
- 在 [`application.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml) 中新增 `volur.security.webauthn` 配置块，全面支持 `${VOLUR_WEBAUTHN_*}` 环境变量；
- 在 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 第 16 章系统配置表格中补充登记 WebAuthn 配置规范。

---

## 3. 验证与测试

1. **单元测试验证**：
   - 更新并扩充 [`PasskeyServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/PasskeyServiceImplTest.java)，覆盖禁用 WebAuthn 时快速失败与列表置空逻辑；
   - 新增 [`WebAuthnConfigurationTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/config/WebAuthnConfigurationTest.java)，全面验证属性动态解析、跨源白名单合并与代理调用；
   - `volur-security` 全部 93 项单元测试通过（0 失败，0 错误）。
2. **平台编译验证**：
   - 平台全量 13 个子模块 `mvn test-compile` 编译通过。
