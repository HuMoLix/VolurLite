# 找回用户名与找回密码独立流程规范化实现与全链路重构

## 1. 变更背景与架构演进
在原有的身份恢复流程中，找回用户名与找回密码混用同一路径，且由于跳转回登录页后旧会话的分布式会话句柄（DSH）与流转票据（TL）未彻底销毁，容易导致再次进入时触发凭据过期并跳转至 `sessionExpireView`。此外，找回密码缺乏完整的身份验证闭环（选择安全验证方式 -> 人机核验 -> 邮箱动态码验证 -> 重置新密码）。

为彻底分离两种业务流程并解决凭据泄露及悬挂问题，本期工程对前后端找回流程进行了全面规范化实施与重构：
1. **忘记用户名流程 (Username Recovery)**：
   `/v1/signin/identifier` -> `/v1/recovery/username/selection` -> `/v1/recovery/challenge/recaptcha` -> `/v1/recovery/username/email` -> 邮件发送成功并即时调用 `resetFlow()`，引导返回 `/v1/signin/identifier`。
2. **忘记密码流程 (Password Recovery)**：
   `/v1/signin/identifier` -> `/v1/signin/challenge/pwd` -> `/v1/recovery/password/selection` (选择安全邮箱) -> `/v1/recovery/challenge/recaptcha` (完成人机验证) -> `/v1/recovery/password/emailotp` (输入邮箱验证码) -> `/v1/recovery/password/reset` (设置新密码并加密提交) -> 提交成功，服务端销毁凭据并终结会话，前端即时 `resetFlow()` 并提示成功，返回 `/v1/signin/identifier`。

---

## 2. 改动点清单

### 2.1 后端工程 (`volur-lite-platform`)
1. **领域模型与入参收敛 (`volur-auth/domain`)**：
   - 新增 `PasswordRecoverySelectRequestDTO`：继承 `FlowRequestDTO`，封装验证方式选择（`method`）；
   - 新增 `PasswordRecoveryEmailOtpRequestDTO`：继承 `FlowRequestDTO`，封装 6 位数字验证码（`code`）；
   - 新增 `PasswordRecoveryResetRequestDTO`：继承 `FlowRequestDTO`，封装新密码与确认密码；
   - 新增 `PasswordRecoveryOptionsVO`：承载脱敏安全邮箱（`maskedEmail`）与脱敏手机号（`maskedPhone`）及可用验证方式列表。
2. **服务层逻辑实现与会话跃迁 (`RecoveryServiceImpl`)**：
   - `getPasswordRecoveryOptions(dsh)`：基于 DSH 解析绑定用户，动态根据系统设置状态返回可用找回方式与脱敏信息；
   - `selectPasswordRecoveryMethod(request)`：验证邮件/短信服务启用状态，签发面向 `recaptcha` 挑战的 TL 并记录 `recoveryType="password"` 元数据；
   - `sendPasswordRecoveryEmailOtp(dsh, tl)`：校验 `recaptcha` 状态与邮箱配置，生成 6 位 OTP 并通过 `EmailService` 发送，防重放与频控；
   - `verifyPasswordRecoveryEmailOtp(request)`：核销 6 位 OTP，将 TL 安全跃迁至 `reset` 步骤并标记 `emailVerified=true`；
   - `submitPasswordReset(request)`：校验 `reset` 步骤 TL 与身份核验标记，解密 GCM 密文或处理明文，强密码复杂度校验，更新密码哈希并通过 `burnAndCompleteSession` 终局销毁会话与全部凭据。
3. **安全核验多态流转 (`ChallengeServiceImpl`)**：
   - 在 `verifyReCAPTCHA` 成功后，检测会话中 `recoveryType`：
     - 若为 `"password"`，跃迁至 `emailotp` 步骤（签发 `emailotp` TL 并返回 `nextStep: "emailotp"`）；
     - 若为找回用户名，则跃迁至 `email` 步骤（签发 `email` TL 并返回 `nextStep: "email"`）。
4. **控制层端点规范化 (`RecoveryController`)**：
   - 暴露 `/api/v1/auth/recovery/password/options` (GET)；
   - 暴露 `/api/v1/auth/recovery/password/select` (POST)；
   - 暴露 `/api/v1/auth/recovery/password/emailotp/send` (POST)；
   - 暴露 `/api/v1/auth/recovery/password/emailotp/verify` (POST)；
   - 暴露 `/api/v1/auth/recovery/password/reset` (POST)。

