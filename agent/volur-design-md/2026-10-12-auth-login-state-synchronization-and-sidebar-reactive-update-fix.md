# 登录完成后侧边栏状态即时同步与无需刷新响应设计文档

## 1. 变更背景与缺陷根因分析

### 1.1 问题现象
用户在完成登录挑战流程并通过前端路由跳转至 `/competitions`（赛事大厅）时，侧边栏（`VSideBar.vue`）底栏 Dock 仍停留在“登录”访客状态（无头像、提示登录）。只有在用户手动刷新（F5）整个浏览器页面之后，侧边栏才会更新为已登录用户的用户名、邮箱和头像。

### 1.2 深度原因排查
1. **`document.cookie` 的非响应式陷阱**：
   - 在 `src/store/modules/user.ts` 中，`isLoggedIn` 原先定义为计算属性：
     `const isLoggedIn = computed(() => getCookie('logged_in') === 'true' || !!user.value)`；
   - 在 Vue 3 响应式原理中，`computed` 仅对其内部访问的响应式对象（如 `ref`、`reactive`）建立依赖追踪。`document.cookie` 属于原生浏览器属性，不具备 Vue Proxy 的 Setter 劫持机制；
   - 当后端在完成登录认证并返回 `Set-Cookie: logged_in=true` 后，由于 `user.value` 依然为 `null`，且没有任何响应式 `ref` 发生变动，导致 `isLoggedIn` 的计算缓存未被标记为脏（Dirty），始终保持 `false`；
2. **`setAuthenticatedUser` 未同步写入 `user.value`**：
   - 登录成功分支中调用的 `userStore.setAuthenticatedUser(...)` 原先仅将用户信息写入了本地历史记录 `knownAccounts`，完全未对当前 Pinia 核心状态 `user.value` 进行赋值；
   - 导致登录成功后，响应式用户实体 `user.value` 依然为 `null`；
3. **`fetchCurrentUser` 存在循环阻断与未异步等待（Unawaited Promise）**：
   - 在 `src/store/modules/user.ts` 的 `fetchCurrentUser` 函数中，前置校验为：`if (!isLoggedIn.value) return null`；
   - 由于上述 `isLoggedIn.value` 因非响应式判定保持 `false`，导致 `fetchCurrentUser(true)` 被直接短路拦截，根本未能向后端发起 `/api/v1/users/me` 请求；
   - 同时，在 `signinFlow.ts`、`signupFlow.ts` 及各登录/保护视图中，`userStore.fetchCurrentUser(true).catch(() => {})` 均未被 `await`，导致在用户信息请求尚未完成时就已经触发了 `router.push(targetRedirect)`，产生了路由导航与状态更新的时序竞态；
4. **刷新页面可恢复的原因**：
   - 当用户手动刷新页面时，整个 SPA 页面重新加载并重新执行 Pinia 初始化；
   - 初始化时 `getCookie('logged_in')` 首次读取到了浏览器中的 cookie，`VSideBar.vue` 的 `onMounted` 钩子探测到登录态并请求 `/users/me`，这才完成了用户信息的晚绑定。

---

## 2. 解决方案与重构明细

### 2.1 响应式 Cookie 状态驱动 (`src/store/modules/user.ts`)
- 引入响应式 `isCookieLoggedIn = ref<boolean>(typeof document !== 'undefined' && getCookie('logged_in') === 'true')`；
- 将 `isLoggedIn` 调整为 `computed(() => isCookieLoggedIn.value || !!user.value)`，使登录态变更有确定的响应式依赖触发源；
- 在用户登出时，显式将 `isCookieLoggedIn.value` 置为 `false`；在用户登录成功时，显式将其置为 `true` 并同步调用 `setCookie('logged_in', 'true')`。

### 2.2 用户实体同步注入与持久化 (`src/store/modules/user.ts`)
- 重构 `setAuthenticatedUser` 方法，在持久化已知账号的同时，同步构建并更新 `user.value` 基础实体，并触发 `isCookieLoggedIn.value = true`，使 `userStore.isLoggedIn`、`userStore.displayName`、`userStore.avatarUrl` 能够在调用发生的毫秒级即刻完成响应式广播更新。

### 2.3 强制拉取与错误自愈机制 (`src/store/modules/user.ts`)
- 在 `fetchCurrentUser(force = false)` 中，支持通过 `force = true` 绕过前置校验直接向后端同步 `/api/v1/users/me` 最新视图；
- 当后端返回 401 Unauthorized 状态时，执行 `clearUser()` 快速清理并同步重置 cookie 与响应式状态。

### 2.4 全认证流异步对齐 (`signinFlow.ts`, `signupFlow.ts`, `AccountChooserView.vue` 等)
- 在所有成功通过验证的流程分支中，移除 `rawIdentifier` 的非必要阻断，确保在 `status === 'SUCCESS'` 时立即调用 `setAuthenticatedUser`；
- 在触发路由跳转前统一执行 `await userStore.fetchCurrentUser(true).catch(() => {})`，彻底消除由于时序竞态导致的目标页面在状态未就绪时挂载的问题。

---

## 3. 影响范围与验证结果

1. **影响文件范围**：
   - `src/store/modules/user.ts`
   - `src/store/modules/flow/signinFlow.ts`
   - `src/store/modules/flow/signupFlow.ts`
   - `src/views/auth/signin/AccountChooserView.vue`
   - `src/views/auth/signup/steps/OptionsChallengeStepView.vue`
   - `src/views/auth/signup/protect/passkeyProtectView.vue`
   - `src/views/auth/signup/protect/totpProtectView.vue`
2. **构建与类型验证**：
   - 执行 `npm run build`，编译打包耗时 15.28s，无任何 TypeScript 报错与语法告警；
3. **表现与一致性保证**：
   - 用户在输入密码/完成核验并提交后，无需刷新页面，跳转至 `/competitions` 的瞬间，侧边栏即刻渲染对应的用户昵称、邮箱与头像；
   - 未登录状态下依旧严格展示未登录访客占位，点击可直接跳转至 `/v1/signin/identifier`。
