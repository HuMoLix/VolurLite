# 验证码 SDK 化重构与前后端自适应多模态安全协同审计文档

## 1. 变更背景与核心目标
在原系统实现中：
1. **模块散落与代码冗余**：验证码校验逻辑散落在各业务控制器与服务层（如 `volur-auth` 的 `AuthServiceImpl`、注册、改密等），业务层充斥着图形码提取、Redis 键核对、滑块 Ticket 校验等样板代码。
2. **缺乏账号维度风控矩阵**：原风控引擎 [`RiskEvaluationEngine`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java) 仅基于单一客户端 IP 进行滑动窗口频控，无法防御使用海量住宅代理 IP 池（Proxy Pool）针对单一目标账户的撞库与分布式爆破（Credential Stuffing / Password Spraying）。
3. **缺少统一的前后端交互契约**：入参分散，且前端各页面（登录、注册、找回密码等）各自手写图形码渲染、重载、弹窗触发及参数组装，维护成本极高。

本次重构对标 Google reCAPTCHA Enterprise 与 Cloudflare Turnstile 的工程实践，完成以下目标：
- 将验证码核验体系收敛封装为统一的 **`CaptchaSDK`** 门面；
- 标准化入参载荷为 **`CaptchaVerifyRequestDTO`**，支持业务 DTO 优雅组合复用；
- 引入「IP + 账号」双轨滑动窗口风控矩阵，实现「图形码 -> 滑块拼图凭证 -> 带外邮件安全码」阶梯式自适应跃迁；
- 设计并落地前端统一无感/自适应组件 **`VolurCaptcha.vue`**，彻底消除视图层手写验证码判断的样板代码。

---

## 2. 详细改动点清单

### 2.1 后端工程 (Java & Spring Boot)
1. **标准化入参载荷契约 [`CaptchaVerifyRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/CaptchaVerifyRequestDTO.java)**：
   - 收敛 `captchaId`, `captchaCode`, `captchaTicket`, `otpCode`, `scene`, `account`。
   - 严格遵循 `Rules.md` 规范：`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`，严禁 `@Builder` 与手写 Getter/Setter。
2. **自适应挑战决策视图 [`CaptchaChallengeDecisionVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/CaptchaChallengeDecisionVO.java)**：
   - 返回 `riskLevel`、`mode` (`NONE` / `IMAGE` / `SLIDER` / `EMAIL_OTP`)、`requireBehaviorVerify`、`requireOtp` 及预置图形码数据。
3. **风控引擎升维 [`RiskEvaluationEngine.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java)**：
   - 新增 `vredis:verification:risk:account_failures:` 前缀。
   - 提供 `evaluateAccountRisk(account, scene)`、`evaluateComprehensiveRisk(ip, account, scene)`、`recordAccountFailure` 及 `reduceAccountRisk`。
4. **统一 SDK 契约与实现 ([`CaptchaSDK.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDK.java) / [`CaptchaSDKImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDKImpl.java))**：
   - 聚合风控、人机挑战调度器与邮件异步投递；
   - 提供 `verify(dto, account, scene)` 单行原子核验方法；
   - 提供 `sendLoginOtp(account, captchaTicket)` 严密带外验证发送（强制滑块核验门禁防邮件轰炸）；
   - 提供 `recordSuccess` 与 `recordFailure` 闭环风控反馈。
5. **REST API 控制器扩展 [`CaptchaController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/CaptchaController.java)**：
   - `GET /api/v1/captcha/decision`：前端预检与自适应模式探测；
   - `POST /api/v1/captcha/send-otp`：高危状态下发送带外动态码。
6. **业务模块适配与解耦 ([`AuthenticateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AuthenticateRequestDTO.java) & [`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java))**：
   - `AuthenticateRequestDTO` 支持组合 `CaptchaVerifyRequestDTO captcha` 并向下兼容扁平字段；
   - `AuthServiceImpl` 注入 `CaptchaSDK`，`authenticate` 方法简化为单行调用 `captchaSDK.verify(...)` 并在认证成功/失败时回调风控闭环。

### 2.2 前端工程 (Vue 3 & TypeScript & Naive UI)
1. **类型与 API 对齐**：
   - [`src/volur-lite-view/src/types/verification/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/verification/index.ts) 与 [`src/volur-lite-view/src/api/verification/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/verification/index.ts) 严格同步后端契约；
   - [`src/volur-lite-view/src/types/auth/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts) 扩展登录请求载荷。
2. **统一多模态验证组件 [`VolurCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/verification/VolurCaptcha.vue)**：
   - 自动预检当前场景与账号的风控等级；
   - 自动在 `NONE`、`IMAGE`、`SLIDER`、`EMAIL_OTP` 四种交互形态间平滑切换；
   - 对外通过 `defineExpose` 暴露 `getPayload(): Promise<CaptchaVerifyRequestDTO>` 与 `handleFailure(err)`。
3. **业务视图整合 [`AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：
   - 剔除原本平铺硬编码的图形码图片、刷新按钮、点击事件等冗余样板代码；
   - 一行挂载 `<VolurCaptcha ref="loginCaptchaRef" scene="LOGIN" :account="loginForm.username" />`；
   - 提交时调用 `getPayload()`，失败时直接交由 `handleFailure(err)` 触发自适应提级。

---

## 3. 核心安全问题解答与架构保障

### 3.1 账号爆破导致他人业务受阻（账号闭锁 / 拒绝服务攻击）防范机制
若风控直接将恶意试错的账号封禁锁定，攻击者只需使用字典遍历全站账号故意输错密码，即可达成**全站用户拒绝服务（Account DoS）**。本方案遵循 Google 与 Okta 的专业安全实践，通过三层机制化解此风险：
1. **摩擦提级（Step-Up Friction）而非阻断业务（Hard Lockout）**：
   - 账号多次失败后，系统不会拒绝请求，而是**提升人机验证要求**：
     * 初阶：要求滑块拼图（自动化脚本失败，正常用户 1 秒滑过）；
     * 高阶：要求输入目标账号邮箱收到的 6 位 OTP 动态码。
   - **关键防线**：攻击者**没有**被攻击用户的邮箱控制权，因此被彻底阻断；而真实用户拥有自己的邮箱，即使受到撞库攻击，依然可以查收 OTP 并凭借正确密码成功登录，**正常业务不受破坏**。
2. **带外发信的防轰炸门禁（Anti-Bombing Gate）**：
   - 为防止黑客频繁触发邮件 OTP 骚扰受害者，调用 `/api/v1/captcha/send-otp` 接口**必须先完成高信誉滑块拼图核验（提供有效 ticket）**，大幅增加攻击者的计算与交互成本；
   - 服务端施加单个账号 60 秒发信冷却锁与单日总量上限。
3. **网络层降维打击（Tarpit & IP Throttling）**：
   - 真正的惩罚和频控直接施加在发起攻击的**恶意 IP / 子网**上，通过 Redis 滑动窗口拦截高频源头，实现「惩罚施加在攻击源，保护施加在受害账号」。

---

## 4. 验证结果与质量保障
1. **后端编译与自动化测试**：
   - 运行 `mvn test -pl volur-verification,volur-auth -am`：全量测试 17 个用例 100% 通过（`BUILD SUCCESS`）。
2. **前端编译与类型检查**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）：0 错误 0 告警，生产构建打包成功。
