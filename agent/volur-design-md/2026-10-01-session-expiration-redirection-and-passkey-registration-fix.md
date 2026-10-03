# 施工与修改记录：认证会话超时视图演进、注册落库机制与安全挑战控制器解耦

- **实施日期**：2026-10-01
- **涉及模块**：
  - 前端工程：`volur-lite-view-NEXT`（`sessionExpireView.vue`, `AuthLayout.vue`, `flow.ts`, `signinFlow.ts`, `signupFlow.ts`, `request.ts`, `auth.ts`, `passkeyProtectView.vue`）
  - 后端工程：`volur-security`（`RecaptchaController.java`, `PasskeyController.java`, `PasskeyService.java`, `PasskeyServiceImpl.java`, `UserPublicKeyCredentialUserEntityRepository.java`, `WebAuthnProperties.java`）
  - 核心架构：认证流转、WebAuthn 多端认证器选择与自适应挑战引擎

---

## 1. 变更背景与需求说明

1. **会话超时视图与重试逻辑精简**：
   - 此前 `/v1/auth/info/sessionexpire` 路径冗长，需规范重命名为 `/v1/info/sessionexpire`；
   - 视图右侧原有装饰性时钟图标与“安全时限已过”副标题需去除，仅保留“为了保护您的账户信息，未完成的流程已自动失效。请重新开始。”单段描述文本；
   - 移除“返回首页”按钮，仅保留单主按钮并更名为“重试”；
   - 对“重试”按钮注入类似 `IdentifierView.vue` 的延迟遮罩层（Scrim）阻断动效，防止连击并提供平滑视觉过渡；
   - 携带 `?continue=&followup=` 深度参数，根据 `followup` 与 `continue` 智能解析重试目标路由（signin 导向 `/v1/signin/identifier`、signup 导向 `/v1/signup`、其他业务模块原样重回）。

2. **用户注册落库时机与临时用户回滚**：
   - 此前针对 Passkey 注册阶段用户不存在的问题引入了临时用户逻辑，但实际上在 `SignupServiceImpl.password()` 中，当用户填写完密码并提交后，系统已直接通过 `userRepository.saveAndFlush` 与 `userProfileRepository.save` 完成实体持久化，并签发了登录令牌；
   - 后续的 Passkey 与 TOTP 为推荐增强步骤而非必须阻塞步骤；
   - 彻底回滚 `UserPublicKeyCredentialUserEntityRepository.java` 中的临时用户逻辑，严格基于持久化 `userRepository` 检索；
   - `WebAuthnProperties.allowedOrigins` 补全开发环境常用端口（3000、3001、5173 等）。

3. **安全挑战控制器解耦拆分 (ChallengeController)**：
   - 拆解原庞大单体 `ChallengeController`，按照 `volur-security/service/` 结构解耦为专属微控制器：
     - `RecaptchaController`：映射 `/api/v1/captcha/challenge/recaptcha`，提供 GET 获取挑战与 POST 验证挑战；
     - `PasskeyController`：映射 `/api/v1/captcha/challenge/passkey`，提供 GET 获取凭据创建参数（支持可选 `authenticatorType`）、POST 注册凭据，以及 GET `/authenticators` 获取平台支持的多端认证器列表；
   - 全面支持 Google 风格的 5 种核心认证器类型：
     1. `windows_password_manager`：Windows 密码管理器
     2. `windows_hello`：Windows Hello
     3. `security_key`：Windows 安全中心的安全密钥U盘
     4. `mobile_device`：iPhone、iPad 或 Android 设备
     5. `apple_keychain`：苹果钥匙串

---

## 2. 详细改造与工程实现

