# 施工设计与审计记录：重构前端渐进式认证状态机（分流 Authenticate 与 Challenge）

- **文件编号**：`2026-09-28-reconstruct-google-auth-flow-with-authenticate-and-challenge-split`
- **日期**：2026-09-28
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/store/modules/authFlow.ts` (新增：状态机与 Google URL 参数管理 Store)
  - `src/volur-lite-view-NEXT/src/router/routes/auth.ts` (重构：拆分 authenticate 与 challenge 路由)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue` (重构：参数自动补全与风控决策分流)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/AuthenticateView.vue` (新增：标准无风险密码核验页)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue` (重构：专属 2 步安全验证挑战页)
  - `src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue` (重构：状态机参数透传接入)

---

## 1. 变更背景与需求纠偏

在原先的认证流程设计中存在一个关键误区：将第二步直接无条件引导至 `/v1/signin/challenge`，并在其中输入密码。
经过与 Google Accounts 真实工业级状态机架构对齐分析：
1. **Challenge 的真实语义**：`challenge` 代表“安全挑战 / 2FA / 风控核验”。它**绝非日常标准密码输入页面**，而是当系统检测到 IP 异常、非常用设备、暴力破解嫌疑或账号配置了强制双重认证时，才触发的拦截门禁；
2. **常规认证的标准路径**：当第一步 `/v1/signin/identifier` 输入账号并完成初筛评估后，若**无环境风险**，应当直接进入标准凭据核验页 `/v1/signin/authenticate` 输入密码；
3. **URL 状态机参数全生命周期维护**：从进入登录页开始，地址栏必须规范化挂载 `continue`、`flowName`、`flowEntry` 与 `dsh`，且在所有后续路由流转（回退选账号、改用现有账号、注册成功回跳、挑战通过流转）中必须全流程无损透传。

---

## 2. 核心架构与落地改动

### 2.1 状态机 Store 封装 (`authFlow.ts`)
在 `src/volur-lite-view-NEXT/src/store/modules/authFlow.ts` 中定义专属 Pinia Setup Store：
- **`dsh` 生成器**：严格生成 `S-<9位集群实例>:<16位微秒时间戳与高熵随机数>`（如 `S-969965009:1790527266429740`）；
- **`ensureFlowParams(route, router)`**：挂载在各页面 `onMounted` 钩子中，检测如果当前 URL 缺失必要参数，通过 `router.replace` 静默规范化 URL；
- **`getQueryParams()`**：统一打包 `continue`、`flowName`、`flowEntry`、`dsh` 以及已识别的 `identifier`，供 `router.push` 调用。

### 2.2 路由重构 (`router/routes/auth.ts`)
认证子路由扩展为四大核心状态节点：
1. `/v1/signin/identifier`：第一步识别账号；
2. `/v1/signin/authenticate`：第二步标准密码核验（常规低风险分支）；
3. `/v1/signin/challenge`：第二步安全挑战核验（高风险/2FA拦截分支）；
4. `/v1/signup`：新用户注册引导。

### 2.3 视图改造与风控决策分支

1. **`IdentifierView.vue`**：
   - 页面挂载时自动执行 `ensureFlowParams` 挂载 `dsh`；
   - 点击“下一步”提交账号后进行风险决策判定：
     - 若命中风险（如测试账号含 `risk` 或 `2fa`，或后端风控引擎返回异常）：跳转至 `/v1/signin/challenge`；
     - 若环境正常：跳转至 `/v1/signin/authenticate`。
2. **`AuthenticateView.vue`**（新创建）：
   - 左侧副标题处通过 `Teleport` 挂载 Google 风格的账号标签芯片（显示头像字样、用户名/邮箱与下拉指示符，点击可退回首步切换账号）；
   - 具备带浮动标签的密码输入框（`GoogleTextField`）、“显示密码”复选框与“忘记密码？”链接；
   - 点击“登录”完成认证后，自动重定向至 `continue` 目标地址（默认 `/competitions`）。
3. **`ChallengeView.vue`**（重构）：
   - 明确为专属安全挑战页，包含安全盾牌提示横幅、动态验证码输入框、信任设备勾选项与“改用其他方式”链接；
   - 安全挑战核验通过后，再推进至密码核验或最终登录。
4. **`SignUpView.vue`**：
   - 接入 `authFlowStore`，在“改用现有账号登录”以及注册成功前往登录时，均完整携带 `continue` 与 `dsh`，避免状态丢失。

---

## 3. 构建与验证结果

- 执行 `npm run build` (`vue-tsc --noEmit && vite build`)；
- TypeScript 类型推导 100% 通过，产物构建用时 2.92s；
- 各认证状态视图拆分为独立 chunk，无任何循环引用或告警。
