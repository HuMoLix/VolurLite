# Passkey (通行密钥) 安全核验挑战链路全通与控制器包路径统一归集设计方案

## 1. 变更背景与问题溯源

在 Volur Lite 账号体系的安全认证演进过程中，用户在注册账号阶段已支持绑定基于 FIDO2 / WebAuthn 的硬件密钥与设备生物识别（Windows Hello、Touch ID、USB 物理安全密钥等）。但在登录阶段，当用户在安全核验备选方式列表（`selectionView.vue`）中选择通行密钥或触发设备核验挑战时，前端控制台直接抛出异常：
```
不支持的安全核验挑战类型: device
```
且浏览器网络面板中没有任何后端请求发出。

### 1.1 根因剖析 (Root Cause Analysis)

1. **前端 Store 状态机缺乏 `device` / `pk` / `passkey` 挑战分支**：
   - 在 `src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts` 的 `submitChallenge` 方法中，条件分支硬编码仅判断了 `pwd`、`etotp`、`totp/2fa`、`recaptcha`。
   - 当用户在选择界面点击“使用安全密钥或通行密钥”或进入 `/v1/signin/challenge/device` 页面并触发核验时，传入挑战类型为 `'device'`，直接穿透至 `else` 严格分支触发 `throw new Error(\`不支持的安全核验挑战类型: ${challengeType}\`)`，导致在向后端发起网络请求前就被前端阻断拦截。

2. **视图层 `deviceChallengeView.vue` 与真实 Passkey WebAuthn 断言脱节**：
   - 现有的 `deviceChallengeView.vue` 仅渲染了一个基础的设备提醒，并传入空对象 `{}` 调用 `submitChallenge('device', {})`，缺少完整的 WebAuthn 握手链路（获取 Assertion Options -> 浏览器唤起 `navigator.credentials.get` -> 收集凭据载荷提交校验）。

3. **后端缺少登录态 Passkey 断言挑战 (Assertion) 支持**：
   - 原 `PasskeyController` 仅提供了注册态的 Creation Options 与凭据保存，缺乏登录认证断言挑战的生成与密码学校验（Assertion Verification）端点。

4. **控制器包架构离散**：
   - 原 `PasskeyController.java` 平铺在 `com.humolix.volur.security.controller.v1` 根目录下，未与统一拆分后的其他 5 个安全核验控制器（`PasswordController`、`EmailOTPChallengeController`、`SMSOTPChallengeController`、`ReCAPTCHAChallengeController`、`ChallengeController`）归集于 `controller/v1/challenge/` 子包下。

---

## 2. 架构设计与改动点

### 2.1 后端控制器包路径重构与端点统一
- **物理路径迁移**：
  将 `PasskeyController.java` 从 `com.humolix.volur.security.controller.v1.PasskeyController` 完整平移归入 `com.humolix.volur.security.controller.v1.challenge.PasskeyController`。彻底删除旧路径文件，更新所有 import。
- **端点对齐与功能增强**：
  保持类级 `@RequestMapping("/api/v1/captcha/challenge/passkey")`：
  - `GET /api/v1/captcha/challenge/passkey`：保留原有用户注册时创建选项获取；
  - `POST /api/v1/captcha/challenge/passkey`：保留原有通行密钥注册保存；
  - `GET /api/v1/captcha/challenge/passkey/authenticators`：保留支持的认证器列表；
  - **新增** `GET /api/v1/captcha/challenge/passkey/assertion-options`：登录态安全核验时获取基于 Spring Security WebAuthn 的断言选项（`PublicKeyCredentialRequestOptions`）；
  - **新增** `POST /api/v1/captcha/challenge/passkey/verify`：登录态验证 WebAuthn 断言凭据，校验密码学签名、推进 Flow 状态机与下发新 TL。

