# Volur Lite 认证模块 (volur-auth) 与验证模块 (volur-verification) 交叉验证审计归档

- **执行日期**：2026-08-26
- **交叉验证模块**：`volur-auth`, `volur-verification`
- **文档类型**：系统间协议对齐、流程交叉核验与单元测试验证

---

## 1. 交叉验证背景与目标

为确保 [`volur-verification`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification) 重构加固后与 [`volur-auth`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth) 核心业务链路无缝对齐，本专项对用户注册、密码登录、重置密码、修改密码及发信等全生命周期链路进行了全面的接口、DTO 与业务逻辑交叉审查与单元测试验证。

---

## 2. 核心链路对齐与核销逻辑核查

### 2.1 用户注册链路 (`POST /api/v1/auth/register`)
- **人机安全验证 (`ENABLE_CAPTCHA`)**：
  - 携带 `captchaTicket`，通过 `captchaService.verifyTicket(captchaTicket, CaptchaScene.REGISTER)` 校验；
  - 自动触发防降级机制，强制核验 Ticket 的生成源是否达到 `SLIDE_PUZZLE`（滑块拼图）强度；
  - 校验成功后 Ticket 立即原子销毁，杜绝重放。
- **邮箱验证码 (`REQUIRE_EMAIL_VERIFICATION`)**：
  - 携带 `emailCaptcha`，通过 `emailService.verifyCode(normalizedEmail, request.getEmailCaptcha())` 核验；
  - 核销成功后立即从 Redis 移除，防止二次注册复用。

### 2.2 用户登录链路 (`POST /api/v1/auth/login`)
- **人机安全验证**：
  - 校验 `CaptchaScene.LOGIN` 场景的 Ticket 或传统验证码；
  - 支持普通低风险下的字符/算术码，当中高风控触发时平滑自动升级。
- **密码防爆破保护**：
  - 5 次连续失败自动触发 15 分钟账号锁定（`vredis:auth:login_lock:{account}`）。

### 2.3 找回与修改密码链路
- **找回密码申请 (`POST /api/v1/auth/forgot-password`)**：
  - 校验 `CaptchaScene.RESET_PASSWORD` 人机凭证；
  - 调用 `emailService.sendResetPasswordEmail` 发送 15 分钟有效的专属重置令牌。
- **邮箱验证码确认改密 (`POST /api/v1/auth/reset-password-confirm`)**：
  - 调用 `emailService.verifyCode` 完成原子核验并重置密码。
- **已登录改密 (`POST /api/v1/auth/password/verify-code` $\to$ `/password/change`)**：
  - 前置校验邮箱所有权与验证码，颁发短期变更凭证，完成后强制下线已签发的所有 JWT 会话。

---

## 3. 交叉测试验证结果

在 [`volur-auth`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth) 模块中新增并执行了交叉集成测试套件 [`AuthVerificationCrossValidationTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/AuthVerificationCrossValidationTest.java)：
1. **测试用例 1 (`testRegisterRequiresValidCaptchaTicketWhenCaptchaEnabled`)**：
   - 验证当系统开启人机验证时，伪造或过期的 Ticket 会准确触发 `InvalidCaptchaException` 并拒绝注册；
2. **测试用例 2 (`testRegisterPassesWhenCaptchaTicketIsValid`)**：
   - 验证合法的滑块 Ticket 能顺利通过注册，完成用户持久化与事件发布；
3. **测试用例 3 (`testRegisterFailsWhenEmailVerificationFails`)**：
   - 验证邮箱验证码输入错误或过期时被准确拦截。

执行 Maven 测试：
```bash
mvn test -pl volur-auth,volur-verification -am
```
所有测试用例 **100% 通过**，无任何失败与异常（`BUILD SUCCESS`）。
