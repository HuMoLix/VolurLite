# 多步骤注册流程（Signup Steps）状态机与界面重构设计方案

## 1. 变更背景与需求概述

系统原有的注册视图 `SignUpView.vue` 采用了单页平铺的表单设计，未能与系统整体的 Google Accounts 式阶段化认证架构（`TransitionLock` 状态机、会话句柄 `dsh`、一次性防重放票据 `TL`、步骤序号 `cid`）以及全生命周期风控深度融合。

为了提升用户注册体验、强化账号凭据安全，并实现根据系统配置动态跳步与后续安全能力扩展，本次针对前后端进行结构性重构：
- **前端工程 (`src/volur-lite-view-NEXT`)**：
  - 参考 `ChallengeView.vue` 将 `SignUpView.vue` 重构为步骤容器视图；
  - 路由改建为 `/v1/signup/steps/:step(.*)*`；
  - 拆分 5 个独立的步骤组件：`NameStepView`、`EmailStepView`、`EtotpChallengeStepView`、`PasswordStepView`、`OptionsChallengeStepView`；
  - 维持完整的 URL Query 参数全链路透传（`continue`, `followup`, `flowName=VlifWebSignUp`, `flowEntry=WebSignUp`, `dsh`, `TL`, `pstMsg`, `cid`）。
- **后端工程 (`volur-auth`)**：
  - 严格采用 `@RequestMapping("/api/v1/auth/signup")` 路径前缀；
  - 实现控制器 `SignUpController`，服务层 `SignUpService` / `SignUpServiceImpl`，以及配套 DTO / VO 模型；
  - 基于 `TransitionLockManager` 实现对 `name`、`email`、`challenge/etotp`、`password`、`challenge/options` 的严格顺序推进与防跳步核验；
  - 根据系统配置 `system.auth.require_email_verification` 动态决策是否执行邮箱验证流程。

---

## 2. 路由与流转状态机映射

### 2.1 步骤路由规范

| 步骤序号 | 路径设计 | 核心职责 | 前序依赖 / 流转决策 |
| :--- | :--- | :--- | :--- |
| **Step 1** | `/v1/signup/steps/name` | 输入并核验用户名 | 初始化会话 (`/flow/initSess`)；检测系统是否要求邮箱验证，动态决策下一步为 `email` 或 `password` |
| **Step 2** | `/v1/signup/steps/email` | 输入邮箱并发送 6 位验证码 | 依赖 `name` 完成且系统要求邮箱核验；异步调用 `emailService.sendVerificationEmail` |
| **Step 3** | `/v1/signup/steps/challenge/etotp` | 校验邮箱验证码 | 校验 6 位数字验证码（原子核销）；支持 60s 冷却重发 |
| **Step 4** | `/v1/signup/steps/password` | 输入与确认密码并落库创建账号 | 校验密码一致性与强度；落库 `User` 与 `UserProfile`，发布注册事件；生成预登录 JWT Token |
| **Step 5** | `/v1/signup/steps/challenge/options` | 提示配置 PassKey、2FA 等安全选项 | 展示安全扩展能力卡片；用户点击跳过或完成后核销会话并跳转业务首页 |

### 2.2 状态机推进与防跳步设计

每次调用后端接口，客户端均须上报当前 `dsh`, `tl`, `cid`：
1. 后端调用 `transitionLockManager.validateTransitionTicket(dsh, tl, cid, expectedStep)`：
   - 强校验 `tl` 与 Redis 会话中的 `activeTL` 严格匹配；
   - 强校验 `expectedStep` 与 `session.getCurrentStep()` 一致；
   - 强校验 `cid` 与 `session.getCid()` 一致。
2. 校验通过后，调用 `transitionLockManager.advanceTransitionTicket(...)`：
   - 记录已完成步骤；
   - 生成全新单次有效的 `TL`；
   - 更新当前步骤与 `cid = cid + 1`；
   - 返回 `SignUpFlowResponseVO`，指导前端切换视图。

---

## 3. 核心改动明细

### 3.1 后端工程 (`volur-auth`)
1. **安全白名单更新**：
   - [`SecurityWhitelistProperties.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java)：将 `"/api/v1/auth/signup/**"` 加入白名单列表，确保未登录访客畅通访问各注册步骤。
2. **领域模型 (DTO / VO)**：
   - [`SignUpNameRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignUpNameRequestDTO.java)
   - [`SignUpEmailRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignUpEmailRequestDTO.java)
   - [`SignUpEmailResendRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignUpEmailResendRequestDTO.java)
   - [`SignUpEtotpChallengeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignUpEtotpChallengeRequestDTO.java)
   - [`SignUpPasswordRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignUpPasswordRequestDTO.java)
   - [`SignUpCompleteRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/SignUpCompleteRequestDTO.java)
   - [`SignUpFlowResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignUpFlowResponseVO.java)
   - [`SignUpOptionVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignUpOptionVO.java)
3. **服务层**：
   - [`SignupService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/SignupService.java)
   - [`SignupServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java)
4. **控制器层**：
   - [`SignupController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java)：遵循 Thin Controller 模式，统一定义在 `/api/v1/auth/signup`。

