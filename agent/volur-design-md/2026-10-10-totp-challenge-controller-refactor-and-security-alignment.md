# 动态身份验证器（TOTP）向安全挑战控制器收敛与端到端重构报告

## 1. 方案背景与目标

在 Volur Lite 统一安全中心与身份认证体系中：
1. **API 对称性与领域收敛**：此前 通行密钥（Passkey）的创建选项与注册绑定已完整收敛至 `volur-security` 模块的 `PasskeyController`（路径 `/api/v1/captcha/challenge/pk`），而动态身份验证器（TOTP）的绑定逻辑却孤立在 `volur-auth` 的 `SignUpController`（路径 `/api/v1/auth/signup/protect/totp/*`）中，且 `TOTPChallengeController` 仅为一个空占位类。
2. **端点路径规范化**：
   - 获取 TOTP 绑定配置：统一采用 `/setup` 路径（`GET /api/v1/captcha/challenge/totp/setup`）；
   - 核验并首次启用 TOTP：统一重构为 `enableTotp`，采用 `/enable` 路径（`POST /api/v1/captcha/challenge/totp/enable`）；
   - 登录期 TOTP 安全挑战核销：统一重构为 `verifyTotp`，采用 `/verify` 路径（`POST /api/v1/captcha/challenge/totp/verify`）。
3. **彻底性与零废弃别名**：按照架构规范彻底清理 `volur-auth` 中历史遗留的 `SignUpTotp*` 实体类、接口和测试，全链路同步更新前端 API 与 Pinia Store，杜绝任何兼容性桥接与历史包袱。

---

## 2. 核心架构改造点

### 2.1 后端领域模型与控制器重构 (`volur-security`)

1. **统一领域模型**：
   - [`TotpSetupVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/TotpSetupVO.java)：包含 `secret`、`otpAuthUrl`、`account`、`issuer`；
   - [`TotpEnableRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/TotpEnableRequestDTO.java)：继承 `FlowRequestDTO`，入参严格约束 6 位数字并具备 `@Trimmed`；
   - [`TotpEnableResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/TotpEnableResponseVO.java)：返回启用状态 `success` 与反馈文案；
   - [`TOTPChallengeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/TOTPChallengeRequestDTO.java)：继承 `FlowRequestDTO`，承载登录挑战时的 6 位动态验证码与 `rememberMe` 选项。

2. **服务层抽取与实现 (`TotpService`)**：
   - 接口定义：[`TotpService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/TotpService.java)
   - 实现类：[`TotpServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/TotpServiceImpl.java)
     - `setupTotp`：校验状态机流转锁（`pt/options`），生成 RFC 4648 Base32 密钥，写入 Redis 临时缓存（`vredis:security:totp:temp:{dsh}`，TTL 10 分钟），签发 `otpauth://` URI；
     - `enableTotp`：校验流转锁与 6 位动态口令，读取 Redis 临时密钥，成功后持久化写入 `user.totp_secret` 并置位 `is_totp_enabled = true`，清除临时缓存；
     - `verifyTotp`：核销登录挑战，校验用户是否已开启 TOTP 与密钥有效性，防爆破与风控记录成功，推进状态机流转至 `authenticate` 步骤。

3. **控制器端点收敛 (`TOTPChallengeController`)**：
   - [`TOTPChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/TOTPChallengeController.java) 类级别注解 `@RequestMapping("/api/v1/captcha/challenge/totp")`：
     - `GET /setup`：`setupTotp(@RequestParam("dsh") String dsh, @RequestParam("tl") String tl)`
     - `POST /enable`：`enableTotp(@Valid @RequestBody TotpEnableRequestDTO request)`
     - `POST /verify`（支持 `""` 路由）：`verifyTotp(@Valid @RequestBody TOTPChallengeRequestDTO request)`

4. **安全挑战多路选择解耦 (`ChallengeServiceImpl.java`)**：
   - 在 `selectChallenge` 中彻底解耦 `totp` 与 `smsotp`，不再误判手机号绑定状态，而是校验用户的 `isTotpEnabled` 与 `totpSecret`；
   - 在 `getChallengeSelection` 中，当检测到用户开启了 TOTP 时，自动注入备选挑战选项 `/v1/signin/challenge/totp`。

### 2.2 彻底清理 `volur-auth` 历史遗留

1. 从 [`SignupController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java) 中彻底移除 `/protect/totp/setup` 与 `/protect/totp/verify` 端点；
2. 从 [`SignupService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/SignupService.java) 及 [`SignupServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java) 中移除 `setupTotp` 和 `verifyAndEnableTotp` 方法及临时缓存常量；
3. 物理删除旧领域实体 `SignUpTotpVerifyRequestDTO.java`、`SignUpTotpSetupVO.java`、`SignUpTotpVerifyResponseVO.java` 以及单测 `SignupServiceImplTotpTest.java`；
4. 在 `volur-security` 中新增完整的自动化单元测试：[`TotpServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/TotpServiceImplTest.java) 与 [`TOTPChallengeControllerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/controller/v1/challenge/TOTPChallengeControllerTest.java)。

### 2.3 前端 API 与视图全链路对齐 (`src/volur-lite-view-NEXT`)

1. **类型层 (`src/types/auth/flow.ts`)**：
   - 导出 `TotpSetupVO`、`TotpEnableRequestDTO`、`TotpEnableResponseVO`、`TOTPChallengeRequestDTO`。
2. **API 请求层 (`src/api/auth.ts`)**：
   - `fetchTotpSetupApi`: 调用 `GET /captcha/challenge/totp/setup`；
   - `submitTotpEnableApi`: 调用 `POST /captcha/challenge/totp/enable`；
   - `submitTotpChallengeApi`: 调用 `POST /captcha/challenge/totp/verify`。
3. **状态机与视图层**：
   - [`signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts)：将 `submitSignUpTotpVerify` 重命名为 `submitTotpEnable`，调用 `submitTotpEnableApi`；
   - [`totpProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue)：同步更新 API 引入与调用。

---

## 3. 测试与验证结果

1. **后端全量构建与单元测试**：
   - 执行 `mvn test -pl :volur-security,:volur-auth -f src/volur-lite-platform/pom.xml`；
   - `volur-security`：116 项测试全部通过（含 `TotpServiceImplTest`、`TOTPChallengeControllerTest`、`TotpUtilTest` 等）；
   - `volur-auth`：62 项测试全部通过；
   - 全模块构建成功（`BUILD SUCCESS`）。
2. **前端工程生产环境构建与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 2916 个模块编译完成，TypeScript 静态类型检查 0 错误，打包完全成功。
