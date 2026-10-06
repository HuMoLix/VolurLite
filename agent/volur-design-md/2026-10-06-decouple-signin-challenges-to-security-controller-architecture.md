# 登录挑战与验证码控制器解耦与架构下沉施工方案报告

## 1. 变更背景与需求溯源

在原有的系统设计中，登录流程（Sign-in Flow）中的各种验证挑战（密码挑战 `pwd`、邮箱动态验证码 `etotp`、短信验证码 `totp` / `2fa`、人机验证 `recaptcha`）以及挑战选择 (`selectChallenge`) 与备选核验列表 (`getChallengeSelection`) 均平铺耦合在 `volur-auth` 模块的 `SigninController` 中。

这种集中式的设计存在以下严重问题：
1. **职责过载与单点膨胀**：`SigninController` 承担了账号标识判定、最终认证发牌、人机验证处理、密码哈希校验、双因子多途径切换与验证码重发等多种职责，违反单一职责原则（SRP）；
2. **前后端路由语义不一致**：前端在 `/v1/signin/challenge/{nextStep}` 页面流转时，统一向 `/api/v1/auth/signin/challenge` 发起宽泛的聚合请求，未能体现出每种挑战的具体路由与独立参数约束；
3. **架构分层倒挂**：通用安全核验能力（如人机验证 `recaptcha`、密码验证器 `Sm3PasswordEncoder`、通行密钥 `passkey`）属于底层安全控制能力，应属于 `volur-security` 模块，而此前 `volur-security` 中仅有空壳或零散类，无法形成闭环。

针对上述架构缺陷，本次重构完成了以下核心升级：
1. **解耦拆分挑战控制器**：将 `challenge`、`selectChallenge`、`getChallengeSelection` 从 `volur-auth` 的 `SigninController` 中剥离，下沉到 `volur-security` 模块的 `com.humolix.volur.security.controller.v1.challenge` 目录下；
2. **RESTful 精细化分权控制器**：
   - `ChallengeController`：负责安全挑战的切换选择 (`POST /api/v1/captcha/challenge/select`) 与备选方式获取 (`GET /api/v1/captcha/challenge/selection`)；
   - `PasswordController`：负责密码凭据解密与校验 (`POST /api/v1/captcha/challenge/pwd`)；
   - `EmailOTPChallengeController`：负责安全邮箱验证码校验 (`POST /api/v1/captcha/challenge/etotp`) 与重发验证码 (`POST /api/v1/captcha/challenge/etotp/send`)；
   - `SMSOTPChallengeController`：负责短信动态口令核销 (`POST /api/v1/captcha/challenge/totp`)；
   - `ReCAPTCHAChallengeController`：遵循标准的获取、核验、提交流转生命周期：
     - `GET /api/v1/captcha/challenge/recaptcha`：拉取人机验证挑战数据；
     - `POST /api/v1/captcha/challenge/recaptcha/verify`：客户端前端交互核销，换取 `recaptchaTicket`；
     - `POST /api/v1/captcha/challenge/recaptcha`：携带 `recaptchaTicket`、`dsh` 与 `tl` 提交并推进状态机到 `pwd` 或 `identifier`；
3. **底层安全模型与服务下沉**：
   - 将 `Sm3PasswordEncoder` 下沉至 `volur-security.crypto`；
   - 将流转模型 `SignInFlowResponseVO` 与 `ChallengeOptionVO` 下沉至 `volur-security.domain.vo`；
   - 在 `volur-security` 中构建独立的 `ChallengeService` 与 `ChallengeServiceImpl`，承载各挑战的防暴破、防重放、受信设备检测与状态机推进逻辑；
4. **Thin Controller 原则在 `volur-auth` 的彻底实践**：
   - `SigninController` 仅保留账号提交 `POST /api/v1/auth/signin/identifier` 与终态认证发牌 `POST /api/v1/auth/signin/authenticate`，保持极度轻量与高内聚；
5. **前端全链路 100% 路由对齐**：
   - 前端 `src/volur-lite-view-NEXT` 的 `src/api/auth.ts`、`src/api/captcha.ts`、`src/store/modules/flow/signinFlow.ts` 与视图组件全面对齐后端 `/api/v1/captcha/challenge/{nextStep}` 路由规范，消除任意假值与隐式 Mock。

