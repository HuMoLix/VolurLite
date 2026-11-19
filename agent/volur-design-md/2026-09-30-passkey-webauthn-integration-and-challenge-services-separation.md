# PassKey (WebAuthn) 官方集成与挑战服务解耦重构方案与施工记录

## 1. 方案背景与目标

在 Volur Lite 现代身份认证与访问控制（IAM）体系演进中，针对账号注册引导流程与登录安全挑战提出了更高层次的安全与体验要求：
1. **选项步骤视图视觉与文案对齐**：`src/views/auth/signup/steps/OptionsChallengeStepView.vue` 的可选项列表样式必须 100% 严格对齐 `src/views/auth/signin/challenge/selectionView.vue`，并将底部按钮文案严格重构为“跳过”与“继续”，杜绝样式分裂与历史冗余文案；
2. **安全挑战服务领域解耦 (Single Responsibility Principle)**：废除旧有平铺式、职责混杂的 `ChallengeService`，彻底拆分为专注于人机滑块/reCAPTCHA 的 `RecaptchaService` 与专注于 FIDO2 / WebAuthn 的 `PasskeyService`；
3. **Spring Security WebAuthn 官方标准支持**：基于 Spring Security 7.1.0 官方 `spring-security-webauthn` 体系，集成 W3C WebAuthn Level 3 与 FIDO2 标准，落地通行密钥（PassKey）凭证存储、注册挑战下发与签名核销；
4. **前端无缝 WebAuthn 凭据创建**：在 `OptionsChallengeStepView.vue` 中接入浏览器原生 `navigator.credentials.create` 接口，支持 Windows Hello、Apple Touch ID / Face ID 以及硬件安全密钥（YubiKey 等），完成端到端通行密钥绑定。

---

## 2. 核心架构设计与改动点

### 2.1 挑战服务解耦与瘦控制器架构

原有的 `ChallengeService` 混合了 reCAPTCHA 生成、非对称加密校验及票据签发。本次重构将其彻底拆分为两个独立的模块化服务：

- **`RecaptchaService` / `RecaptchaServiceImpl`**：
  - 核心职责：生成自适应图形/点触/滑块 reCAPTCHA 挑战，验证客户端非对称加密公私钥载荷，签发人机校验通过凭证（Burn Ticket）；
  - `SignInServiceImpl` 依赖直接重构注入 `RecaptchaService`，彻底移除旧 `ChallengeService` 桥接与别名；
  - 配合单元测试 `RecaptchaServiceImplTest` 覆盖常规核销与签名伪造防御。
- **`PasskeyService` / `PasskeyServiceImpl`**：
  - 核心职责：生成符合 Relying Party（信赖方）配置规范的 `PublicKeyCredentialCreationOptions`，存入 Redis（5 分钟 TTL），在客户端签名完成后调用 `WebAuthnRelyingPartyOperations.registerCredential` 核验签名并持久化；
  - 核销成功后自动清理 Redis 挑战并向状态机上下文写入 `passkeyRegistered=true`；
  - 配合单元测试 `PasskeyServiceImplTest` 覆盖创建与注册流程。
- **`ChallengeController`**：
  - 统一映射路径 `/api/v1/captcha/challenge`；
  - `GET /recaptcha` 与 `POST /recaptcha`：人机验证挑战下发与核销；
  - `GET /passkey` 与 `POST /passkey`：通行密钥创建选项下发与客户端凭证注册绑定；
  - 所有接口由 `dsh`（会话句柄）与 `tl`（转换锁）严格受控，防止跨流程非法伪造。

### 2.2 Spring Security WebAuthn 标准配置与持久化

1. **依赖升级**：在 `volur-security/pom.xml` 中引入 `org.springframework.security:spring-security-webauthn`（由父工程依赖管理，自动引入 `webauthn4j-core:0.31.6.RELEASE`）；
2. **数据表结构设计 (`user_credentials`)**：
   在 `scripts/upgrade/upgrade_to_latest.sql` 中新增幂等创建 DDL：
   - 字段包括 `credential_id`、`user_entity_user_id`、`public_key`、`signature_count`、`uv_initialized`、`backup_eligible`、`authenticator_transports`、`public_key_credential_type`、`backup_state`、`attestation_object`、`attestation_client_data_json`、`created`、`last_used`、`label`；
   - 包含主键与针对 `user_entity_user_id` 的检索索引；
3. **持久化层与信赖方注入**：
   - `UserCredential.java`：JPA 实体映射 `user_credentials` 表；
   - `UserPublicKeyCredentialUserEntityRepository.java`：实现 Spring Security 的 `PublicKeyCredentialUserEntityRepository` 接口，桥接底层 `UserRepository`；
   - `WebAuthnConfiguration.java`：通过 `@Configuration` 组装 `JdbcUserCredentialRepository`、`Webauthn4JRelyingPartyOperations` 与 `WebauthnJackson2Module`；
   - `WebAuthnProperties.java`：提供可配置的 `volur.security.webauthn` 参数（如 RP ID、RP Name 等）。

