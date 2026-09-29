# Google Accounts 规范分布式会话句柄 (dsh) 与状态流转锁 (TL) 架构演进与密码挑战收归施工记录

- **施工日期**：2026-09-29
- **施工模块**：
  - 后端核心框架：`volur-framework` (`com.humolix.volur.framework.security.flow`)
  - 后端认证中心：`volur-auth` (`controller/v1`, `service`, `domain/dto`, `domain/vo`, `config`)
  - 新一代前端系统：`volur-lite-view-NEXT` (`store`, `views`, `types`, `api`, `utils`, `router`)
- **执行规范**：严格遵循 `GEMINI.md`、`Rules.md`、`view-NEXT-rules.md`。

---

## 1. 变更背景与架构痛点分析

在早期的 `volur-lite-view-NEXT` 认证流中，URL 中的状态机参数（`dsh` 与流转锁 `TL`）由前端使用伪随机数模拟生成，后端并未建立起强一致的分布式状态机绑定机制。这带来了显著的安全隐患与架构不对齐：
1. **跃迁跳步（Step Skipping）漏洞**：若攻击者直接在浏览器构造目标 URL（例如直接从账号输入跳跃到密码或两步验证页），缺乏后端验签的流转锁将无法有效阻断非法跳步；
2. **重放攻击（Replay Attack）隐患**：前端自行签发的 TL 无法与后端会话生命周期严格绑定，已被破解的验证码挑战若未及时核销，可能被攻击者重复利用；
3. **分布式状态丢失（State Drift）**：用户访问登录页时，缺少集群唯一的 Session ID 引导与分布式缓存绑定，无法跨集群节点协同共享风控与审计上下文；
4. **认证视图职责割裂**：密码输入原本独立于 `AuthenticateView.vue`，未能对齐 Google Accounts 将密码降级为众多 Challenge 之一（`pwd`）的设计哲学。

---

## 2. 核心架构重构与改动点

### 2.1 后端：在 `volur-framework` 中构建 Google Accounts 状态机与流转锁体系
位于 `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/flow/`：

1. **分布式会话模型 [`SignInFlowSession`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/flow/SignInFlowSession.java)**：
   - 记录 `dsh` (如 `S-969965009:1790527266429740`)、`identifier`、`currentStep`、步骤序号 `cid`、当前合法激活的 `activeTL`、风控评级 `riskLevel`、已通关列表 `completedSteps` 以及客户端网络特征（IP、UserAgent）；
   - 在 Redis 中以 `vredis:security:flow:session:{dsh}` 统一维护，设置短期 TTL（15 分钟），保证集群节点强一致。
2. **流转锁与句柄管理器 [`TransitionLockManager`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/flow/TransitionLockManager.java)**：
   - **`generateDsh()`**：生成 Google 规范分布式句柄（9 位集群号 + 微秒时间戳与高熵随机数，格式 `S-<9位>:<16位>`）；
   - **`generateTL(...)`**：生成 Google 规范事务锁（前缀 `ADG-`，结合 HMAC-SHA256 签名与 60 位高熵字符串，绑定 `dsh|cid|step|identifier|timestamp`）；
   - **`validateTransitionTicket(...)`**：对请求携带的 TL 进行防伪、防跳步（Step Skipping）与防重放（Replay Attack）校验；
   - **`advanceTransitionTicket(...)`**：原子核销旧 TL，推进步骤序号 `cid`，签发下一阶段合法的 TL 并刷新缓存；
   - **`burnAndCompleteSession(...)`**：全部认证通过后，彻底焚毁临时会话与 TL，签发正式 JWT 令牌。

### 2.2 后端：在 `volur-auth` 中改造 Controller 与 Service 流程
位于 `src/volur-lite-platform/volur-auth/`：

1. **领域模型与视图对象**：
   - [`SignInIdentifierRequestDTO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignInIdentifierRequestDTO.java)：账号标识提交载荷（`identifier`、`dsh`、`cid: 0`、`deviceFingerprint`）；
   - [`SignInChallengeRequestDTO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignInChallengeRequestDTO.java)：安全挑战核验载荷（`dsh`、`tl`、`cid`、`challengeType: pwd/recaptcha/2fa/device`、密码或验证码）；
   - [`SignInInitVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignInInitVO.java)：初始化分配会话载荷（`dsh`、`flowName`、`flowEntry`、`cid: 0`）；
   - [`SignInFlowResponseVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignInFlowResponseVO.java)：统一状态机决策流转视图（返回更新后的 `tl`、`cid`、`nextStep` 或登录成功后的 `accessToken` / `refreshToken`）。
