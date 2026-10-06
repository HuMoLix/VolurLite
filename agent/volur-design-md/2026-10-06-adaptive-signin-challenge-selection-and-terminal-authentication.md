# 自适应登录挑战选择与终端认证收敛架构实现

## 1. 变更背景与架构动机

在 Volur 平台的 Google 风格认证流中，前期实现了基于状态机模型（`Flow` / `SigninFlow`）、单步事务锁（`TransitionLock`）与分布式会话句柄（`dsh`）的安全控制。然而在深入审计与业务扩展中发现以下架构断层：
1. **挑战方式切换时的状态机脱节（Step Skipping 403 风险）**：
   - 默认流程在账号核验（`identifier`）通过后下发 `nextStep="pwd"`，TL 强哈希绑定于 `"pwd"` 步骤；
   - 当用户在 `pwdChallengeView.vue` 点击“试试其他方式”跳转至 `selectionView.vue` 并选择备选核验方式（如 `etotp` 邮件验证码）时，前端若直接导航至目标页面并提交，因 TL 绑定的步骤与提交的挑战类型不匹配，服务端 `TransitionLockManager.validateTransitionTicket` 会直接拦截并抛出“检测到非法越级跳转”异常；
2. **缺乏显式挑战分支切换与能力嗅探端点**：
   - 服务端缺少一个专门处理“用户主动改变当前核验方式”的状态机推进动作；且 `etotp`（邮件动态码）需要在用户选定该方式的瞬时由后端自动生成并发送 6 位验证码；
3. **认证终点节点收敛缺失（Terminal Authentication Step）**：
   - 登录凭据的签发（`accessToken` / `refreshToken`）与 HttpOnly 安全 Cookie 的注入被分散并内嵌在各个 challenge 分支（`case "pwd"` / `case "2fa"`）中，破坏了职责单一原则，且使得 `waitingSteps` 预置的 `"authenticate"` 步骤名存实亡。

---

## 2. 核心改动点与实现细节

### 2.1 后端工程 (`volur-auth` & `volur-security`)

