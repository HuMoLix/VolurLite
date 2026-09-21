# 用户注册接口邮箱验证码绕过漏洞全链路分析与加固实施归档

- **执行日期**：2026-09-22
- **涉及模块**：`volur-auth`, `volur-verification`, `volur-system`, `volur-lite-view`
- **文档类型**：高危安全漏洞修复、前后端契约对齐与全链路业务加固
- **漏洞类型**：客户端控制安全决策 (CWE-602) / 身份验证绕过 (CWE-287)

---

## 1. 变更背景与漏洞复现分析

### 1.1 异常现象
在用户注册接口 `POST /api/v1/auth/register` 中，当客户端故意省略 `emailCaptcha` 请求入参（或者传入空字符串 `""`）时，后端绕过了邮箱验证码所有权核验流程，直接将该账号持久化至数据库并返回注册成功。

### 1.2 根因排查 (Root Cause)
1. **服务端条件反模式与短路失效**：
   在 [`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 原有实现中：
   ```java
   boolean requireEmailVerification = systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, false);
   String emailCaptcha = request.getEmailCaptcha();

   if (requireEmailVerification || StringUtils.hasText(emailCaptcha)) {
       if (!emailService.verifyCode(normalizedEmail, emailCaptcha)) {
           throw new InvalidCaptchaException("邮箱验证码错误或已过期");
       }
   } else {
       checkCaptchaIfEnabled(request.getCaptchaId(), request.getCaptchaCode(), request.getCaptchaTicket(), CaptchaScene.REGISTER);
   }
   ```
   - 在新实例中，`system.auth.require_email_verification` 未在 [`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java) 的 `ensureDefaultSettings()` 中默认初始化；
   - 导致 `requireEmailVerification` 默认回退为 `false`；
   - 此时判定直接退化为 `StringUtils.hasText(emailCaptcha)`，由客户端是否传值决定走哪个验证分支。攻击者只要不传该字段，即可直接跳过邮箱核验。
2. **回退分支安全防线被穿透**：
   - 绕过邮箱核验后落入 `else` 分支中的 `checkCaptchaIfEnabled(...)`；
   - 若系统人机验证 `system.captcha.enabled` 关闭，则该注册完全无校验；
   - 即使开启人机验证，攻击者只需先通过一次滑块验证拿到通用人机 Ticket，不上送 `emailCaptcha`，就能冒用任何他人的合法邮箱注册账号。
3. **前端状态感知与管理端脱节**：
   - 前端 [`AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue) 原先硬编码显示邮箱验证码输入框及必填校验；
   - [`system.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/system.ts) 状态库未暴露 `isEmailVerificationRequired`；
   - 后台管理端 [`AdminSystemView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue) 缺少对该配置项的可视化管理。

---

## 2. 修复实施细节

### 2.1 后端服务层安全决策权威源收敛
修改 [`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)：
1. 默认安全基线（Secure by Default）：`getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true)` 默认值改为 `true`；
2. 彻底移除 `|| StringUtils.hasText(emailCaptcha)` 客户端驱动反模式；
3. 当 `requireEmailVerification` 为 `true` 时：
   - 显式校验 `request.getEmailCaptcha()` 非空，为空直接抛出 `BusinessException(HttpStatus.BAD_REQUEST, "邮箱验证码不能为空")`；
   - 严格核验验证码，失败抛出 `InvalidCaptchaException("邮箱验证码错误或已过期")`；
   - 绝不回退至 `else` 分支；
4. 当 `requireEmailVerification` 为 `false` 时：
   - 降级走 `checkCaptchaIfEnabled(...)` 人机安全凭证核验，防止批量注册。

同时在 [`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java) 中统一采用 `SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true`。

### 2.2 系统配置默认值与增量迁移
1. 在 [`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java) 的 `ensureDefaultSettings()` 中添加：
   ```java
   initDefaultIfAbsent(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, "true", "AUTH", "BOOLEAN", true, "注册是否强制邮箱验证");
   ```
2. 在数据库增量升级脚本 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 中添加幂等初始化逻辑。

### 2.3 前端动态适配与管理控制台对齐
1. **Pinia 状态库** ([`system.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/system.ts))：
   - 新增并导出 `isEmailVerificationRequired = computed(() => settings.value['system.auth.require_email_verification'] !== 'false')`；
2. **认证表单组件** ([`AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue))：
   - 邮箱验证码输入框增加 `v-if="isEmailVerificationRequired"`；
   - `rules.emailCaptcha` 动态设定，当开启邮箱验证时为 `{ required: true }`，关闭时为空；
   - 当系统关闭邮箱验证（如内网/离线赛）但开启人机验证时，点击“立即注册”直接唤起滑块验证并将 `captchaTicket` 传入 `doRegister(ticket)`，前后端严格闭环；
3. **后台管理配置与国际化**：
   - [`AdminSystemView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue) 与 [`GeneralSettingsTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/GeneralSettingsTab.vue) 增加 `requireEmailVerification` 开关；
   - 补充中英文国际化语言包（[`zh_CN/admin/system.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/system.json)、[`en_US/admin/system.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/system.json)）。

### 2.4 单元测试验证
在 [`AuthVerificationCrossValidationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/AuthVerificationCrossValidationTest.java) 中补充/强化了 5 组核心用例：
- `testRegisterFailsWhenEmailVerificationRequiredAndCodeMissing`（缺失验证码时拒绝）；
- `testRegisterFailsWhenEmailVerificationFails`（验证码错误时拒绝）；
- `testRegisterPassesWhenEmailVerificationSucceeds`（验证码正确时放行）；
- `testRegisterPassesWhenCaptchaTicketIsValid`（关闭邮箱验证时依赖人机凭证通过）；
- `testRegisterRequiresValidCaptchaTicketWhenCaptchaEnabled`（关闭邮箱验证时伪造人机凭证拦截）。

---

## 3. 测试与验证结论
- **后端工程测试**：执行 `mvn test`，全平台 12 个模块（含 `volur-auth` 与 `volur-verification`）全部通过，测试用例 `BUILD SUCCESS`；
- **前端工程构建**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全站类型检查与打包 100% 成功。