2. **Spring Security 白名单扩展**：
   - 在 [`SecurityWhitelistProperties`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java) 中将 `/api/v1/auth/session/**` 与 `/api/v1/auth/signin/**` 纳入公开免鉴权放行清单。
3. **服务层编排与风控驱动 [`AuthServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)**：
   - `initSignInSession`：首访若无 dsh 则由边缘/后端分配生成并在 Redis 建立初始会话；
   - `processIdentifier`：调用 `RiskEvaluationEngine` 评估综合风控。高风险环境派发 `recaptcha` 挑战（`cid=1`，初始 TL）；低风险环境直发 `pwd` 密码挑战（`cid=1`，初始 TL）；
   - `processChallenge`：
     - 若为人机验证（`recaptcha`），核验通过后削减风控评级，内部重算风控评分（Interstitial，内部推进至 Step: `cid=2`），更新通关签名签发新 TL 并推进至密码挑战 `pwd`（`cid=3`）；
     - 若为密码挑战（`pwd`），校验 TL 合法性与密码散列，若命中 2FA 规则则推进至 `2fa` / `totp`（`cid=4`），否则核销会话签发正式 JWT 令牌。
4. **控制层薄接口 [`AuthController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java)**：
   - 新增 `GET /api/v1/auth/session/init`；
   - 新增 `POST /api/v1/auth/signin/identifier`；
   - 新增 `POST /api/v1/auth/signin/challenge`。

### 2.3 前端：在新一代前端中收归挑战视图与后端状态机联动
位于 `src/volur-lite-view-NEXT/`：

1. **网络请求与强类型封装**：
   - 建立 [`src/utils/request.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/request.ts) 统一强类型 Axios 实例；
   - 建立 [`src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts) 封装后端 3 个状态机接口；
   - 建立 [`src/types/auth/flow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/flow.ts) 严格 1:1 对齐后端 DTO / VO。
2. **状态机 Store 重构 [`useAuthFlowStore`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/authFlow.ts)**：
   - 彻底废除客户端自制 Math.random 的 `generateDsh()` 与 `generateTL()`；
   - `ensureFlowParams`：进入页面若无 `dsh`，异步请求后端分配，并以 `router.replace` 写入 URL；
   - `submitIdentifier`：调用 `/auth/signin/identifier`，摄入后端签发的 `dsh`、`tl`、`cid` 与 `nextStep`；
   - `submitChallenge`：调用 `/auth/signin/challenge`，透传当前 `dsh`、`tl`、`cid`，当通关时自动持久化 JWT Token 并引导跳转。
3. **输入账号视图改造 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)**：
   - 移除前端硬编码的分支模拟，改为 `await authFlowStore.submitIdentifier(trimmed)`，根据后端返回的 `nextStep` 动态路由跳转至 `/v1/signin/challenge/${nextStep}`。
4. **安全挑战视图重构与密码降级收归 [`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue)**：
   - **把密码降级为众多 Challenge 中的一种（`pwd`）**：在 `ChallengeView.vue` 中无缝收敛密码表单（GoogleTextField、显示密码开关、忘记密码、操作按钮、账号标签芯片）；
   - 当路由为 `/v1/signin/challenge/pwd` 时，卡片大标题展示“欢迎”，按钮文案为“登录”；
   - 完整支持 `recaptcha`、`pwd`、`2fa`、`device` 等多态挑战，并在完成一步后按序携带更新后的 `TL` 与 `cid` 流转；
   - 当密码通过且无需 2FA 时，直接成功登录并引导重定向。
5. **原认证视图降级与路由重定向**：
   - [`AuthenticateView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AuthenticateView.vue)：作为过渡兼容垫片，挂载时自动重定向至 `/v1/signin/challenge/pwd` 并保留全部 Query 参数；
   - [`auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts)：路由 `/v1/signin/authenticate` 直接重定向至 `/v1/signin/challenge/pwd`。

---

## 3. 安全防护与完整流转流程验证

整体状态机流转与 `cid` 递增如下表所示：