1. **领域模型与请求 DTO 扩展 (`domain.dto`)**：
   - `SignInChallengeSelectRequestDTO`：包含 `dsh`, `tl`, `cid`, `targetChallengeType`，用于发起挑战方式切换；
   - `SignInAuthenticateRequestDTO`：包含 `dsh`, `tl`, `cid`, `rememberMe`，用于最终终端认证收敛发牌；
   - 严格遵循零注释与 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor` 规范。

2. **控制器层扩展 (`SigninController.java`)**：
   - 新增 `@PostMapping("/challenge/select")` 端点：对接 `signinService.selectChallenge`；
   - 新增 `@PostMapping("/authenticate")` 端点：对接 `signinService.authenticate`；
   - 严格遵循 Thin Controller 原则与 `ApiResponseEntity` 小驼峰工厂方法。

3. **服务层核心改造 (`SigninServiceImpl.java`)**：
   - **`identifier` 阶段**：
     - 当触发高危风控或爆破频次超限（`failures >= 5`）时，动态编排 `waitingSteps = ["recaptcha", "identifier", "authenticate"]`，签发 `nextStep="recaptcha"` 的 TL 并快速阻断（不执行数据库查询）；
     - 正常放行且确认用户存在后，编排 `waitingSteps = ["pwd", "authenticate"]`，签发默认首选 `nextStep="pwd"` 的 TL。
   - **`selectChallenge` 方式切换阶段**：
     - 校验当前会话与原 TL（调用 `validateStepAndTl` 防止未授权跳步）；
     - 校验用户真实能力（若选 `etotp` 必须具备安全邮箱）；
     - 若选择 `etotp`，自动调用 `emailService.sendVerificationEmail(email, username, true)` 投递 6 位动态验证码；
     - 重塑 `waitingSteps = [targetType, "authenticate"]`，递增 `cid` 并签发针对目标挑战类型的新 `TL`。
   - **`challenge` 挑战核验阶段**：
     - 增加 `case "etotp"` 分支：核验邮件验证码（`emailService.verifyCode`），核验通过后从 `waitingSteps` 移除 `etotp`；
     - 改造 `case "pwd"`、`case "2fa"`、`case "device"`：各前置挑战核验成功后，**不再直接生成 Token 或写入 Cookie**，而是核销自身步骤，将 `nextStep` 推进到 `"authenticate"`，签发针对 `"authenticate"` 的流转票据；
     - `case "recaptcha"` 分支：验证通过后核销 `recaptcha`，若用户存在平滑流转至密码或标识输入。
   - **`authenticate` 终端收敛发牌阶段**：
     - 校验 TL 步骤必须为 `"authenticate"`；
     - 确保 `flow.getWaitingSteps()` 仅剩 `"authenticate"`（所有前置挑战已真正通过）；
     - 调用 `transitionLockManager.burnAndCompleteSession` 物理销毁 Redis 会话与事务锁（防重放）；
     - 检查账号封禁与维护模式；
     - 由 `jwtUtil` 统一签发 `accessToken` 与 `refreshToken`，通过 `authCookieService.writeAuthCookies` 注入 HttpOnly 安全 Cookie，返回 `status: "SUCCESS"`。
   - **`getChallengeSelection` 真实能力过滤**：
     - 查验用户真实数据：仅当用户绑定了邮箱时提供 `etotp`，绑定了手机时提供 `totp`，排除了当前正在执行的 `currentType`。

### 2.2 前端工程 (`volur-lite-view-NEXT`)

1. **类型与 API 对齐 (`types/auth/flow.ts` & `api/auth.ts`)**：
   - 增加 `SignInChallengeSelectRequestDTO` 与 `SignInAuthenticateRequestDTO` 类型；
   - 封装 `selectSignInChallengeApi` (`POST /api/v1/auth/signin/challenge/select`) 与 `submitSignInAuthenticateApi` (`POST /api/v1/auth/signin/authenticate`)。

2. **状态机 Store 扩展 (`store/modules/flow/signinFlow.ts`)**：
   - 实现 `selectChallenge(targetType)`：调用换步接口并更新本地 `tl`, `cid`, `activeChallengeType`；
   - 实现 `submitAuthenticate(rememberMe)`：调用终端认证接口收敛登录；
   - 在 `submitChallenge` 内部实现透明终态桥接：当后端任意 challenge 校验成功且返回 `nextStep === 'authenticate'` 时，由 Store 自动发起 `submitAuthenticate` 请求完成发牌，对前端已有视图（`pwdChallengeView` 等）完全无感透明。

3. **交互视图优化 (`views/auth/signin/challenge/selectionView.vue`)**：
   - 在 `handleSelectMethod(item)` 中，先调用 `await signinFlowStore.selectChallenge(item.type)`，拿到后端签发的目标类型新 TL 后，再路由跳转到目标挑战页面，彻底消除越级跳步 403 风险。

---

## 3. 安全隐患修复与架构合规性说明

1. **彻底消除 Step Skipping 越级跳步与状态漂移**：
   - 用户切换认证方式从单纯的前端路由跳转，升级为前后端协同的事务级原子流转；每一张流转票据（TL）都严格绑定其对应的挑战动作。
2. **单一真实数据源与单一职责原则**：
   - 消除在多个 challenge 分支中分散书写 `writeAuthCookies` 和 `jwtUtil` 的代码异味；所有会话终结、发牌、写 Cookie 均由终端收敛节点 `/authenticate` 统一接管。
3. **Fail-Fast & Zero-Mock 原则**：
   - 当用户账号未绑定邮箱时切换 `etotp` 立即抛出明确异常；所有邮件验证严格通过真实 `EmailService` 校验与核销，杜绝伪造。

---

## 4. 验证与审计结论

1. **后端单元测试与集成测试验证**：
   - 运行 `mvn test -pl volur-auth -am`：全量 57 个测试用例（包括新增的 `challenge_pwd_success_shouldAdvanceToAuthenticate`、`authenticate_success_shouldWriteAuthCookiesAndReturnSuccess`、`selectChallenge_toEtotp_shouldSendEmailAndAdvanceTL`、`challenge_etotp_success_shouldAdvanceToAuthenticate`）**100% 通过（57/57 全绿，0 失败 0 错误）**。
2. **前端类型检查与生产构建验证**：
   - 在 `src/volur-lite-view-NEXT` 运行 `npm run build`：`vue-tsc --noEmit && vite build` 耗时 7.89s，0 警告 0 错误全绿构建成功。
3. **代码规范遵循**：
   - 所有改动的 Java 与 TypeScript 文件均严格保持纯粹代码与零行内注释（文档外置原则）。
