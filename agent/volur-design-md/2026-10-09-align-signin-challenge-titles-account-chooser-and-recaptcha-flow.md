# 审计与施工记录：登录挑战标题对齐、账号选择器路由跳转与邮件/短信验证码前置 reCAPTCHA 安全防刷重构

## 1. 变更背景与需求目标

根据用户提出的系统优化与安全防护需求，本次针对登录流转（Sign-in Flow）进行了多处前端交互优化与全链路安全加固：
1. **密码验证挑战标题对齐**：修改 `pwdChallengeView.vue` 中左侧标题为“密码验证”（`auth.password_verification`），替代原先宽泛的“欢迎”；
2. **认证布局账号切换按钮交互修复**：修复认证卡片左侧头像与身份标识扩展区域（`#auth-header-extra`）中的账号切换气泡芯片（`.auth-account-chip`）点击行为，使其携带 `isScrim` 延迟（1000ms）及遮罩层跳转至账号选择器视图（`AccountChooserView.vue`，路径 `/v1/signin/accountchooser`），并防止 `AuthLayout.vue` 的全局卡片代理点击造成并发冲突；
3. **备选登录挑战标题对齐**：修改 `selectionView.vue` 中左侧标题为“更多登录方式”（`auth.more_signin_options`），替代原先的“触发 2 步安全验证”；
4. **邮箱挑战视图精简与对齐**：修改 `emailotpChallengeView.vue` 左侧标题为“邮箱验证”（`auth.email_verification`）；删除右侧冗余的“触发 2 步安全验证”提示横幅（`.auth-challenge-banner`）；将输入框标签从“输入 6 位安全验证码”对齐为“输入邮箱验证码”（`auth.enter_email_code_label`）；移除冗余的内联“改用其他方式”链接（保留底部标准的“试试其他方式”按钮）；
5. **邮箱并发与短信刷接口安全防御（前置 reCAPTCHA）**：在用户通过更多登录方式选择“电子邮箱验证码”或“手机短信验证码”登录时，后端严禁直接触发邮件发送或短信网关；而是在调用 `selectChallenge` 时前置插入一个 `recaptcha` 安全人机验证步骤，待客户端顺利完成人机核验并向 `/captcha/challenge/recaptcha` 提交凭据通过后，再行触发邮件发送或短信下发，并推进状态机至相应 OTP 输入挑战页，彻底规避未认证会话恶意高频触发验证码投递导致的邮件并发与资源耗尽风险。

---

## 2. 核心架构重构与改动详情

### 2.1 前端挑战视图与布局改造 (`volur-lite-view-NEXT`)
1. **[`pwdChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/pwdChallengeView.vue)**：
   - 将卡片左侧标题声明修改为 `useCardHeader(() => t('auth.password_verification'), '')`。
2. **[`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue)**：
   - 将 `#auth-header-extra` 中的 `.auth-account-chip` 点击事件从 `goBackIdentifier` 升级为 `handleSwitchAccount`；
   - 点击时执行 `await signinFlowStore.triggerScrim(undefined, 1000)`，并在延迟与进度条/遮罩生效后路由跳转至 `/v1/signin/accountchooser`（携带当前会话 query 参数）；
   - **清理多余 Toast 弹窗**：移除了在未检测到身份账号（`!signinFlowStore.identifier`）重定向回账号输入页（`IdentifierView`）时弹出的 `window.$message?.warning?.(t('auth.identifier_required'))`，避免进入输入页时出现突兀的“请输入邮箱或账户名称”Toast 提示。
3. **[`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)**：
   - 在卡片点击代理拦截器 `handleCardActionClick` 的白名单列表中添加 `scrimBtn.classList.contains('auth-account-chip')`，杜绝父子点击事件竞争或重复触发 Scrim。
4. **[`selectionView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/selectionView.vue)**：
   - 将卡片左侧标题声明修改为 `useCardHeader(() => t('auth.more_signin_options'), '')`；
   - 重构 `handleSelectMethod(item)`，根据 Fail-Fast Navigation 准则，使用 `await signinFlowStore.selectChallenge(item.type)` 返回的真实业务步骤推进前端路由（`/v1/signin/challenge/${nextStep}`），彻底杜绝前端硬编码。
