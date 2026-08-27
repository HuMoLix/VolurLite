# 注册图形验证码校验交互与邮箱验证码发送顺序重构

## 1. 变更背景
在用户注册流程中，为防止恶意攻击者利用接口枚举撞库、批量恶意消耗 SMTP 发信配额：
1. **前端交互层面**：注册表单发送邮箱验证码前，必须先确认图形验证码（或算术验证码）已正确完整填写，并根据后端下发的验证码类型（如 6 位字符图形验证码 `LINE`/`SHEAR` 或算术计算验证码 `MATH`）进行格式与完整度前置校验，未完成填写时禁止触发发信动作。
2. **后端业务层面**：邮箱验证码发送接口（`/api/v1/email/captcha/register`）必须严格按照“先核验图形验证码 / 人机凭证，再判定邮箱是否已被注册”的执行顺序流转。同时，修复了不同 Scene（场景）下图形验证码 Redis Key 匹配回退（`DEFAULT` 场景兼容）机制。

---

## 2. 改动点汇总

### 2.1 前端工程 (`volur-lite-view`)
1. **`src/components/auth/AuthForm.vue`**：
   - 引入 `captchaType` 状态记录服务端下发的验证码类型（`LINE`、`SHEAR`、`MATH` 等）。
   - 增加 `isCaptchaFilled` 计算属性及 `validateCaptchaCompletion` 强校验方法：针对 6 位字符验证码校验 `length === 6`，针对算术验证码校验数值格式；
   - 邮件验证码发送按钮绑定 `:disabled="countdown > 0 || !registerForm.email || (isCaptchaEnabled && !isCaptchaFilled)"`；
   - 在 `sendEmailVerificationCode()` 中前置校验图形验证码完成状态，未完成时提示相应警告；
   - 在表单规则 `registerRules` / `loginRules` 中增加图形验证码长度与格式的动态 Validator；
   - 发送失败或异常时自动刷新验证码并清空已填写的验证码输入框。
2. **`src/locales/zh_CN/auth.json` & `src/locales/en_US/auth.json`**：
   - 补充中英文提示文案：`graphic_captcha_length_6`、`math_captcha_invalid`、`fill_captcha_before_email`。

### 2.2 后端工程 (`volur-verification`)
1. **`EmailServiceImpl.java`**：
   - 强化 `sendVerificationEmail(EmailCaptchaRequestDTO request)` 执行顺序：优先调用 `checkCaptchaIfEnabled(request, CaptchaScene.REGISTER)` 进行人机核验，通过后再调用 `sendVerificationEmail(email, username, false)` 执行邮箱格式规范校验、数据库查重 (`userRepository.existsByEmail`) 及白名单过滤。
   - 增加对 `request == null` 的防御性校验。
2. **验证码 Provider 场景回退机制 (`LineCaptchaProvider`, `ShearCaptchaProvider`, `MathCaptchaProvider`, `SlidePuzzleCaptchaProvider`)**：
   - 在 `verify` 方法中，当根据传入的具体场景（如 `REGISTER`）未命中 Redis Key 时，自动回退查询通用默认场景（`DEFAULT`），解决了前端通过通用图形验证码接口获取验证码后在注册场景验证失败的问题。
3. **单元测试 (`EmailServiceVerificationTest.java`)**：
   - 增加针对 `sendVerificationEmail` 的单测用例，验证验证码错误时直接抛出 `400 BAD_REQUEST` 且不触发用户库查重，验证码正确且邮箱存在时抛出 `409 CONFLICT`，验证码正确且邮箱全新时正常触发发信任务。

---

## 3. 安全隐患修复与架构规范说明
1. **防止邮箱探测与撞库**：未完成图形验证码的请求在进入系统逻辑层前被直接阻断，攻击者无法通过发信接口探测目标邮箱是否已被注册。
2. **保护发信额度**：杜绝了绕过人机核验直接调用接口消耗 SMTP 配额的漏洞。
3. **严格遵循规范**：
   - 前端完全遵循 `view-Rules.md`：严格对齐 DTO 类型，杜绝 `any`，保持 Pinia Setup Store 与 Naive UI 体系一致；
   - 后端完全遵循 `Rules.md`：构造器注入、分层清晰、无冗余样板代码。

---

## 4. 影响范围与验证
- **影响模块**：`volur-verification`、`volur-auth`、`volur-lite-view` (注册/登录/发信)。
- **验证结果**：
  - 前端：`vue-tsc --noEmit && vite build` 编译打包 100% 通过。
  - 后端：`mvn test` 全平台模块 11/11 编译测试 100% 通过（新增 3 项验证码与发信顺序单测）。