### 2.1 会话超时专属视图与重定向流 (`sessionExpireView.vue` & `flow.ts`)
1. **视图重构**：
   - 移除了卡片右侧的 SVG 图标与副标题，呈现单一行内段落：“为了保护您的账户信息，未完成的流程已自动失效。请重新开始。”；
   - 移除了次要操作按钮（“返回首页”），保留单一蓝色主按钮“重试”（`.btn-next-step`）；
   - 点击“重试”时，通过 `activeFlowStore.triggerScrim` 唤起全卡片加载进度条、半透明遮罩与操作锁，延时后安全切换路由。
2. **路由与工具函数升级**：
   - 路由由 `/v1/auth/info/sessionexpire` 调整为 `/v1/info/sessionexpire`；
   - 在 `src/utils/flow.ts` 中封装 `buildSessionExpireLocation(route)` 与 `buildSessionExpireHref()`，统一生成携带 `continue` 与 `followup` 参数的规范 URL；
   - `sessionExpireView.vue` 根据 URL 中的 `followup`（优先）及 `continue` 判断跳转目的地：
     - 包含 `signup`：重置状态并跳转至 `/v1/signup`；
     - 包含 `signin`：重置状态并跳转至 `/v1/signin/identifier`；
     - 其他模块：按原始路径返回。
3. **拦截器全面对齐**：
   - `src/router/index.ts`（路由全局守卫）；
   - `src/store/modules/flow/signinFlow.ts`（登录流状态机）；
   - `src/store/modules/flow/signupFlow.ts`（注册流状态机）；
   - `src/utils/request.ts`（Axios 401/403 异常拦截器）。

### 2.2 注册实体落库机制与回滚
- 在 `SignupServiceImpl.java` 中确认：用户在完成密码步骤后，系统即已向 MySQL 写入并刷新 `User` 和 `UserProfile` 记录，随后进入 `pt/options` 步骤；
- 回滚 `UserPublicKeyCredentialUserEntityRepository.java`，移除未持久化临时用户构建分支，保持数据源唯一真实性（SSOT）；
- 保持 `passkeyProtectView.vue` 中健壮的 `base64UrlToBuffer` 实现，保障从 JSON 对象或字符串安全无损解码。

### 2.3 挑战控制器拆分与 Passkey 多认证器实现
1. **解耦拆分**：
   - 彻底删除原 `ChallengeController.java`；
   - 新增 `RecaptchaController.java`（`/api/v1/captcha/challenge/recaptcha`）；
   - 新增 `PasskeyController.java`（`/api/v1/captcha/challenge/passkey`）。
2. **多端认证器构建与下发**：
   - 新增 `PasskeyAuthenticatorOptionVO.java`，定义认证器类型键名、名称、归属类别（PLATFORM/CROSS_PLATFORM）、描述说明与图标；
   - 在 `PasskeyServiceImpl.java` 中实现 `getSupportedAuthenticators()`，支持：
     - `windows_password_manager`：Windows 密码管理器
     - `windows_hello`：Windows Hello
     - `security_key`：Windows 安全中心的安全密钥U盘
     - `mobile_device`：iPhone、iPad 或 Android 设备
     - `apple_keychain`：苹果钥匙串
   - 在 `getCreationOptions` 中根据客户端传入的 `authenticatorType` 构建对应的 `AuthenticatorSelectionCriteria`（如 Platform/Cross-Platform 区分，Preferred/Required UV 设定），若未指定则由浏览器/系统自动唤起多选弹窗。

---

## 3. 验证与审计结论

1. **前端工程编译构建**：
   - 执行 `npm run build`，TypeScript 类型检查通过，Vite 编译耗时 2.57s 顺利产出静态资源产物，无任何构建错误。
2. **后端工程编译与测试**：
   - 执行 `mvn clean test-compile -DskipTests`，全平台 13 个子模块全部通过（`BUILD SUCCESS`）；
   - 执行 `mvn test -pl volur-security -am`，18 项单元测试全部通过。
3. **架构规范遵守**：
   - 严格遵循零代码注释铁律，所有技术方案记录于本外置文档；
   - 路由与 API 命名风格严格遵守规范。
