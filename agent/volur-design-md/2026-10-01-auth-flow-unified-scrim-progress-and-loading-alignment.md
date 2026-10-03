# 认证与注册流程统一遮罩、延迟与进度条交互对齐方案

## 1. 变更背景与需求分析

在 Google 风格的账号体系中，用户在页面上进行的任何关键动作（无论是异步 API 鉴权，还是各子步骤间的路由跳转、返回上一步、选择验证项、跳过可选配置等），均具备高度统一的视觉与交互反馈：
1. **顶部卡片水平进度条**（Google Dual-Track Progress Bar）：在操作触发瞬间启动无限循环双波浪平滑动效；
2. **全屏背景遮罩**（Auth Scrim）：以柔和淡入的形式降低背景干扰，强化当前操作聚焦度；
3. **卡片透明点击阻断层**（Card Action Blocker）：在处理过程中阻止用户的连击与重复提交（Debounce & Anti-Duplicate）；
4. **最小延迟保底机制**（Minimum Loading Duration，1000ms）：即使接口或路由跳转极快，亦确保动效至少展示 1000ms，避免界面闪烁（Flicker-Free），提供沉稳可靠的操作质感。

此前，上述体验仅在 `IdentifierView.vue`（登录账号输入页）中的“下一步”和“创建账号”按钮上得以完整体现，而在 `AuthLayout.vue` 承载的注册流程各步骤（`EmailStepView`、`EtotpChallengeStepView`、`OptionsChallengeStepView`、`PasskeyProtectView`、`TotpProtectView`、`SignUpView`）以及登录挑战流程各组件中，部分按钮与列表项缺乏 `triggerScrim` 或 `withLoading` 联动，导致交互体验出现断层。

---

## 2. 交互体系架构设计

```mermaid
flowchart TD
    A["用户交互事件<br/>(点击按钮 / 列表项 / 头部胶囊)"] --> B{操作类型}
    
    B -->|异步请求接口<br/>(submitIdentifier / submitChallenge / submitSignUp*)| C["flowStore.withLoading(action)"]
    B -->|纯前端路由跳转 / 状态流转<br/>(goBack / handleSkip / handleSelectOption / goToSignIn)| D["flowStore.triggerScrim(action, 1000ms)"]
    
    C --> E["激活 Store 状态: loading = true"]
    D --> E
    
    E --> F["AuthLayout.vue 状态联动"]
    
    subgraph UI视觉反馈 ["AuthLayout.vue 反馈体系"]
        F --> G["auth-scrim 遮罩淡入"]
        F --> H["auth-card__progress 水平进度条滚动"]
        F --> I["auth-card__blocker 拦截卡片重复交互"]
    end
    
    C --> J["执行业务逻辑并测算耗时 elapsed"]
    D --> J
    
    J --> K{"耗时是否小于 1000ms?"}
    K -->|是| L["setTimeout 补足至 1000ms"]
    K -->|否| M["立即进入下一阶段"]
    
    L --> N["置 loading = false"]
    M --> N
    N --> O["UI 遮罩与进度条平滑退出"]
```

---

## 3. 具体改造清单

### 3.1 注册可选保护步骤与列表项 (`OptionsChallengeStepView.vue`)
- **保护项列表项** (`.auth-selection-item`)：
  - 点击 `handleSelectOption(opt)` 时，包裹 `signupFlowStore.triggerScrim` 执行路由推进；
  - 列表项添加 `:isScrim="true"` 属性与全局事件响应协同。
- **“跳过”与“继续”按钮** (`handleSkip`, `handleContinue`)：
  - 包裹 `signupFlowStore.triggerScrim` 重置 flow 并完成重定向跳转；
  - 按钮绑定 `:disabled="loading"` 与 `:isScrim="true"`。

### 3.2 注册各阶段导航与返回按钮
- **`EmailStepView.vue`**：
  - “返回上一步”按钮 (`goBackName`)：包裹 `signupFlowStore.triggerScrim`；
  - 按钮绑定 `:disabled="loading"` 与 `:isScrim="true"`。
- **`EtotpChallengeStepView.vue`**：
  - “返回修改邮箱”按钮 (`goBackEmail`)：包裹 `signupFlowStore.triggerScrim`；
  - 按钮绑定 `:disabled="loading"` 与 `:isScrim="true"`。
- **`PasskeyProtectView.vue`**：
  - “创建通行密钥”按钮 (`handleCreatePasskey`)：核心 WebAuthn 凭据创建及向后端注册流程统一包裹进 `signupFlowStore.withLoading`；
  - “稍后 / 返回选项”按钮 (`handleBackOptions`)：包裹 `signupFlowStore.triggerScrim`；
  - “继续”按钮 (`handleFinish`)：包裹 `signupFlowStore.triggerScrim`；
  - 按钮绑定 `:disabled="loading || isRegistering"` 与 `:isScrim="true"`。
- **`TotpProtectView.vue`**：
  - “返回”和“知道了”按钮 (`handleBackOptions`)：包裹 `signupFlowStore.triggerScrim`；
  - 按钮绑定 `:disabled="loading"` 与 `:isScrim="true"`。

### 3.3 头部账号芯片胶囊按钮
- **`SignUpView.vue`** (`.auth-account-chip`)：
  - 点击 `goBackName` 时包裹 `signupFlowStore.triggerScrim`，并绑定 `:isScrim="true"` 与 `:disabled="signupFlowStore.loading"`。
- **`ChallengeView.vue`** (`.auth-account-chip`)：
  - 点击 `goBackIdentifier` 时包裹 `signinFlowStore.triggerScrim`，并绑定 `:isScrim="true"` 与 `:disabled="signinFlowStore.loading"`。

---

## 4. 验证与审计

1. **类型检查与构建**：
   - 执行 `npx vue-tsc --noEmit`，TypeScript 类型检查通过（0 error）；
   - 执行 `npm run build`，Vite 生产包构建成功，静态资源打包无报错。
2. **代码风格与规范符合性**：
   - 严格遵循零注释原则，代码文件干净纯粹；
   - 遵循 ES2026 与 Google Material Design Workspace 视觉与色彩规范。