| 阶段 | 路由路径 | 步骤序号 `cid` | 会话句柄 `dsh` | 流转锁 `TL` 状态 | 触发动作与后端决策 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **0. 首次访问** | `/v1/signin/identifier` | 0 | 后端新分配 (如 `S-969...`) | 无 | 前端 `ensureFlowParams` 探针请求后端初始化 |
| **1. 账号提交** | POST `/api/v1/auth/signin/identifier` | 0 | 保持同一 `dsh` | 签发 `TL_1` | 风险引擎评估：正常 -> `pwd` (cid=1)；高危 -> `recaptcha` (cid=1) |
| **2. 人机验证 (若高危)** | `/v1/signin/challenge/recaptcha` | 1 | 保持同一 `dsh` | 持有 `TL_1` | 用户破解验证码，提交核验 |
| **3. 风控重算 (Interstitial)**| POST `/api/v1/auth/signin/challenge` | 2 (内部) | 保持同一 `dsh` | 销毁 `TL_1`，签发 `TL_2` | 服务端完成 Interstitial 重算，签发通关签名 `TL_2`，推进至 `pwd` (cid=3) |
| **4. 密码挑战 (正常/通关后)**| `/v1/signin/challenge/pwd` | 1 或 3 | 保持同一 `dsh` | 持有 `TL_1` 或 `TL_2` | 校验密码散列，检查 2FA |
| **5. 二次验证 (若启用 2FA)** | `/v1/signin/challenge/2fa` | 4 或 5 | 保持同一 `dsh` | 销毁密码 TL，签发 `TL_3` | 输入 6 位动态验证码 |
| **6. 认证完成** | 业务目标页 (continue) | - | 彻底销毁 `dsh` 会话 | 彻底销毁全部 TL | 签发 JWT Access/Refresh Token，重定向至目标页 |

### 单元测试与构建验证：
1. **后端单元测试**：编写 [`TransitionLockManagerTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/security/TransitionLockManagerTest.java)，覆盖 dsh 格式、TL 格式、创建会话、越级跳步拦截（Step Skipping）以及重放攻击拦截（Replay Attack），5 项单元测试 100% 通过；
2. **后端全量编译**：`mvn test-compile -DskipTests` 在 `volur-lite-platform` 12 个模块中全部 `BUILD SUCCESS`；
3. **前端全量构建**：`npm run build` (`vue-tsc --noEmit && vite build`) 0 报错 0 告警，产物打包完成。

---

## 4. 深度重构与架构解耦（SigninController 独立抽离与状态机全域泛化）

根据进一步的架构规范与未来注册（SignUp）/找回密码（Recovery）等场景复用需求，进行了二次深度重构：

### 4.1 控制器与服务层单一职责隔离 (`SigninController` & `SigninService`)
1. **独立抽离 `SigninController`**：
   - 路径定位：`com.humolix.volur.auth.controller.v1.SigninController`；
   - 类级别统一注解：`@RequestMapping("/api/v1/auth/signin")`；
   - 统一收拢三个核心端点：
     - `GET /api/v1/auth/signin/init`（兼容别名 `/session/init`）：用于首次访问或缺少句柄时初始化会话；
     - `POST /api/v1/auth/signin/identifier`：用于提交账号标识并触发风控判定；
     - `POST /api/v1/auth/signin/challenge`：用于提交各类安全挑战核验。
2. **独立抽离 `SigninService` / `SigninServiceImpl`**：
   - 将原平铺在 `AuthServiceImpl` 中的登录状态机编排逻辑（会话初始化、风控综合评估、人机挑战核销、密码散列核验、2FA 递增等）完整下沉至 `SigninServiceImpl`；
   - 彻底将 `transitionLockManager` 与 `riskEvaluationEngine` 从 `AuthServiceImpl` 中解耦移除，`AuthService` 重新聚焦传统账号注册、修改密码与注销事务，严格遵循 `Rules.md` Thin Controller 与高内聚低耦合原则；
3. **前端 API 请求路径对齐**：
   - 在 [`src/volur-lite-view-NEXT/src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts) 中将初始化接口路径调整为 `/auth/signin/init`，与后端 Controller 的 `@RequestMapping("/api/v1/auth/signin")` 实现 100% 路径对称。

