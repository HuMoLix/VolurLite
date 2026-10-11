# 全链路用户登录/注册成功状态持久化与侧边栏响应式同步设计与实现方案

## 1. 变更背景与问题溯源

在用户使用特定账号（例如 `577896795@qq.com`）成功登录系统后，主页面侧边栏 [`VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 底部的用户信息栏与展开的个人信息卡片（User Card Popover）中，用户名及邮箱信息未显示真实账号数据，而是退化为缺省值 `"Volur User"`。

经过对认证流、状态管理及视图组件的全链路深度排查，确认根本原因如下：
1. **认证流持久化缺失**：
   - 在 [`signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts) 中，用户完成密码或动态凭证核验（`submitChallenge`）后：
     - 若核验直接返回 `SUCCESS`，流程未调用任何持久化方法即完成跳转；
     - 若核验驱动进入下一阶段 `authenticate` 并自动完成终态认证，其返回的认证实体并未持久化写入本地已知账号列表（Known Accounts）或用户状态中心。
   - 在注册流 [`signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts) 以及注册可选步骤（[`OptionsChallengeStepView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/steps/OptionsChallengeStepView.vue)、[`totpProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue)、[`passkeyProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/passkeyProtectView.vue)）完成或跳过时，仅完成流程重置并路由跳转，未对新注册账号进行持久化登记。
2. **状态响应式断层与多头存储分散**：
   - [`VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 之前直接在计算属性中调用 `getKnownAccounts()`。由于 `localStorage` 并不具备 Vue 的响应式追踪能力，在页面不刷新的情况下即使数据写入也无法触发计算属性重算。
   - 原项目存在单独的 `src/utils/accountChooser.ts`，多处组件各自独立读写本地存储，违背了 Pinia 单一真实源（Single Source of Truth）原则。
3. **缺少全局 UserStore 与服务端实时回填**：
   - `volur-lite-view-NEXT` 缺少独立的 Pinia `userStore` 模块与对应的 API 请求客户端，在用户具有登录态 Cookie（`logged_in=true`）时无法向后端 [`UserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java) 发起 `GET /api/v1/users/me` 实时获取最新的用户信息（用户名、头像、邮箱等），导致本地缓存为空时彻底陷入无数据状态。

---

## 2. 核心架构设计与改造方案

基于 Vue 3 + Pinia Setup Store + TypeScript 现代全栈标准与项目规范（`GEMINI.md`、`view-NEXT-rules.md`），构建了**所有持久化数据统一归口 Pinia、单向数据流与自闭环服务端回填**的体系：

```
[ 认证流 / 注册流 / 账号选择器 ]
              │
              ▼ (saveAccount / setAuthenticatedUser)
┌─────────────────────────────────────────────────────────────┐
│                 Pinia Store: useUserStore                   │
│                                                             │
│  State:                                                     │
│    user: Ref<UserSummaryVO | null>                          │
│    knownAccounts: Ref<KnownAccount[]>                       │
│                                                             │
│  Auto Persistence (deep watch):                             │
│    watch(user) ───────────> localStorage(volur_user_summary)│
│    watch(knownAccounts) ──> localStorage(volur_known_accounts)
│                                                             │
│  Getters:                                                   │
│    isLoggedIn / currentUser / displayName                   │
│    userEmail / avatarUrl / userInitial / hasKnownAccounts   │
└─────────────────────────────────────────────────────────────┘
       ▲                                     │
       │ (GET /api/v1/users/me)              ▼ (computed 响应式流转)
[ 后端 UserController ]             [ VSideBar 侧边栏 & Card Popover ]
```

---

## 3. 改动点清单与详细说明

### 3.1 新增用户领域类型定义 [`src/types/user/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/user/index.ts)
- 1:1 严格对齐后端领域模型：
  - `UserSummaryVO`：对应后端 `com.humolix.volur.user.domain.vo.UserSummaryVO`（字段包含 `id`、`username`、`email`、`avatarUrl`、`role`、`org`、`bio`、`createdAt`）。
  - `UserProfileResponseVO`：对应后端 `com.humolix.volur.user.domain.vo.UserProfileResponseVO`。
  - `UpdateUserProfileRequestDTO`、`UpdateAvatarRequestDTO`：对应后端请求实体。

