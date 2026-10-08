# 全流程认证会话生命周期对齐与废弃凭据重置统一加固

## 1. 变更背景

在对 `src/volur-lite-view-NEXT` 认证体系进行全功能链路横向排查时，发现除“找回用户名”流程外，登录终局认证、忘记密码跳转、挑战被拒返回、账号选择器以及注册状态机等多处关键流程中，均存在服务端物理销毁 Session（`burnAndCompleteSession`）与前端 Pinia 单例 Store / 路由 Query 未同步重置凭据的问题。这一缺陷导致用户在二次登录、流程跨转或失败重试时，极易复用已经失效的死 DSH 或错配的流转票据（TL），进而被后端的 401/403 异常拦截器强行打入 `sessionExpireView.vue`（会话超时）页面。

## 2. 根因分析与风险排查

1. **登录终局认证成功后凭据残留**：
   在 `signinFlow.ts` 中，无论是通过 `submitChallenge` 还是显式调用 `submitAuthenticate` 完成登录，服务端均会在成功签发 Cookie 的同时执行 `transitionLockManager.burnAndCompleteSession(...)` 销毁会话。但前端判定 `status === 'SUCCESS'` 后未调用 `resetFlow()`，导致废弃的 `dsh`、`tl` 依然驻留在 Store 中。当用户登出或在同一页面再次登录时，`ensureFlowParams` 会将死 DSH 回填至 URL，导致下一次任何操作必然 401 报错并跳到超时页面。
2. **密码挑战页“忘记密码”路径与 TL 错配**：
   在 `pwdChallengeView.vue` 中，用户点击“忘记密码”时原代码直接 `router.push('/v1/recovery/challenge')`，未传入正确的选择路径，且 Store 中残留了密码步骤签发的专属 TL。进入安全核验后，服务端因 TL 的 `expectedStep` 不匹配抛出 403 跃迁跳步（Step Skipping）异常，被前端 Axios 拦截器捕获后强制重定向至会话超时页。
3. **挑战被拒视图携带脏票据回流**：
   在 `rejectView.vue` 中，用户点击“返回”登录标识输入页时未清空 `tl` 与 `cid`，将失败步骤的残余票据带回初始登录流程。
4. **账号选择器与注册状态机的参数冗余**：
   在 `AccountChooserView.vue` 中，激活已处于 `ACTIVE` 状态的账号未清理未完成登录流，且选择其他账号时未重置标识；在 `signupFlow.ts` 中，`getQueryParams` 无条件向 URL 输出 `dsh: ""` 与 `cid: "0"`，破坏了未初始化会话分支的判定。

## 3. 改动点

1. **`src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts`**：
   - 在 `submitChallenge` 检测到 `status === 'SUCCESS'` 以及 `nextStep === 'authenticate'` 成功时，保存目标重定向后立即执行 `resetFlow()`；
   - 在 `submitAuthenticate` 检测到 `status === 'SUCCESS'` 且完成已知账号保存后，保存目标重定向后立即执行 `resetFlow()`，保证 Store 凭据在认证成功终局彻底重置。
2. **`src/volur-lite-view-NEXT/src/views/auth/signin/challenge/pwdChallengeView.vue`**：
   - 修正 `handleForgotPassword` 逻辑，在跳转前清空挑战专用的 `tl` 与 `cid`，触发统一 Scrim 动效，并跳转至标准的找回入口 `/v1/recovery/selection`；
   - 模板中为“忘记密码”超链接添加 `:isScrim="true"` 属性。
3. **`src/volur-lite-view-NEXT/src/views/auth/signin/challenge/rejectView.vue`**：
   - 在 `handleRejectedBack` 返回登录页前显式清空 `tl` 与 `cid`，消除脏票据回流。
4. **`src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue`**：
   - 选中 `ACTIVE` 状态账号跳转前调用 `resetFlow()`；
   - 点击“使用其他账号”时清空 `identifier`、`tl` 与 `cid`，保证以纯净状态进入账号输入界面。
5. **`src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts`**：
   - 对齐 `getQueryParams` 规范，仅在 `dsh.value` 存在时输出 `dsh`，仅在 `cid.value > 0` 时输出 `cid`。

## 4. 影响范围与验证

- **影响范围**：
  - 登录主流程（`signinFlow.ts`、`pwdChallengeView.vue`、`rejectView.vue`、`AccountChooserView.vue`）
  - 注册流程参数输出（`signupFlow.ts`）
- **验证结果**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，无任何 TypeScript 编译与构建错误；
  - 登录全链路各节点会话状态与后端生命周期严格闭环，杜绝因凭据泄漏导致的误跳超时页。