### 4.2 状态机通用泛化扩展 (超越 Signin，全面覆盖 SignUp / Recovery)
1. **定义通用阶段化会话模型 [`AuthFlowSession`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/flow/AuthFlowSession.java)**：
   - 不再局限于登录流，显式声明 `flowName`（如 `VolurWebSignIn`、`VolurWebSignUp`、`AccountRecovery`）、`flowEntry`（如 `ServiceLogin`、`SignUp`、`PasswordReset`）；
   - 新增 `metadata: Map<String, Object>` 上下文存储区，可用于 SignUp 阶段暂存多步注册草稿（如用户名、邮箱、基本资料）而在最终挑战通关后原子入库；
   - 历史兼容类 `SignInFlowSession` 继承自 `AuthFlowSession`。
2. **`TransitionLockManager` 泛化支持**：
   - 提供 `createOrGetSession(dsh, flowName, flowEntry, ip, ua)` 通用构造方法；
   - 新增 `setMetadata(dsh, key, value)` 与 `getMetadata(dsh, key)`，实现多阶段业务数据的分布式暂存；
   - 通用流转锁 `ADG-` 生成与校验逻辑可无缝复用于 SignUp 验证码核销、新用户密码设置及安全问答。
3. **安全白名单放行扩展**：
   - 在 [`SecurityWhitelistProperties`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java) 中同步预先将 `/api/v1/auth/signup/**` 纳入白名单放行清单，确保注册阶段化流转无阻。

### 4.3 后端冗余参数清理 (`HttpServletRequest` 解耦)
- **背景与原因**：`SigninServiceImpl` 与 `SigninController` 中原先声明的 `HttpServletRequest` 形参并没有被实际使用；系统内部的客户端特征获取工具 `RequestUtil.getClientIp()` 与 `RequestUtil.getUserAgent()` 已经通过 Spring 框架底层的 `RequestContextHolder.getRequestAttributes()` 从线程局部上下文（ThreadLocal / 虚拟线程上下文）安全获取当前请求对象。
- **改动动作**：彻底移除了 `SigninService`、`SigninServiceImpl` 与 `SigninController` 中 `initSession`、`identifier` 与 `challenge` 上的 `HttpServletRequest` 冗余形参，净化接口签名。

### 4.4 前端与状态机流转入口状态机制对齐 (`flowEntry: ServiceLogin <-> SignUp`)
- **对齐 Google Accounts 行为哲学**：
  - `flowName`: 代表统一认证状态机引擎（Google Accounts 为 `GlifWebSignIn`，Volur 为 `VolurWebSignIn`）；
  - `flowEntry`: 代表当前用户进入流程的原始业务意图与入口模式：
    - `ServiceLogin`：服务登录；
    - `SignUp`：账号注册 / 创建账号；
    - `AddSession`：多账号并发追加会话。