### 2.2 服务层业务逻辑扩充 (`PasskeyService` & `PasskeyServiceImpl`)
1. **断言选项生成 (`getAssertionOptions`)**：
   - 严格进行会话基线探测与 `TransitionLockManager.validateTransLock` 校验（当前步骤支持 `pk` 或 `device`）；
   - 根据会话绑定的标识定位用户实体；
   - 调用 `rpOperations.createCredentialRequestOptions` 生成带有该用户已注册 credentialId 白名单及 challenge 的断言配置；
   - 序列化并通过 Redis 进行 5 分钟安全暂存 (`vredis:security:passkey:assertion:<dsh>`)。
2. **断言核验与状态推进 (`verifyPasskeyAssertion`)**：
   - 校验会话状态与 TL 锁；
   - 反序列化凭据载荷为 `PublicKeyCredential<AuthenticatorAssertionResponse>`；
   - 组装 `RelyingPartyAuthenticationRequest` 调用 `rpOperations.authenticate(...)` 完成密码学签名、计数器及凭据归属验证；
   - 校验成功后清除 Redis 缓存，记录设备信任与风控评分；
   - 推进 `SigninFlow` 完成当前步骤（`completeStep`），流转至 `authenticate` 终态，生成并颁发下一阶段 TL。

### 2.3 前端 API 与状态机支持 (`api/auth.ts` & `signinFlow.ts`)
1. **API 层**：
   - `fetchPasskeyAssertionOptionsApi`：请求 `/captcha/challenge/passkey/assertion-options`；
   - `submitPasskeyChallengeApi`：请求 `/captcha/challenge/passkey/verify`。
2. **`signinFlow.ts`**：
   - 在 `submitChallenge` 中添加 `challengeType === 'device' || challengeType === 'pk' || challengeType === 'passkey'` 判定分支；
   - 携带 `credential` 载荷调用 `submitPasskeyChallengeApi`；
   - 继承通用的 `nextStep === 'authenticate'` 自动收尾流转，直接获取认证令牌完成登录。

### 2.4 视图层全功能重构 (`deviceChallengeView.vue`)
- 全面按照 Google Material Design / Workspace 色彩体系实现；
- 具备完善的 Base64URL 与 ArrayBuffer 双向编解码工具链；
- 点击“验证”后：
  1. 调用 `fetchPasskeyAssertionOptionsApi` 获取断言配置；
  2. 映射 `allowCredentials` 与 `challenge` 为 ArrayBuffer；
  3. 调用浏览器 `navigator.credentials.get({ publicKey: ... })` 唤起设备原生生物识别或硬件安全密钥；
  4. 转换并提交凭据至 `signinFlowStore.submitChallenge('device', ...)`；
  5. 验证成功后自动重定向至比赛大厅，提供取消及优雅错误反馈。

---

## 3. 安全防护与审计说明

1. **防重放与跨步骤跃迁**：
   - `getAssertionOptions` 与 `verifyPasskeyAssertion` 均严格绑定 DSH 与动态 TL 校验，凭据单次使用即销毁；
   - 断言选项存入 Redis 设置 5 分钟绝对 TTL，防止旧挑战被攻击者持久利用。
2. **严格对象反序列化过滤器 (ObjectInputFilter)**：
   - 白名单仅放行 `org.springframework.security.web.webauthn.**` 与 `java.base/*`，杜绝反序列化远程代码执行漏洞。
3. **Fail-Fast & Zero-Mock 铁律**：
   - 绝不使用前端假数据或跳步伪造凭据，所有断言签名均经底层密码学引擎检验。

---

## 4. 影响范围与验证结果

- **后端工程**：
  - 13 个子模块 `mvn test` 100% 通过（共 258+ 测试用例全部绿灯）；
  - `PasskeyServiceImplTest` 覆盖 options 生成、注册保存、序列化往返、登录断言生成与核验完整链路。
- **前端工程**：
  - `src/volur-lite-view-NEXT` 执行 `npm run build` 成功，0 错误；
  - `src/volur-lite-view` 执行 `npm run build` 成功，0 错误。