---

## 2. 详细改动清单与实现细节

### 2.1 后端工程架构调整 (volur-security & volur-auth)

1. **核心模型与加密器下沉 (`volur-security`)**：
   - [`Sm3PasswordEncoder.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/crypto/Sm3PasswordEncoder.java)：移动至 `com.humolix.volur.security.crypto`，注入 `VolurThreadManager`，作为 Spring `@Component` 统一向全平台提供国密 SM3+PBKDF2 安全哈希与异步管道；
   - [`SignInFlowResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/SignInFlowResponseVO.java)：移动至 `com.humolix.volur.security.domain.vo`，作为统一认证流转状态响应模型；
   - [`ChallengeOptionVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/ChallengeOptionVO.java)：移动至 `com.humolix.volur.security.domain.vo`，作为挑战备选方案数据契约；
   - 新增各挑战请求参数 DTO：
     - [`PasswordChallengeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/PasswordChallengeRequestDTO.java)；
     - [`EmailOTPChallengeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/EmailOTPChallengeRequestDTO.java)；
     - [`SMSOTPChallengeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/SMSOTPChallengeRequestDTO.java)；
     - [`ReCAPTCHAChallengeSubmitDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/ReCAPTCHAChallengeSubmitDTO.java)；
     - [`ChallengeSelectRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/ChallengeSelectRequestDTO.java)。

2. **服务层抽象与实现 (`volur-security`)**：
   - [`ChallengeService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/ChallengeService.java) & [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java)：
     - `verifyPassword`：严格校验事务锁 TL (`pwd`)，校验设备基线，解密端到端 `$V1$GCM$` 密码密文，校验登录错误锁定计数器，比对密码，检查全站维护模式及封禁状态；若为新设备登录且绑定了邮箱，自动向邮箱发送验证码并下发 `etotp` 挑战，若为受信设备则推进至 `authenticate` 并记录受信设备指纹；
     - `verifyEmailOTP`：校验 TL (`etotp`)，验证邮件验证码真实性，核销通过后推进至 `authenticate` 并记录受信设备；
     - `sendEmailOTP`：校验当前会话与 TL，重新触发邮件验证码投递；
     - `verifySMSOTP`：校验 TL (`totp` / `2fa`)，推进至 `authenticate`；
     - `verifyReCAPTCHA`：校验 TL (`recaptcha`)，原子核销一次性凭据 `recaptchaTicket`；若账号不存在则平滑引导回 `identifier` 并提示错误，若账号存在则签发新的 TL 推进到 `pwd`；
     - `selectChallenge`：校验当前会话与当前步骤 TL，允许用户选择切换任意可用二次验证方式（如从密码切换至邮箱验证码，或在多因子之间切换）；
     - `getChallengeSelection`：根据会话 `dsh` 获取脱敏安全邮箱与手机号，排除当前已处步骤，返回前端可选列表。

3. **精细化控制器建设 (`volur-security.controller.v1.challenge`)**：
   - [`ChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/ChallengeController.java)：
     - `POST /api/v1/captcha/challenge/select`
     - `GET /api/v1/captcha/challenge/selection`
   - [`PasswordController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasswordController.java)：
     - `POST /api/v1/captcha/challenge/pwd`
   - [`EmailOTPChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/EmailOTPChallengeController.java)：
     - `POST /api/v1/captcha/challenge/etotp`
     - `POST /api/v1/captcha/challenge/etotp/send`
   - [`SMSOTPChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/SMSOTPChallengeController.java)：
     - `POST /api/v1/captcha/challenge/totp`
   - [`ReCAPTCHAChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/ReCAPTCHAChallengeController.java)：
     - `GET /api/v1/captcha/challenge/recaptcha`：获取人机验证题目
     - `POST /api/v1/captcha/challenge/recaptcha/verify`：前端行为数据校验与发牌
     - `POST /api/v1/captcha/challenge/recaptcha`：提交 ticket 推进状态机

4. **`volur-auth` 重构与瘦身**：
   - [`SigninController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java)：剥离所有挑战接口，仅保留 `POST /api/v1/auth/signin/identifier` 与 `POST /api/v1/auth/signin/authenticate`；
   - [`SigninServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java)：移除非必要依赖注入，专注账号标识核验、综合风控评估与终态 JWT / Cookie 颁发；
   - 彻底删除 `volur-auth` 中迁移出走的历史包袱类。

