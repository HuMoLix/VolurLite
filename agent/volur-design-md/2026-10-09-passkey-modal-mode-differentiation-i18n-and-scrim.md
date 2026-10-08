# PasskeyModal 模式分流、i18n 多语言对齐与 isScrim 延迟交互规范实现

## 1. 变更背景与需求概述

针对通行密钥模态框 [`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 提出以下演进要求：
1. **模式区分（Mode Differentiation）**：清晰区分“创建通行密钥（`create`）”与“使用通行密钥（`use` / `verify`）”两种业务场景，不同场景具备各自专属的标题、描述文案以及主操作按钮标签；
2. **字体与基线对齐（Font & Alignment Harmony）**：解决此前“选择其他设备”（原普通 `<button>`）与“创建安全密钥”（`<n-button>`）在没有 Tailwind Preflight 保护下继承的字体族、字阶与行高不一致导致的基线和文字排版不对齐问题；
3. **i18n 多语言全覆盖**：对通行密钥模态框涉及的所有静态文案与多态标签进行中简（`zh-CN`）、中繁（`zh-TW`）和英文（`en-US`）的规范化抽取与接入；
4. **统一 isScrim 延迟与遮罩层**：完全参考 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 的卡片加载遮罩与动效体系，为模态框内的按钮动作增加 1000ms（`MIN_LOADING_DURATION_MS`）的操作阻断延迟、卡片顶部 Material 线性渐进进度条（`passkey-modal__progress`）及阻断遮罩（`passkey-modal__blocker`）。

---

## 2. 核心改动点

### 2.1 PasskeyModal 模式区分与动态计算属性
- 在 [`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 中定义 `mode?: 'create' | 'use' | 'verify'` 属性（默认为 `'create'`）；
- 构建响应式计算属性自动匹配文案：
  - **`displayTitle`**：
    - `create`：`t('auth.passkey_modal_create_title')`（“为您的 Volur 账号创建通行密钥” / “Create a passkey for your Volur Account”）；
    - `use` / `verify`：`t('auth.passkey_modal_use_title')`（“使用通行密钥确认是您本人” / “Use your passkey to confirm it's really you”）；
  - **`displayDescription`**：
    - `create`：`t('auth.passkey_modal_create_desc')`；
    - `use` / `verify`：`t('auth.passkey_modal_use_desc')`；
  - **`displayPrimaryText`**：
    - `create`：`t('auth.passkey_modal_create_btn')`（“创建安全密钥”）；
    - `use` / `verify`：`t('auth.passkey_modal_use_btn')`（“继续”）；
  - **`displayOtherDeviceText`**：`t('auth.passkey_modal_choose_other_device')`（“选择其他设备”）；
  - **`displayCancelText`**：`t('common.cancel')`（“取消”）；
- 兼容通过组件 Prop 显式覆盖。

### 2.2 按钮体系重构与字体对齐修复
- 将所有底部操作按钮（“取消”、“选择其他设备”、“创建安全密钥 / 继续”）统一替换为 Naive UI 的 `<n-button>` 组件；
- 统一样式规则：
  - 设置统一高度 `36px`、圆角 `9999px`、字体大小 `14px`、字重 `500`、行高 `20px` 以及 `display: inline-flex; align-items: center; justify-content: center`；
  - 深度选择器 `:deep(.n-button__content)` 显式继承 `font-family: inherit` 与相同字阶，彻底消除了浏览器原生 `<button>` 在 Preflight 禁用环境下出现的字体脱节与基线偏斜。

### 2.3 isScrim 延迟机制与卡片动效集成
- 参考 `AuthLayout.vue`，在模态卡片顶部引入 Material Design 标准线性渐进进度条（包含轨道与主/副位移动效 `mdc-linear-progress-*-indeterminate-*`）；
- 引入卡片级防抖阻断遮罩 `passkey-modal__blocker`，捕获并拦截点击、鼠标、键盘与触控事件；
- 实现 `runWithScrim(action)`：确保所有按钮触发的异步 WebAuthn 过程或关闭过程具有至少 1000ms 的平滑状态过渡；
- 底部各按钮均打上 `:isScrim="true"` 属性并配置独立的 `:loading` 状态。

### 2.4 多语言词条补齐
- 同步在 `zh-CN/auth.json`、`en-US/auth.json`、`zh-TW/auth.json` 中配置了通行密钥模态框双模式词条。

### 2.5 挑战视图同步对接
- 在 [`passkeyChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/passkeyChallengeView.vue) 中将 `<PasskeyModal>` 的 `mode` 设置为 `'use'`；
- 在点击挑战视图底部的“验证”按钮时，增加 `await signinFlowStore.triggerScrim(undefined, 1000)` 延迟遮罩层后再弹出模态框，保持全端交互体验的高度一致性。

---

## 3. 规范与质量验证
- **全代码零注释**：组件与视图代码严格恪守纯净规范，不含任何行内备注；
- **构建测试**：通过 `vue-tsc --noEmit && vite build` 编译，零类型错误。
