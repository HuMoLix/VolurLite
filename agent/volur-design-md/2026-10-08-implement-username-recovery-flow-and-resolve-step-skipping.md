# 找回用户名完整业务状态机实现与消除非法越级跳转施工记录

## 1. 变更背景与问题溯源
在找回用户名流程中，用户在进入 `/v1/recovery/challenge/recaptcha` 页面加载人机验证组件时，控制台抛出 `403 FORBIDDEN` 错误：
`"检测到非法越级跳转，请按顺序完成验证"`。

### 1.1 根本原因剖析
- 后端防跃迁防护机制：[`TransitionLockManager.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/flow/TransitionLockManager.java) 在 `validateTransLock(dsh, tl, "recaptcha")` 中，严格核对当前设备会话状态机 `flow.getCurrentStep()` 是否与预期步骤 `"recaptcha"` 匹配；
- 状态机未流转问题：之前前端在 `/v1/recovery/username/email` 输入邮箱后，直接通过 Vue Router `router.push('/v1/recovery/challenge/recaptcha')` 导航至挑战页面，**未向服务端请求推进状态机**，会话中的步骤仍停留在上一阶段（如 `"pwd"` 或未推进），导致 `<Recaptcha>` 组件请求获取人机验证题目时被后端防跳步（Step Skipping）安全规则拦截并拒绝。

---

## 2. 核心架构与方案实施 (方案 A: 后端统一状态机推进)

### 2.1 后端领域模型与控制器升级 (volur-auth)
1. **新增请求入参 DTO**（统一收敛于 `com.humolix.volur.auth.domain.dto`）：
   - `UsernameRecoveryEmailRequestDTO`：接收 `dsh`、`email`（带 `@Trimmed`、`@NotBlank`、`@Email`）、`tl`、`cid`；
   - `UsernameRecoveryRecaptchaRequestDTO`：接收 `dsh`、`tl`、`cid`、`recaptchaTicket`（带 `@Trimmed`、`@NotBlank`）；
   - `UsernameRecoveryEmailOTPRequestDTO`：接收 `dsh`、`tl`、`cid`、`code`（带 `@Pattern(regexp = "^\\d{6}$")`）；
2. **新增响应视图 VO**：
   - `UsernameRecoveryResultVO`：返回找回的用户名列表 `usernames`；
3. **服务层状态机闭环实现 (`RecoveryServiceImpl.java`)**：
   - `submitRecoveryEmail`：
     - Fail-Fast 校验邮件服务是否开启配置；
     - 恢复/创建当前会话 Session，绑定 `identifier` 为目标邮箱；
     - 调用 `transitionLockManager.nextTransLock` / `createTransLock` 将流程推进至 `"recaptcha"`，签发当前步骤唯一的安全流转票据 `nextTL`，保存 Session；
     - 返回带有新 `tl` 和 `cid` 的 `SignInFlowResponseVO`；
   - `submitRecoveryRecaptcha`：
     - 强校验当前状态机步骤为 `"recaptcha"`，调用 `recaptchaService.validateAndBurnTicket` 校验并核销人机凭证（防重放）；
     - 调用 `emailService.sendVerificationEmail` 向安全邮箱发送 6 位动态验证码；
     - 推进状态机步骤至 `"emailotp"`，签发最新的 `nextTL` 与 `cid`；
   - `verifyRecoveryEmailOTP`：
     - 强校验当前步骤为 `"emailotp"`；
     - 调用 `emailVerificationCodeService.verifyCode` 核销 6 位验证码；
     - 查询匹配的账号列表，调用 `transitionLockManager.burnAndCompleteSession` 销毁流转锁并完成流程；
   - `resendRecoveryEmailOTP`：
     - 强校验步骤为 `"emailotp"`，重新发送邮件动态验证码；
4. **控制器端点暴露 (`RecoveryController.java`)**：
   - `POST /api/v1/auth/recovery/username/email`
   - `POST /api/v1/auth/recovery/username/recaptcha`
   - `POST /api/v1/auth/recovery/username/emailotp`
   - `POST /api/v1/auth/recovery/username/emailotp/send`

### 2.2 前端视图与流转对接 (volur-lite-view-NEXT)
1. **API 请求层扩展 (`src/api/auth.ts`)**：
   - 增加 `submitRecoveryEmailApi`、`submitRecoveryRecaptchaApi`、`verifyRecoveryEmailOTPApi`、`resendRecoveryEmailOTPApi`；
2. **邮箱输入视图 (`EmailView.vue`)**：
   - 输入有效邮箱后，点击“下一步”首先调用 `submitRecoveryEmailApi`；
   - 将服务端返回的最新 `tl` 与 `cid` 同步至 `signinFlowStore`，平滑导航至 `/v1/recovery/challenge/recaptcha`；
3. **人机验证挑战视图 (`recaptchaChallengeView.vue`)**：
   - 携带最新 `tl` 挂载 `<Recaptcha>` 组件，服务端校验状态机步骤通过，顺利出题并验证；
   - 用户验证通过后点击“下一步”，调用 `submitRecoveryRecaptchaApi` 核销凭据并发送验证码，将 `tl` 与 `cid` 推进至 `"emailotp"`，平滑导航至 `/v1/recovery/username/emailotp`；
4. **验证码校验视图 (`EmailOtpView.vue`)**：
   - 用户输入 6 位验证码后调用 `verifyRecoveryEmailOTPApi`；
   - 找回成功后回填账号并跳转回登录页继续登录；支持重发倒计时与修改邮箱回退。

---

## 3. 安全与规范审查 (Compliance Check)

- **消除越级隐患**：通过服务端在每个步骤完成时驱动状态机旋转，杜绝客户端擅自跳步，完全消除了“检测到非法越级跳转”的异常；
- **纯粹代码与零注释铁律**：所有新增和变更代码严格做到 0 解释性注释、0 行内备注；
- **全链路构建验证**：
  - 后端执行 `mvn test -pl volur-auth`，54 项测试全部通过（0 失败）；
  - 前端执行 `vue-tsc --noEmit && vite build`，编译打包全部通过（0 错误）。