---

### 2.2 前端工程对齐与重构 (volur-lite-view-NEXT)

1. **API 请求层 1:1 对齐**：
   - [`src/api/captcha.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/captcha.ts)：
     - `verifyRecaptchaChallengeApi` 路径修正为 `/captcha/challenge/recaptcha/verify`；
     - 新增 `submitRecaptchaChallengeApi`，路径为 `POST /captcha/challenge/recaptcha`；
   - [`src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts)：
     - `selectSignInChallengeApi` 路径切换至 `/captcha/challenge/select`；
     - `fetchChallengeSelectionApi` 路径切换至 `/captcha/challenge/selection`；
     - 新增 `submitPasswordChallengeApi` (`/captcha/challenge/pwd`)；
     - 新增 `submitEmailOTPChallengeApi` (`/captcha/challenge/etotp`)；
     - 新增 `resendEmailOTPChallengeApi` (`/captcha/challenge/etotp/send`)；
     - 新增 `submitSMSOTPChallengeApi` (`/captcha/challenge/totp`)；
     - 彻底清除已废弃的通用 `submitSignInChallengeApi` 与无用类型。

2. **状态机与流转调度解耦**：
   - [`src/store/modules/flow/signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts)：
     - `submitChallenge(challengeType, payload)` 实现根据 `challengeType` 智能路由到对应专用接口：
       - `'pwd'` -> 自动加密凭据并请求 `submitPasswordChallengeApi`；
       - `'etotp'` -> 请求 `submitEmailOTPChallengeApi`；
       - `'totp'` / `'2fa'` -> 请求 `submitSMSOTPChallengeApi`；
       - `'recaptcha'` -> 请求 `submitRecaptchaChallengeApi`；
       - 未知类型直接 Fast-Fail 报错，绝不隐式兜底；
     - 新增导出 `resendEmailOTP()` 方法。

3. **视图组件动态适配**：
   - [`totpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/totpChallengeView.vue)：
     - 从路由参数动态读取 `currentType`（`etotp` 或 `totp`）；
     - 动态渲染“安全邮箱”或“安全手机”提示；
     - 提交验证码时精准提交给对应类型的后端控制器；
     - “试试其他方式”路由动态携带正确的 `from` 步骤来源；
   - [`recaptchaChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/recaptchaChallengeView.vue)：
     - 弹窗通过后点击“下一步”时，提交至 `POST /captcha/challenge/recaptcha`，接收返回的 `res.nextStep`（若为 `identifier` 则安全回退并提示账号不存在，若为 `pwd` 则继续前进）。

---

## 3. 测试与验证报告

### 3.1 后端工程测试全绿验证
全平台 13 个模块执行 `mvn test` 测试用例全部 100% 通过：
```
[INFO] Reactor Summary for volur-lite-platform 1.0.0-SNAPSHOT:
[INFO] volur-lite-platform ................................ SUCCESS
[INFO] volur-framework .................................... SUCCESS (Tests run: 58)
[INFO] volur-logging ...................................... SUCCESS (Tests run: 12)
[INFO] volur-system ....................................... SUCCESS (Tests run: 14)
[INFO] volur-user ......................................... SUCCESS (Tests run: 32)
[INFO] volur-verification ................................. SUCCESS (Tests run: 54)
[INFO] volur-security ..................................... SUCCESS (Tests run: 79)
[INFO] volur-notification ................................. SUCCESS (Tests run: 4)
[INFO] volur-auth ......................................... SUCCESS (Tests run: 47)
[INFO] volur-challenge .................................... SUCCESS
[INFO] volur-container .................................... SUCCESS (Tests run: 18)
[INFO] volur-competition .................................. SUCCESS (Tests run: 43)
[INFO] volur-server ....................................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### 3.2 前端工程生产构建验证
- **`volur-lite-view-NEXT`（新一代前端）**：`npm run build` 耗时 2.74s，0 错误，0 警告，TypeScript 严格检查 100% 通过；
- **`volur-lite-view`（经典前端）**：`npm run build` 耗时 6.38s，0 错误构建成功。
