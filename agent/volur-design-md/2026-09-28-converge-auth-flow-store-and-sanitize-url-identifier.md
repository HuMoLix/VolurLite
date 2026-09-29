# 施工与审计记录：收敛认证状态机 Store 账号存储并净化 URL Query 中的明文账号

## 1. 变更背景与架构动机
在早期认证流原型中，前端为了在刷新或跨步骤流转时能够展示用户账号芯片（Account Chip）与保留状态，直接将用户输入的明文账号作为 URL Query 参数（`identifier=test`）拼接在路由之后。

经过对 Google Accounts 工业级 IAM 与零信任架构的逆向对比分析，发现：
1. **隐私与安全合规隐患**：在 URL Query 中明文携带用户账号/邮箱，会导致该凭据留在浏览器历史记录、服务端 Access Log，并在跨域引用或跳转时通过 HTTP `Referer` 标头外泄；
2. **与 Google 体验脱节**：Google 官方登录在由 `/v3/signin/identifier` 流转至 `/challenge/pwd` 或 `/challenge/recaptcha` 时，URL 仅包含流程控制参数（`dsh`, `TL`, `cid`, `flowName`, `flowEntry`, `continue`, `followup`, `pstMsg`），完全不暴露任何账号明文。账号上下文均通过服务端会话（由 `dsh` 索引）或客户端受限安全存储维护。

为此，对前端认证流转 Store 及各登录视图进行全面收敛与净化重构。

---

## 2. 核心改动点说明

### 2.1 `src/store/modules/authFlow.ts`
1. **引入会话持久化与状态隔离**：
   - 增加常量 `STORAGE_IDENTIFIER_KEY = 'volur_auth_identifier'`；
   - Store 初始化时优先从 `sessionStorage` 恢复已有账号；
   - 新增 `setIdentifier(val: string)` 方法，同步响应式变量与 `sessionStorage`，保障标签页生命周期内刷新页面状态不丢失；
   - `resetFlow()` 重置流程时，同步清除 `sessionStorage` 中的账号凭据。
2. **URL Query 生成净化 (`getQueryParams`)**：
   - 彻底移除 `params.identifier = identifier.value`，严格保持 URL 参数仅用于流转控制。
3. **入参防御与地址栏即时净化 (`ensureFlowParams`)**：
   - 增加防御拦截逻辑：若从旧书签或外部历史链接携带了 `?identifier=...` 进入页面，Store 自动将其安全提取并摄入 `setIdentifier()`，随后立即从 URL Query 中剔除该字段并通过 `router.replace` 静默重写浏览器地址栏，实现即时脱敏与净化。

### 2.2 登录视图改造
1. **`IdentifierView.vue`**：
   - 校验通过后，使用 `authFlowStore.setIdentifier(trimmed)` 写入 Store，由 `issueTransitionTicket()` 签发的后续路由 Query 不再附带 `identifier`。
2. **`AuthenticateView.vue`**：
   - 账号芯片显示改从 `authFlowStore.identifier` 读取；
   - 增加空账号门禁校验：若用户直接在地址栏回车进入密码页且无已输入账号上下文，自动通过 `window.$message.warning` 提示并重定向回第一步 `/v1/signin/identifier`。
3. **`ChallengeView.vue`**：
   - 同样将展示和门禁对齐 Store 中的 `authFlowStore.identifier`，消除对 `route.query.identifier` 的依赖。
4. **`SignUpView.vue`**：
   - 账号注册完成后，调用 `authFlowStore.setIdentifier(email.value)` 并通过干净的 `authFlowStore.getQueryParams()` 跳转至登录页。

---

## 3. 影响范围与回归验证
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/store/modules/authFlow.ts`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/AuthenticateView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue`
- **构建与回归验证**：
  - 执行 `npm run build`，TypeScript 类型检查与 Vite 生产级 Bundle 打包全部通过（0 错误，0 警告）。
  - 路由跳转后地址栏变为纯净的 Google 工业级格式：
    `http://127.0.0.1:3001/v1/signin/authenticate?continue=/competitions&followup=/competitions&flowName=GlifWebSignIn&flowEntry=ServiceLogin&dsh=S-...&pstMsg=1&TL=...&cid=1`