### 2.2 新一代前端工程 (`volur-lite-view-NEXT`)
1. **类型定义与 API 封装**：
   - 在 `types/auth/recovery.ts` 中声明 `PasswordRecoveryOptionsVO`、`PasswordRecoverySelectRequestDTO`、`PasswordRecoveryEmailOtpRequestDTO`、`PasswordRecoveryResetRequestDTO`；
   - 在 `api/auth.ts` 中封装 `getPasswordRecoveryOptionsApi`、`selectPasswordRecoveryMethodApi`、`sendPasswordRecoveryEmailOtpApi`、`verifyPasswordRecoveryEmailOtpApi`、`submitPasswordResetApi`。
2. **路由架构与目录迁移 (`router/routes/auth.ts`)**：
   - 将原平铺的 `SelectionView.vue` 迁移至 `views/auth/recovery/username/SelectionView.vue` 并物理删除旧文件；
   - 配置找回用户名路由：
     - `/v1/recovery/username/selection` (`V1RecoveryUsernameSelection`)
     - `/v1/recovery/username/email` (`V1RecoveryUsernameEmail`)
   - 配置找回密码路由：
     - `/v1/recovery/password/selection` (`V1RecoveryPasswordSelection`)
     - `/v1/recovery/password/emailotp` (`V1RecoveryPasswordEmailOtp`)
     - `/v1/recovery/password/reset` (`V1RecoveryPasswordReset`)
   - 根路径 `/v1/recovery` 默认重定向至 `/v1/recovery/username/selection`。
3. **视图实现与状态机闭环**：
   - `views/auth/recovery/password/SelectionView.vue`：展示脱敏邮箱并选择找回方式，核验通过进入 `recaptcha`；
   - `views/auth/recovery/challenge/recaptchaChallengeView.vue`：根据后端响应的 `nextStep` 快速失败跳转（`emailotp` -> 密码流程，`email` -> 用户名流程），返回按钮根据 `recoveryType` 分流；
   - `views/auth/recovery/password/EmailOtpView.vue`：挂载即发码并启动 60 秒防抖倒计时（取消发码 toast 弹窗，由界面倒计时与副标题清晰反馈），输入 6 位数字核销后跳转 `reset`；
   - `views/auth/recovery/password/ResetView.vue`：输入新密码与确认密码，使用 `encryptFlowCredential` GCM 加密提交，重置成功后执行 `signinFlowStore.resetFlow()` 并直接引导回登录入口（取消成功 toast 弹窗）；
   - 同步修正 `IdentifierView.vue` 与 `pwdChallengeView.vue` 的找回跳转路径。
4. **多语言国际化字典补齐**：
   - 在 `zh-CN/auth.json`、`en-US/auth.json`、`zh-TW/auth.json` 中全量补齐找回密码相关文案。

---

## 3. 安全隐患修复与合规审计
1. **防止会话悬挂与凭据污染**：
   在找回密码终局（重置密码完成）与找回用户名终局（邮件提交完成）以及用户主动返回登录页时，全链路强制调用 `resetFlow()`，销毁本地 Pinia Store、缓存与路由参数中的 DSH 和 TL，杜绝旧凭据失效导致 401/403 异常弹窗或误跳超时页面。
2. **消除隐式 Mock 与假值反模式 (Fail-Fast & Zero-Mock 原则)**：
   人机核验与邮箱验证码流转中，完全由后端返回的真实 `res.data.nextStep` 驱动路由跳转，若缺失或异常则直接抛出强类型错误阻断，杜绝前端私自假定默认步骤。
3. **密码端到端防窃听传输**：
   重置密码请求体中的密码及确认密码均经过平台 `encryptFlowCredential`（WebCrypto AES-GCM 结合 DSH 会话密钥）加密，后端解密核验一致性与强度后方可写入持久层。

---

## 4. 影响范围与验证结果
- **后端测试**：`mvn test -pl volur-auth -Dtest=Recovery*` (12/12 单元测试通过)，`mvn test -pl volur-security -Dtest=Challenge*` (6/6 单元测试通过)。
- **前端测试**：`vue-tsc --noEmit && vite build` 生产构建成功，无任何类型错误或模板编译异常。