### 2.3 前端 API 与类型对齐

- **API 接口声明 (`src/api/auth.ts`)**：
  - `fetchPasskeyCreationOptionsApi(params: { dsh: string; tl: string })`: 调用 `GET /captcha/challenge/passkey`；
  - `submitPasskeyRegisterApi(params: { dsh: string; tl: string }, data: PasskeyRegisterRequestDTO)`: 调用 `POST /captcha/challenge/passkey`；
- **强类型模型 (`src/types/auth/flow.ts`)**：
  - 导出 `PasskeyCreationOptionsVO`、`PasskeyRegisterRequestDTO`、`PasskeyRegisterResponseVO`，完全杜绝 `any`。

### 2.4 注册安全保护路由与独立保护视图解耦 (`/pt/options` -> `/pt/pk` & `/pt/totp`)

1. **路由与步骤名标准化**：
   - 将注册流中的选项步骤由原 `challenge/options` 重构规范为 `pt/options`（Protect Options）；
   - 后端 `SignUpController` 的选项接口由 `@GetMapping("/challenge/options")` 同步重命名为 `@GetMapping("/protect/options")`；
   - 后端 `SignUpServiceImpl`、`PasskeyServiceImpl` 及前端 `auth.ts`、`signupFlow.ts` 同步全链路更新；
2. **保护子视图物理隔离与参数穿透**：
   - 当用户在 `/v1/signup/steps/pt/options` 点击“通行密钥”时，带齐当前流转全量状态参数（`continue`、`followup`、`flowName`、`flowEntry`、`dsh`、`tl`、`pstMsg`、`cid`）安全跳转至 `/v1/signup/steps/pt/pk`；
   - 对应前端实现组件独立收敛至 `src/views/auth/signup/protect/passkeyProtectView.vue`，承载原生 WebAuthn（指纹/面容/安全密钥）注册与状态展示；
   - 2FA 同理：点击“身份验证器”带齐状态参数跳转至 `/v1/signup/steps/pt/totp`，对应前端独立组件 `src/views/auth/signup/protect/totpProtectView.vue`；
   - 集中由 `src/views/auth/signup/protect/index.ts` 导出，`SignUpView.vue` 动态路由调度；
3. **视觉结构与主标题对齐 `selectionView.vue`**：
   - `OptionsChallengeStepView.vue` 专注于保护项的选择与流转引导；
   - 列表项精简为仅使用纯粹的主标题（“通行密钥”与“身份验证器”），彻底移除了副标题与标签，与 `selectionView.vue` 单行主标题样式保持 100% 严格一致；
   - 彻底移除了“您也可以随时在登录后前往「个人中心 - 安全设置」中开启或管理这些安全防护方式。”提示段落与右侧冗余操作文本；
4. **按钮文案与完成流转**：
   - 次要按钮：文案严格保持“**跳过**”，点击调用 `handleSkip()` 完成注册；
   - 主要按钮：文案严格保持“**继续**”，点击调用 `handleContinue()` 完成注册。

---

## 3. 规范与纯粹性审计

1. **纯粹代码与零注释铁律**：
   - 所有新增与修改的 Java 代码（`ChallengeController.java`、`PasskeyService.java`、`PasskeyServiceImpl.java`、`RecaptchaService.java`、`RecaptchaServiceImpl.java`、`WebAuthnConfiguration.java` 等）以及前端 Vue/TS 代码（`OptionsChallengeStepView.vue`、`auth.ts`、`flow.ts`）严格杜绝任何解释性注释、思路描述与冗余备注；
   - 所有架构决策与逻辑推导统一外置收敛至本文档。
2. **重构彻底性原则**：
   - 彻底废除并物理删除了旧 `ChallengeService.java`、`ChallengeServiceImpl.java` 及其测试用例，未保留任何别名或废弃兼容层；
   - `SignInServiceImpl` 同步更新引用。
3. **数据库幂等性迁移规范**：
   - 在 `scripts/upgrade/upgrade_to_latest.sql` 中新增的 `user_credentials` 表使用 `CREATE TABLE IF NOT EXISTS`，具备完全幂等性，可在生产环境中重复安全执行。

---

## 4. 验证与回归测试

1. **后端编译与单元测试**：
   - 执行 `mvn test "-Dtest=RecaptchaServiceImplTest,PasskeyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`；
   - 平台 13 个子模块全部构建成功（`BUILD SUCCESS`），单元测试 100% 通过。
2. **前端工程全量构建**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit` 类型静态检查与 Vite 生产打包）；
   - 全量编译通过，无任何 TypeScript 类型错误与样式警告。
