# 施工与修改记录：认证流程 followup 参数纠正与会话超时重入架构修复

- **实施日期**：2026-10-01
- **涉及模块**：
  - 前端工程：`volur-lite-view-NEXT`（`signupFlow.ts`, `signinFlow.ts`, `flow.ts`, `sessionExpireView.vue`, `IdentifierView.vue`, `NameStepView.vue`）
- **核心架构**：Google 风格认证状态机（`VlifWebSignUp` / `VlifWebLogin`）、会话句柄（`dsh`）生命周期与超时重试重入机制

---

## 1. 变更背景与问题定位

在用户进行新账号注册流程（如在 `NameStepView.vue` 输入用户名步骤）中，当 `dsh` 会话过期后点击“下一步”，系统跳转至超时提示页面：
`/v1/info/sessionexpire?continue=%2F&followup=%2F`

经深度链路溯源，定位到以下关键问题：
1. **参数语义混淆与默认值错误覆盖**：
   - `continue` 代表用户认证/注册彻底完成后的**最终业务目标落地页**（如 `/competitions`）；
   - `followup` 代表会话异常或超时中断后，用户点击“重试”时的**流程重启重入点**（注册流程应为 `/v1/signup`，登录流程应为 `/v1/signin/identifier`）；
   - 此前在 `signupFlow.ts` 中，`continueUrl` 与 `followup` 初始值被默认设为了 `'/'`，且在 `ensureFlowParams` 中由于照搬登录流逻辑，在缺少 `followup` 时错误地将其赋值为 `newQuery.continue`（即 `'/'`）；
   - 导致注册页面挂载时通过 `router.replace` 将 URL 强行覆写为 `?continue=/&followup=/&...`。
2. **超时构建降级探测机制被“假值”绕过**：
   - 在 `flow.ts` 的 `buildSessionExpireHref` 与 `buildSessionExpireLocation` 中，原本编写了根据路径前缀将缺少 `followup` 的请求降级为 `/v1/signup` 的容错代码；
   - 但因为上一步将 `followup` 强行写入了 `'/'`（非空字符串），导致 `!followupTarget` 条件判定为 `false`，兜底逻辑被彻底绕过，最终输出 `continue=%2F&followup=%2F`。
3. **超时落地页连锁反应**：
   - 在 `sessionExpireView.vue` 中，由于 `followup` 为 `'/'`，既无法根据 `followup.includes('signup')` 正确匹配注册状态机，又在用户点击“重试”时因 `followup.startsWith('/')` 直接将用户重定向至根路径 `/`，导致用户丢失注册流程。
4. **跨流程切换时的参数污染**：
   - 在 `IdentifierView.vue`（“创建账号”）与 `NameStepView.vue`（“改用现有账号登录”）互相跳转时，原逻辑全量透传了原有 Store 的 query 参数（包括对端的 `dsh` 和 `followup`），造成跨流程污染。

---

## 2. 详细改造与工程实现

### 2.1 状态机初始值与参数补齐修复 (`signupFlow.ts` & `signinFlow.ts`)
1. **`signupFlow.ts`**：
   - 将 `continueUrl` 默认值设为 `/competitions`，`followup` 默认值设为 `/v1/signup`；
   - 在 `ensureFlowParams` 中，当 `continue` 为空时兜底为 `query.redirect || '/competitions'`；
   - 当 `followup` 为空或为 `'/'` 时，规范赋值为 `'/v1/signup'`。
2. **`signinFlow.ts`**：
   - 将 `continueUrl` 默认值设为 `/competitions`，`followup` 默认值设为 `/v1/signin/identifier`；
   - 在 `ensureFlowParams` 中，当 `continue` 为空时兜底为 `query.redirect || '/competitions'`；
   - 当 `followup` 为空或为 `'/'` 时，规范赋值为 `'/v1/signin/identifier'`。

### 2.2 超时跳转链接构建防伪值降级 (`flow.ts`)
- 在 `buildSessionExpireLocation` 与 `buildSessionExpireHref` 中：
  - 判定条件升级为 `if (!followupTarget || followupTarget === '/')`；
  - 杜绝 `'/'` 作为伪有效参数绕过路径探测，确保注册路径统一降级为 `/v1/signup`，登录路径统一降级为 `/v1/signin/identifier`。

### 2.3 超时重试重入决策优化 (`sessionExpireView.vue`)
- 在 `onMounted` 中，若检测到 `followup` 缺失或为 `'/'`，根据 `continue` 是否包含 `signup` 智能自愈为 `/v1/signup` 或 `/v1/signin/identifier`；
- 在 `resolveTargetDestination` 中，针对 `followup.startsWith('/') && followup !== '/'` 进行精准匹配，若无法匹配且无特定路径，兜底返回 `/v1/signin/identifier` 并携带原始 `continue`，杜绝直接将未登录访客丢出到 `/` 的缺陷。

### 2.4 登录与注册流程切换解耦 (`IdentifierView.vue` & `NameStepView.vue`)
- 在 `IdentifierView.vue` 点击“创建账号”时，仅传递目标业务地址 `continue` 与 `followup: '/v1/signup'`，重置注册流参数，杜绝沿用登录流的旧 `dsh`；
- 在 `NameStepView.vue` 点击“改用现有账号登录”时，仅传递目标业务地址 `continue` 与 `followup: '/v1/signin/identifier'`，重置登录流参数，杜绝沿用注册流的旧 `dsh`。

---

## 3. 验证与审计结论

1. **前端工程编译构建**：
   - 执行 `npm run build`，TypeScript 类型检查与 Vite 生产构建全部通过，无任何编译错误；
2. **零注释铁律遵守**：
   - 所有 TypeScript 与 Vue 文件保持极致纯粹与紧凑，无冗余行内备注或解释性注释，全部技术方案收敛记录于本文档中。