### 3.2 新增用户 API 模块 [`src/api/user.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/user.ts)
- 封装强类型 RESTful 接口：
  - `getCurrentUserApi()`：`GET /api/v1/users/me`（静默请求，`skipToast: true`）。
  - `getCurrentUserProfileApi()`：`GET /api/v1/users/profile`。
  - `getUserProfileByIdApi(userId)`：`GET /api/v1/users/{userId}/profile`。
  - `updateUserProfileApi(data)`：`PUT /api/v1/users/profile`。
  - `updateAvatarApi(data)`：`PUT /api/v1/users/avatar`。

### 3.3 全面重构 Pinia 用户状态管理 [`src/store/modules/user.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/user.ts)
- **自包含持久化初始化**：
  - `user`：初始化时由 `loadStoredUser()` 自动从 `localStorage('volur_user_summary')` 进行反序列化水合；
  - `knownAccounts`：初始化时由 `loadStoredKnownAccounts()` 自动从 `localStorage('volur_known_accounts')` 水合；
- **响应式持久化监听（Auto-Persistence）**：
  - `watch(user, ..., { deep: true })`：监听 `user` 响应式引用，状态变动时立即自动同步持久化或清理；
  - `watch(knownAccounts, ..., { deep: true })`：监听 `knownAccounts` 列表，实时自动同步持久化；
- **全量计算属性**：
  - `isLoggedIn`：基于 Cookie `logged_in` 或 `user` 实体双重保障；
  - `currentUser`：优先提取 `user` 概要，缺省 fallback 至 `knownAccounts[0]`；
  - `displayName`：统一计算用户名或昵称，默认安全回退；
  - `userEmail`、`avatarUrl`、`userInitial`、`hasKnownAccounts`；
- **状态动作**：
  - `saveAccount(account)`：统一账号去重与前置推入，触发响应式持久化；
  - `removeAccount(identifier)`：统一从列表中移除指定账号并更新持久化；
  - `clearAccounts()`：清空已知账号列表；
  - `setAuthenticatedUser(accountData)`：将认证成功的账号写入已知列表；
  - `fetchCurrentUser(force)`：调用 `getCurrentUserApi()`，支持在途 Promise 去重防抖（Deduping），拉取成功后更新 `user` 并通过 `saveAccount` 双向同步；
  - `clearUser()`：清理当前用户登录实体及本地持久化。

### 3.4 彻底废除并删除离散持久化工具 [`src/utils/accountChooser.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/accountChooser.ts)
- 遵循单一数据源原则，彻底删除原独立工具文件 `src/utils/accountChooser.ts`；
- 所有原来直接依赖该工具的业务组件全部平滑升级为直连 `useUserStore()`：
  - [`AccountChooserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue)：直接使用 `userStore.knownAccounts`、`userStore.saveAccount`、`userStore.removeAccount`；
  - [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)：直接使用 `userStore.hasKnownAccounts` 控制账号选择入口显隐。

### 3.5 登录与注册流程对接
- [`signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts)：
  - 在 `submitChallenge`（动态核验成功、自动认证成功）与 `submitAuthenticate` 中全链路调用 `userStore.setAuthenticatedUser(...)` 并并发调用 `userStore.fetchCurrentUser(true)`。
- [`signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts) 及步骤视图：
  - 在密码提交成功及流程完成阶段统一调用 `userStore.setAuthenticatedUser(...)`。

### 3.6 侧边栏全面绑定 Pinia userStore [`src/components/sidebar/VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue)
- 模板内所有个人信息显示彻底解耦对内联判断和本地静态对象的依赖：
  - 头像与占位符直连 `avatarUrl` 与 `userInitial`（由 `userStore` 提供）；
  - 主副标题文字直连 `displayName` 与 `userEmail`（由 `userStore` 提供）；
  - 卡片弹窗直连 `userStore` 计算属性；
- 生命周期钩子与监听器：
  - `onMounted` 钩子中登录态检测并静默拉取 `userStore.fetchCurrentUser()`；
  - `watch(() => userStore.isLoggedIn)` 监听器保障实时更新；
  - 登出动作调用 `userStore.clearUser()`。

---

## 4. 规范遵守与代码质量审查

1. **纯粹代码与零注释原则**：所有修改及新增的代码文件（`*.ts`、`*.vue`）均严格保持零行内解释性注释、零冗余备注，保证代码自解释与纯净；所有技术方案与设计推导全量集中于本文档。
2. **单一真实源（Single Source of Truth）**：彻底移除了独立于 Pinia 之外的 `accountChooser.ts`，所有本地持久化与数据生命周期均由 Pinia Store 集中接管。
3. **构建验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），14,834 个模块全部编译通过，TypeScript 类型检查零错误，产物正常打包。