### 3.2 前端工程 (`volur-lite-view-NEXT`)
1. **路由与类型定义**：
   - [`src/types/auth/flow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/flow.ts)：增加 SignUp 强类型请求与响应结构；
   - [`src/router/routes/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts)：重构 `signup` 路由为 `steps/:step(.*)*`，默认重定向至 `steps/name`；
2. **状态管理物理隔离 (`src/store/modules/flow/`)**：
   - 彻底废除并删除扁平巨石 Store `authFlow.ts`，按照单一职责与业务生命周期进行解耦与收敛；
   - [`src/store/modules/flow/signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts)：专门维护登录认证流程状态（`identifier`、密码挑战、设备二次验证、人机验证、多因素选择、重试限制等）；
   - [`src/store/modules/flow/signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts)：专门维护多步骤注册流程状态（`username`、`email`、邮箱动态口令校验、密码设定、安全扩展配置选项等）；
   - [`src/store/modules/flow/flowLoading.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/flowLoading.ts)：抽离提炼公共加载态与触感遮罩延迟逻辑（`loading`, `setLoading`, `startLoading`, `stopLoading`, `withLoading`, `triggerScrim`, `MIN_LOADING_DURATION_MS`），消除了 `signinFlow.ts` 与 `signupFlow.ts` 中的完全重复代码；
   - [`src/store/modules/flow/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/index.ts)：统一向外导出 `useSigninFlowStore`、`useSignupFlowStore`、`createFlowLoading` 以及常量 `MIN_LOADING_DURATION_MS`；
   - 全面重构所有登录与注册视图组件的 Store 依赖与方法调用，严禁遗留任何历史废弃别名与兼容垫片；
   - [`src/components/layouts/auth/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)：自动根据当前路由路径识别并适配当前激活的 Flow Store（`signupFlowStore` 或 `signinFlowStore`），实现顶层遮罩与加载态透明联动；
3. **API 模块**：
   - [`src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts)：封装 `submitSignUpNameApi`、`submitSignUpEmailApi`、`resendSignUpEmailApi`、`submitSignUpEtotpApi`、`submitSignUpPasswordApi`、`fetchSignUpOptionsApi`、`completeSignUpApi`；
4. **认证卡片标题与副标题去中心化解耦 (`src/composables/useCardHeader.ts`)**：
   - 彻底废除并在主视图（`SignUpView.vue`、`ChallengeView.vue`）中移除了按当前步骤分支计算的大块集中式 `computed(() => cardTitle / cardSubtitle)`；
   - 封装 [`src/composables/useCardHeader.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/composables/useCardHeader.ts)，通过 `provideCardHeader` / `useCardHeader` 机制实现标题与副标题的去中心化声明；
   - 各具体的步骤与挑战视图（如 `EmailStepView.vue`、`PwdChallengeView.vue` 等）在其内部直接调用 `useCardHeader(title, subtitle)`，支持静态字符串、响应式 Ref 或 Getter 自动同步，子组件对自己的标题与副标题全权负责；
5. **视图层与操作按钮布局**：
   - 全面对齐 [`src/views/auth/signin/IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 的按钮排版布局规范；
   - 所有注册子步骤视图底部的辅助操作按钮（如“改用现有账号登录”、“返回上一步”、“暂不设置，直接跳过”等）与主提交按钮（“下一步”、“完成注册”、“前往主页”）统一右对齐紧凑并排排列（`class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8"`），彻底杜绝左右两端散布的 `justify-between` 排版；
   - 为 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 中的“创建账号”跳转按钮与 [`NameStepView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/steps/NameStepView.vue) 中的“改用现有账号登录”按钮全面接入 `triggerScrim` 触感加载逻辑与 `:disabled="loading"` 交互防护，保持跨流程跳转时触感动画、顶部进度条与遮罩层效果的对称与平滑；
   - [`src/views/auth/signup/SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue)：主容器视图，动态标题、副标题与 Account Chip 头像胶囊；
   - `src/views/auth/signup/steps/NameStepView.vue`
   - `src/views/auth/signup/steps/EmailStepView.vue`
   - `src/views/auth/signup/steps/EtotpChallengeStepView.vue`
   - `src/views/auth/signup/steps/PasswordStepView.vue`
   - `src/views/auth/signup/steps/OptionsChallengeStepView.vue`
   - `src/views/auth/signup/steps/index.ts`
   - 各登录挑战子视图（`PwdChallengeView`、`RecaptchaChallengeView`、`DeviceChallengeView`、`TotpChallengeView`、`SelectionView`、`RejectView`）

---

## 4. 规范遵循与审计验证

1. **规范合规**：
   - 彻底贯彻零注释铁律，所有 Java、Vue、TS 代码文件无冗余行内备注或解释性注释；
   - 贯彻重构彻底性原则，无遗留废弃别名；
   - 严格遵循 Google Material Design 3 色彩规范，亮暗色反差明确，严禁正文刺眼全黑 `#000000`；
   - 保持 Tailwind CSS 与 Naive UI 无冲突隔离。
2. **构建与测试验证**：
   - 后端 Maven 多模块测试与编译：`BUILD SUCCESS`；
   - 前端 TypeScript 与 Vite 打包：`vue-tsc --noEmit && vite build` 0 错误，打包顺利完成。