5. **[`emailotpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/emailotpChallengeView.vue)**：
   - 将卡片左侧标题声明修改为 `useCardHeader(() => t('auth.email_verification'), '')`；
   - 彻底移除模板右侧顶部 `.auth-challenge-banner` 元素及其样式依赖；
   - 输入框标签绑定对齐为 `:label="t('auth.enter_email_code_label')"`；
   - 移除内联 `.auth-another-way-link` 链接并清理组件内对应的 SCSS 声明；保留底部统一的 `btn-try-another-way`。
6. **[`recaptchaChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/recaptchaChallengeView.vue)**：
   - 升级 `handleTryAnotherWayClick`，使其平滑路由至 `/v1/signin/challenge/selection?from=recaptcha&page=1`，使用户在人机验证阶段可自由退回切换其他可用方式。
7. **多语言国际化配置 (`src/volur-lite-view-NEXT/src/locales/`)**：
   - 同步在 `zh-CN/auth.json`、`zh-TW/auth.json` 与 `en-US/auth.json` 中增补以下键名映射：
     - `auth.password_verification`（密码验证 / 密碼驗證 / Password verification）
     - `auth.more_signin_options`（更多登录方式 / 更多登入方式 / More ways to sign in）
     - `auth.email_verification`（邮箱验证 / 信箱驗證 / Email verification）
     - `auth.enter_email_code_label`（输入邮箱验证码 / 輸入信箱驗證碼 / Enter email verification code）

### 2.2 后端安全防刷机制与流转状态机重构 (`volur-security`)
1. **会话元数据管理扩展 (`SessionManager.java`)**：
   - 新增 `removeMetadata(String dsh, String key)` 方法，支持在流转步骤消费完成后安全核销临时会话标签，避免状态污染。
2. **挑战切换选择器前置人机验证 (`ChallengeServiceImpl.selectChallenge`)**：
   - 当客户端请求切换至 `etotp`、`smsotp`（或历史别名 `totp`、`sms`）时，首先执行常规前置校验（账号是否存在、是否绑定邮箱或手机号）；
   - **严禁直接发信/发短信**：在会话元数据中安全存入 `pendingChallenge`（`etotp` 或 `smsotp`）；
   - 将认证流转步骤调整为 `["recaptcha", pendingStep, "authenticate"]`；
   - 生成针对 `recaptcha` 的下一个流转票据 TL，并将当前步骤标记为 `recaptcha`，直接向前端返回 `nextStep = "recaptcha"` 与 `status = "CHALLENGE"`。
3. **人机验证核验与安全降水投递 (`ChallengeServiceImpl.verifyReCAPTCHA`)**：
   - 人机核验通过并核销票据后，优先探测 `pendingChallenge` 会话元数据；
   - 若存在待定挑战：
     - 原子删除 `pendingChallenge` 元数据，调用 `riskEvaluationEngine.reduceRisk(clientIp)`；
     - 若待定挑战为 `etotp`：此时方安全调用 `emailService.sendVerificationEmail(...)` 向关联邮箱下发 6 位验证码；
     - 将流转步骤推进至待定挑战（`etotp` 或 `smsotp`），签发新 TL 并更新会话状态机；
     - 返回 `nextStep = "etotp"` 或 `"smsotp"`。

---

## 3. 验证与测试结果

1. **前端类型与编译检查**：
   - 执行 `npx vue-tsc --noEmit`，全项目 TypeScript 编译检查 100% 通过，0 错误，0 警告。
2. **后端单元与集成测试**：
   - 更新并补全 `ChallengeServiceImplTest`：
     - `selectChallenge_toEtotp_shouldRequireRecaptchaAndNotSendEmailYet` 成功断言选择邮箱验证时返回 `recaptcha` 步骤且未调用邮件服务；
     - `verifyReCAPTCHA_withPendingEtotp_shouldSendEmailAndAdvanceToEtotp` 成功断言通过人机核验后触发邮件发送并签发 `etotp` 步骤 TL；
   - 执行 `mvn test -pl volur-security,volur-auth`：
     - `volur-security` 全部 103 个测试用例通过；
     - `volur-auth` 全部 62 个测试用例通过；
     - 累计 165 个测试用例 100% 绿色通过，无任何破坏性回归。
