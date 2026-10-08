# 找回用户名流程会话凭据泄漏与状态未重置缺陷修复

## 1. 变更背景

在用户从登录页面（`IdentifierView.vue`）点击“忘记用户名”进入找回用户名选项页（`SelectionView.vue`），随后选择“电子邮箱”找回途径并顺利完成邮件发送（`EmailView.vue`）后，用户返回登录页（`IdentifierView.vue`）再次点击“忘记用户名”时，系统出现异常并被强制重定向至会话超时页（`sessionExpireView.vue`）。

## 2. 根因分析

1. **会话生命周期在后端已销毁**：
   在 `RecoveryServiceImpl.java` 中，当用户提交邮箱找回请求（`submitRecoveryEmail`）成功投递邮件后，后端执行了 `transitionLockManager.burnAndCompleteSession(...)`，该方法通过 `sessionManager.destroySession(dsh)` 在 Redis / 会话存储中彻底物理删除了当前会话（Session Burned）。
2. **前端 Pinia 状态与路由参数未清理**：
   在 `EmailView.vue` 中，当邮件提交成功后前端未调用 `signinFlowStore.resetFlow()`，导致已经被后端销毁的 `dsh`、失效的流转票据 `tl` 以及序列号 `cid` 依然滞留在 Store 中。
   在用户点击“返回登录”时，`signinFlowStore.getQueryParams()` 仍将已失效的 `dsh` 拼接到 URL Query 中传回 `/v1/signin/identifier`。
3. **前端本地时间戳判断导致未能自愈**：
   `IdentifierView.vue` 在初始化时执行 `ensureFlowParams`，由于仅依靠本地时间戳与 15 分钟 TTL（`isDshExpired`）进行粗粒度判定，未能识别出已被服务端销毁的会话，因此未重新调用 `/session/initSess` 初始化新会话。
4. **再次操作触发 401 拦截导致重定向**：
   用户再次点击“忘记用户名”并尝试选择方式时，带入已销毁的 `dsh`，后端抛出 `HttpStatus.UNAUTHORIZED`（401）且提示“登录或业务流程会话句柄已失效或不存在”。前端 Axios 拦截器捕获后触发 `buildSessionExpireHref()`，强行将页面重定向至 `/v1/info/sessionexpire`。

## 3. 改动点

1. **`src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts`**：
   - 优化 `getQueryParams` 方法，仅在 `dsh.value` 存在时才在 Query 参数中写入 `dsh`，仅在 `cid.value` 大于 0 时写入 `cid`，彻底杜绝空值或已清空状态下输出冗余参数。
2. **`src/volur-lite-view-NEXT/src/views/auth/recovery/username/EmailView.vue`**：
   - 在 `submitRecoveryEmailApi` 成功响应后，立即调用 `signinFlowStore.resetFlow()` 重置状态机，并使用 `router.replace` 将当前 URL 中的无效 `dsh` 与 `TL` 剔除。
   - 在“返回登录”操作（`handleBackToSignin`）中调用 `resetFlow()`，确保返回登录页时 Query 不含旧凭据，引导 `IdentifierView` 正确触发 `/session/initSess` 开启全新生命周期。
3. **`src/volur-lite-view-NEXT/src/views/auth/recovery/SelectionView.vue`**：
   - 在从选择页点击“返回登录”时，显式清空 `tl` 与 `cid`，防止找回流程的中间挑战票据污染登录流程。

## 4. 影响范围与验证

- **影响范围**：
  - 找回用户名流程（`EmailView.vue`、`SelectionView.vue`）
  - 登录流程 Store 会话重置逻辑（`signinFlow.ts`）
- **验证结果**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，无任何类型与构建错误；
  - 邮件发送成功后凭据即时清空，再次进入找回或重新登录时均可平滑创建新会话，杜绝误跳 `sessionExpireView.vue`。
