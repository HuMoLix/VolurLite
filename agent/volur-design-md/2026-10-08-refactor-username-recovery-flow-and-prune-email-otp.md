# 找回用户名流程重构与邮箱验证码剔除施工记录

## 1. 变更背景与需求分析
原找回用户名流程存在以下流程设计与安全隐患问题：
1. **时序与防刷前置缺失**：原流程中，用户在 `SelectionView.vue` 选择“电子邮箱”后直接进入 `EmailView.vue` 提交邮箱，随后才触发 reCAPTCHA 人机核验。这导致输入目标标识与验证码发送逻辑未前置进行人机防护，易被自动化程序针对性探测或造成资源浪费；
2. **账号枚举风险（Timing & Enumeration Leak）**：原流程在输入邮箱后立即进行用户及验证码发送，并通过前端 OTP 页面让用户输入验证码，向前端暴露或间接证实了邮箱关联状态；
3. **流程冗余与交互繁重**：找回用户名核心诉求仅为让合法所有者知晓账号用户名，要求用户输入验证码再在网页上显式展示用户名不仅繁琐，而且将用户名直接暴露在浏览器会话中。

为优化用户体验并强化安全防护，本次改造目标如下：
1. 在 `SelectionView.vue` 中选择任意一项找回方式后，强制优先跳转至 `/v1/recovery/challenge/reCAPTCHA` 进行前置人机验证；
2. reCAPTCHA 核验通过后，根据选择的找回方式再跳转至对应表单视图（如选择“电子邮箱”则跳转至 `EmailView.vue`）；
3. 在 `EmailView.vue` 输入邮箱提交后，后端仅在内部查询该邮箱是否存在关联用户，前端统一显示“邮件发送成功”以阻断账号枚举攻击。若邮箱存在关联用户，后端异步向其发送包含被找回用户名的精美通知邮件；
4. 彻底删除前端 `EmailOtpView.vue`、相关路由及 API，同步移除后端 OTP 校验接口、DTO、VO 与未使用的依赖。

---

## 2. 核心架构与改动点

### 2.1 后端工程重构 (Java & Spring Boot)
1. **邮件模板与邮件服务扩展**：
   - 新增 `recover_username_email.html` 找回用户名专用 HTML 邮件模板，沿用 Volur 标准品牌设计风格（深曜黑主按钮、卡片容器、多端自适应）；
   - 在 `EmailService` 接口声明 `sendRecoverUsernameEmail(String email, String username)`；
   - 在 `EmailServiceImpl` 中实现模板预加载 `RECOVER_USERNAME_TEMPLATE_NAME` 与发信逻辑，动态拼接前台登录入口链接；
2. **找回控制器瘦身 (`RecoveryController`)**：
   - 移除 `POST /api/v1/auth/recovery/username/emailotp` 与 `POST /api/v1/auth/recovery/username/emailotp/send`；
   - 保留 `/username/options`、`/username/recaptcha`、`/username/email`；
   - `submitRecoveryEmail` 统一返回受理成功提示响应 `ApiResponseEntity.ok(...)`；
3. **状态机与服务实现重构 (`RecoveryServiceImpl`)**：
   - `submitRecoveryRecaptcha`：校验 reCAPTCHA ticket，成功后跃迁状态机至 `email` 阶段，生成并下发对应阶段的 Transition Lock；
   - `submitRecoveryEmail`：
     - 校验邮件系统可用性（未配置时 Fast-Fail 抛出 `BusinessException`）；
     - 强校验 Transition Lock（确保经过了 reCAPTCHA 前置验证）；
     - 查找用户：`userRepository.findByEmail(normalizedEmail).ifPresent(user -> emailService.sendRecoverUsernameEmail(...))`；
     - 若用户不存在，静默跳过发信，彻底杜绝枚举攻击；
     - 调用 `transitionLockManager.burnAndCompleteSession(...)` 核销会话与锁；
   - 彻底移除 `verifyRecoveryEmailOTP`、`resendRecoveryEmailOTP` 方法及其对 `EmailVerificationCodeService` 的依赖；
4. **领域模型与测试用例同步更新**：
   - 物理删除 `UsernameRecoveryEmailOTPRequestDTO.java` 与 `UsernameRecoveryResultVO.java`；
   - 更新 `RecoveryControllerTest.java` 与 `RecoveryServiceImplTest.java`，覆盖新流程的所有测试场景。

### 2.2 前端工程重构 (Vue 3 / TypeScript / Naive UI)
1. **选项路由前置 (`SelectionView.vue`)**：
   - 用户点击找回选项后，携带 `recoveryMethod` 参数统一跳转至 `/v1/recovery/challenge/reCAPTCHA`；
   - `onMounted` 钩子自动调用 `ensureFlowParams` 保证全局会话状态参数完整；
2. **人机验证挑战桥接 (`recaptchaChallengeView.vue`)**：
   - “返回”按钮回退至 `/v1/recovery/selection`；
   - reCAPTCHA 挑战成功后，根据选中的 `recoveryMethod`（如 `EMAIL`）无缝跳转至 `/v1/recovery/username/email`；
3. **找回邮箱视图与状态提示 (`EmailView.vue`)**：
   - “返回”按钮回退至 `/v1/recovery/selection`；
   - 提交成功后切换至成功展示卡片，提示“找回邮件已发送”并提供“返回登录”操作跳转至 `/v1/signin/identifier`；
   - 前端统一提示成功，不泄漏邮箱关联状态；
4. **废弃组件与路由清理**：
   - 物理删除 `EmailOtpView.vue`；
   - 在路由配置 `src/router/routes/auth.ts` 中移除 `V1RecoveryUsernameEmailOtp`；
   - 在 API 模块 `src/api/auth.ts` 中移除 `verifyRecoveryEmailOTPApi` 与 `resendRecoveryEmailOTPApi`；
5. **多语言与视觉体系**：
   - 在 `zh-CN/auth.json`、`en-US/auth.json`、`zh-TW/auth.json` 中统一扩充 `recovery_email_tip`、`recovery_email_sent_title`、`recovery_email_sent_desc`。

---

## 3. 安全防护与审计说明
1. **反爬与防刷前置**：进入任何找回方式表单前强制通过 Cloudflare / Google reCAPTCHA 验证，阻断自动化请求泛滥；
2. **防账号枚举（Anti-Enumeration & Constant-Response）**：提交邮箱无论是否存在关联用户，前端均统一展示成功受理界面，避免攻击者借此探测系统有效注册邮箱；
3. **零注释铁律与代码纯洁性**：代码库全量遵循规范，零行内冗余注释，所有设计与审计信息统一外置收敛在此施工文档中。

---

## 4. 影响范围
- **后端模块**：`volur-auth`、`volur-verification`；
- **前端模块**：`volur-lite-view-NEXT` 认证找回路由与视图；
- **自动化测试**：Maven 单元测试全量通过，前端 `vue-tsc` 与 Vite 构建零警告零报错通过。
