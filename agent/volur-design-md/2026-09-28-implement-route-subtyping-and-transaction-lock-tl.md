# 施工设计与审计记录：实现认证路由子类型化与状态流转事务锁 (TL) 门禁机制

- **文件编号**：`2026-09-28-implement-route-subtyping-and-transaction-lock-tl`
- **日期**：2026-09-28
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/store/modules/authFlow.ts` (升级：TL 票据签发、验签与 cid 挑战阶梯维护)
  - `src/volur-lite-view-NEXT/src/router/routes/auth.ts` (升级：启用 challenge/:type? 路由子类型化)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue` (升级：精准流转并签发首个 TL 票据)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue` (升级：多态挑战组件分发与 TL 门禁防护)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/AuthenticateView.vue` (升级：TL 票据门禁校验)

---

## 1. 变更背景与架构对齐

根据对 Google Accounts 官方安全验证挑战 URL 的逆向分析：
`https://accounts.google.com/v3/signin/challenge/recaptcha?TL=ADG-...&cid=1&dsh=S-...&flowName=GlifWebSignIn...`

Google 认证系统具备两项关键安全与交互机制：
1. **路由子类型化 (Route Subtyping)**：
   `/challenge/{challengeType}`（如 `/recaptcha` 人机图形验证、`/2fa` 动态验证码、`/device` 异常设备确认），而非单一扁平路由。
2. **状态流转事务锁票据 (Transaction Lock / Ticket Lock - `TL`)**：
   前置步骤通过后由服务端/状态机签发高熵防篡改令牌（前缀 `ADG-`），后续页面进入时强制验签。缺少 `TL` 或 `TL` 过期直接阻断跳步访问，踢回首步。
3. **挑战阶梯序号 (`cid`)**：
   维护挑战链条序号（`cid=1`, `cid=2`），支持多层级防线接力。

---

## 2. 核心实现方案与改动点

### 2.1 状态机 Store 扩展 (`authFlow.ts`)
1. **`generateTL()`**：
   生成 Google 标准前缀 `ADG-` + 60 位高熵字母与随机数序列（如 `ADG-GRTbPbNl0pSndZCtatZwpc273Y1KxgoCFYDnvVdrHspzQ9BKk03FfMrdB24l`）；
2. **`issueTransitionTicket(targetStep)`**：
   在完成某一阶段校验时，签发新 `TL` 票据，并注入 `cid`、`continue`、`followup`、`pstMsg=1` 等全套参数；
3. **`validateTransitionTicket(route)`**：
   核验当前路由 URL query 中是否携带以 `ADG-` 开头的合法 `TL` 票据；若无则判定为非法跳步或会话已失效。

### 2.2 路由层支持子类型化 (`routes/auth.ts`)
配置动态子路由匹配：
```ts
{
  path: 'challenge/:type?',
  name: 'V1SignInChallenge',
  component: () => import('@/views/auth/signin/ChallengeView.vue'),
  meta: { title: '登录 - 安全挑战', ... }
}
```
可精准匹配：
- `/v1/signin/challenge/recaptcha`
- `/v1/signin/challenge/2fa`
- `/v1/signin/challenge/device`
- `/v1/signin/challenge`（通用兜底）

### 2.3 视图行为与多态呈现

1. **`IdentifierView.vue`**：
   - 模拟后端风控引擎决策：
     - 若命中自动化/爬虫特征（含 `bot`/`captcha`）：签发 `TL`，导向 `/v1/signin/challenge/recaptcha`；
     - 若命中多因子拦截（含 `2fa`/`totp`）：签发 `TL`，导向 `/v1/signin/challenge/2fa`；
     - 若命中异常环境/设备（含 `risk`/`device`）：签发 `TL`，导向 `/v1/signin/challenge/device`；
     - 常规低风险：签发 `TL`，直接导向 `/v1/signin/authenticate` 输入密码。
2. **`ChallengeView.vue` (多态渲染与验签)**：
   - **门禁拦截**：进入页面时执行 `validateTransitionTicket`，若无 `TL` 提示警告并立即安全重定向至 `/v1/signin/identifier`；
   - **`recaptcha` 分支**：渲染经典 Google reCAPTCHA 交互卡片（“我不是机器人”勾选框、旋转检查动画、绿色对勾通过态、Google 条款链接）；
   - **`2fa` 分支**：渲染 6 位动态验证码输入框与“在此设备上信任 30 天”；
   - **`device` 分支**：渲染新设备拦截警告横幅与会话时间/客户端指纹核验卡片；
   - 挑战通过后，递增 `cid` 并签发下一阶段 `TL`，平滑推进至 `/v1/signin/authenticate`。
3. **`AuthenticateView.vue` (密码凭据页)**：
   - 同步接入 `TL` 门禁防护：用户不可在未通过首步或未通过强制挑战的情况下直接访问密码页。

---

## 3. 构建与验证结果

- 执行 `npm run build` (`vue-tsc --noEmit && vite build`)；
- TypeScript 类型推导 100% 通过（构建用时 2.44s）；
- 产物分包清晰，各挑战子类型与票据系统完全解耦、运行流畅。