- **改动动作**：
  1. 在 [`useAuthFlowStore`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/authFlow.ts) 中增加 `setFlowEntry`，并使 `ensureFlowParams` 自动感知路由路径：若处于 `/signup` 路由则默认注入 `flowEntry=SignUp`，若处于 `/signin` 路由则注入 `flowEntry=ServiceLogin`；
  2. 在 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 点击“创建账号”跳转时，透传更新后的 `flowEntry: 'SignUp'`，同时保留同一分布式句柄 `dsh` 与 `continue` 目标地址；
  3. 在 [`SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue) 点击“改用现有账号登录”跳转时，透传更新后的 `flowEntry: 'ServiceLogin'`；
  4. 后端 `initSession` 接收可选的 `flowEntry` 参数，并在会话复用或新建时原子更新 Redis 中会话的 `flowEntry` 属性。

---

## 5. VLIF (Volur Login Interface Framework) 与独立 FlowController 架构升级

为了进一步对齐 Google Accounts 底层的 GLIF (Google Login Interface Framework) 体系，并将 `dsh` 与 `TL` 状态机彻底泛化为平台通用的工作流中枢，实施了以下系统级重构：

### 5.1 架构设计思想：统一工作流控制器 (`FlowController`) 与服务抽象
1. **职责分离与跨业务面扩展**：
   - 之前在 `SigninController` 中保留 `initSession` 存在职责边界模糊的问题，无法优雅服务未来的注册（SignUp）、多因素恢复（Recovery）、设备授权等多种非登录工作流；
   - 建立全新的全局工作流控制中心 [`FlowController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/FlowController.java)，类级别注解 `@RequestMapping("/api/v1/flow")`；
   - 核心端点：`GET /api/v1/flow/initSess?flowEntry=WebServiceLogin&flowName=VlifWebLogin&fp=<MD5指纹>&t=<时间戳>&dsh=<可选已存在会话>`；
   - 核心领域模型 [`FlowInitVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/FlowInitVO.java)：携带签发的 `dsh`、`flowName`、`flowEntry`、步骤序号 `cid: 0`、浏览器指纹校验码 `fp` 以及服务端绝对时间戳 `serverTime`。
2. **工作流层级规范对齐 (VLIF)**：
   - 全面升级命名为 **VLIF (Volur Login Interface Framework)**，对标 Google GLIF；
   - **`flowName`**：界定顶层大模块业务边界：
     - `VlifWebLogin`：登录工作流（对标 `GlifWebSignIn`）；
     - `VlifWebSignUp`：注册工作流；
     - `VlifWebRecovery`：找回凭据工作流；
   - **`flowEntry`**：界定大模块下的具体功能入口与场景细分：
     - `WebServiceLogin`：Web 服务入口登录；
     - `WebSignUp`：注册入口；
     - `WebPasswordReset`：重置密码入口。

### 5.2 浏览器指纹采集与时间戳防篡改绑定 (`fp` & `t`)
1. **前端 RFC 1321 MD5 纯 TypeScript 指纹生成器**：
   - 编写 [`src/utils/fingerprint.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/fingerprint.ts)；
   - 深度采集客户端固有特征：屏幕色彩深度与分辨率、时区偏移量、硬件并发线程数 (`hardwareConcurrency`)、设备内存 (`deviceMemory`)、操作系统平台 (`navigator.platform`)、浏览器首选语言以及 Canvas 2D 熵特征；
   - 通过高效的 RFC 1321 MD5 算法将采集的硬件环境特征转化为 32 位 Hex 哈希摘要作为设备指纹 `fp`。
2. **后端会话与风控上下文绑定**：
   - 在 [`FlowServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/FlowServiceImpl.java) 中，将客户端传参的 `fp` 与客户端时间戳 `t` 记录进 `AuthFlowSession` 的 `metadata` 中，便于后续风控引擎 `RiskEvaluationEngine` 跨步骤对比设备特征漂移，防范 Session Hijacking（会话劫持）。

### 5.3 跨模块分支动态平滑切换与句柄复用 (`Session Continuation`)
1. **复用同一分布式句柄 `dsh` 切换工作流**：
   - 当用户从登录页（`flowName=VlifWebLogin&flowEntry=WebServiceLogin`）点击“创建账号”进入注册页时，无需重新分配全新 `dsh`；
   - 前端将已持有的 `dsh` 带入 `GET /api/v1/flow/initSess?flowName=VlifWebSignUp&flowEntry=WebSignUp&dsh=S-...`；
   - 后端 [`TransitionLockManager`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/flow/TransitionLockManager.java) 会感知该 `dsh` 存在，并在锁保护下原子更新该会话的 `flowName` 与 `flowEntry`，重置步骤序号 `cid=0`，实现同一用户在登录/注册/恢复之间状态一致性漫游。
2. **前端路由与别名对齐**：
   - 在 [`src/router/routes/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts) 中配置别名 `alias: 'auth/signin'`，使得 `/v1/auth/signin/identifier` 与 `/v1/signin/identifier` 均可平滑访问；
   - 全局 Store [`useAuthFlowStore`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/authFlow.ts) 统一管理 `flowName`、`flowEntry`、`fp`、`serverTime` 与 `dsh`。

### 5.4 验证结果
1. **自动化单元测试**：
   - 编写并执行 [`FlowControllerTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/FlowControllerTest.java)，验证 `GET /api/v1/flow/initSess` 接口正确返回 `FlowInitVO` 且 HTTP 状态码为 200；
   - 执行 `volur-auth` 模块及其依赖模块的全量单元测试（33 项单元测试），全部通过（Failures: 0, Errors: 0）。
2. **前后端编译验证**：
   - 后端全工程 Maven 编译全部 `BUILD SUCCESS`；
   - 前端 Vite 打包 `npm run build` 全部成功无任何报错。



