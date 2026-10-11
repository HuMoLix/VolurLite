# 注册流程 PasskeyProtectView 未调用 PasskeyModal 之现状深度分析与 create 模式演进方案

## 1. 现状剖析与问题根因

在注册流程子路由 `/v1/signup/steps/pt/pk`（由 [`SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue) 动态挂载的 [`passkeyProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/passkeyProtectView.vue)）中，当前存在严重的代码重复与架构割裂问题。

### 1.1 核心缺陷分析

1. **绕过统一弹窗与视觉断层（Bypassing PasskeyModal）**：
   - 现行 `passkeyProtectView.vue` 点击“创建通行密钥”按钮后，直接在主视图内部调用 `handleCreatePasskey()`，立即拉起浏览器/操作系统底层 WebAuthn 凭据创建弹窗。
   - 缺少如登录验证视图 [`passkeyChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/passkeyChallengeView.vue) 一致的 Google 质感过渡模态弹窗（[`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue)），丢失了插图引导（`/static/svg/passkeys_scene_add_account.svg`）、进度指示与场景说明。
2. **底层 WebAuthn 逻辑严重重复（Massive Code Duplication）**：
   - `passkeyProtectView.vue` 内部私自实现了近 200 行底层 WebAuthn 编码与接口调用逻辑（包括 `copyToPureArrayBuffer`、`base64UrlToBuffer`、`bufferToBase64Url`、`fetchPasskeyCreationOptionsApi`、`submitPasskeyRegisterApi`、凭据选项组装与异常捕获）；
   - 这些底层能力早已被抽象并稳定维护在平台安全核心组件 [`passkey.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/passkey.vue) 之中，形成双份冗余且极易在协议升级时产生分型缺陷。
3. **缺少认证器类型分流选择（Missing Attachment Selection）**：
   - `PasskeyModal.vue` 原生支持“选择其他设备”（`attachment: 'cross-platform'`，支持物理安全密钥或跨设备手机扫码）与“创建安全密钥”（`attachment: 'platform'`，本设备 Windows Hello / Touch ID 等）；
   - `passkeyProtectView.vue` 现行代码仅能单一依赖系统默认或选项配置，用户无法主动选择是否使用外部硬件密钥。

---

## 2. passkeyChallengeView 模式参考与差异对比

### 2.1 登录挑战视图的规范实现（mode="use"）

在 [`passkeyChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/passkeyChallengeView.vue) 中：
1. **视图职责轻量化**：仅负责呈现当前验证账号信息、尝试时间及底部操作栏；
2. **平滑过渡交互**：主按钮点击时触发 1000ms 的 `triggerScrim`，随后唤起 `isModalVisible = true`；
3. **受控模态弹窗挂载**：
   ```vue
   <PasskeyModal
     v-model:visible="isModalVisible"
     mode="use"
     :dsh="signinFlowStore.dsh"
     :tl="signinFlowStore.tl"
     @success="handleVerifySuccess"
     @error="handleVerifyError"
   />
   ```
4. **全生命周期受控**：成功与失败均通过清晰的事件派发向上传递，视图通过 `useFormError` 集中管理错误展示。

### 2.2 通行密钥模态框对 create 模式的原生支持准备度

[`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 在此前重构中已完全具备 `mode="create"` 的就绪支持：
- **动态文案与国际化**：
  - 标题：`t('auth.passkey_modal_create_title')`（“为您的 Volur 账号创建通行密钥”）
  - 描述：`t('auth.passkey_modal_create_desc')`（“创建通行密钥后，您只需使用指纹、面容或屏幕锁定即可登录...”）
  - 主操作按钮：`t('auth.passkey_modal_create_btn')`（“创建安全密钥”）
  - 次操作按钮：`t('auth.passkey_modal_choose_other_device')`（“选择其他设备”）
  - 词条均已完备收敛于 `zh-CN`、`en-US`、`zh-TW` 语言包。
- **动作路由分流**：
  - 点击“选择其他设备”：调用 `passkeyRef.value?.createPasskey({ attachment: 'cross-platform', dsh, tl, username })`
  - 点击“创建安全密钥”：调用 `passkeyRef.value?.createPasskey({ attachment: 'platform', dsh, tl, username })`
  - 注册成功后派发 `@success`，内部自动关闭弹窗。

---

## 3. 注册保护视图（passkeyProtectView）演进与接入方案

### 3.1 状态与流程适配

- **上下文 Store 替换**：由登录流程的 `signinFlowStore` 替换为注册流程的 `signupFlowStore`；
- **参数传递**：向 `PasskeyModal` 传递 `:dsh="signupFlowStore.dsh"`、`:tl="signupFlowStore.tl"` 以及 `:username="signupFlowStore.username"`，确保注册挑战验证与凭据绑定归属到正确的待注册用户；
- **注册成功流转**：
  - 收到 `@success` 后，将页面 `passkeyRegistered` 状态置为 `true`；
  - 页面自动切换为绿色对勾成功形态，主按钮转变为“下一步”（点击调用 `handleFinish()` 导向 `continueUrl` 或 `/competitions` 并完成 `resetFlow()`），次按钮为“返回选项”；
- **异常捕获与反馈**：
  - 引入 `useFormError`，将 `@error` 产生的错误文本在主页面上予以清晰呈现；
  - 移除原先冗余的内部预取 `prefetchCreationOptions` 及所有底层 ArrayBuffer 序列化函数。

### 3.2 改造前后代码量与架构收益

- **代码量减少约 65%**：从 301 行直降至 110 行紧凑高自解释代码；
- **消除全链路双重维护隐患**：统一收敛于 `@/components/security/PasskeyModal.vue` 与 `passkey.vue`；
- **保持极致纯净规范**：严格恪守全代码零注释铁律，文档外置归档于本方案中。

---

## 4. 实施改造完成记录

### 4.1 左侧卡片标题与副标题精准更新
在 [`passkeyProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/passkeyProtectView.vue) 中通过 `useCardHeader` 动态设置标题：
- **未绑定状态（`!passkeyRegistered`）**：
  - 标题：`t('auth.signup.passkey_protect_title')` -> “创建设备安全密钥”（彻底替换了旧版 `auth.signin.passkey_method_name` 导致的 “FIDO2 / WebAuthn 通行密钥”）
  - 副标题：`t('auth.signup.passkey_protect_desc')` -> “设备密钥保存在您的个人设备中，可实现快速登录 Volur 用户。”
- **绑定成功状态（`passkeyRegistered`）**：
  - 标题：`t('auth.signup.passkey_registered_title')` -> “通行密钥已成功绑定”
  - 副标题：`t('auth.signup.passkey_registered_desc')` -> “已成功为此设备注册通行密钥，下次登录时可直接使用指纹、面容或设备锁快速进入。”

### 4.2 PasskeyModal 按钮右对齐排版修复
- 针对 [`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 底部按钮左对齐的问题，将其底部容器 `.passkey-modal-footer` 样式重构为：
  ```scss
  .passkey-modal-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    width: 100%;
  }
  ```
- 严格遵循 Google 对话框与平台 `AuthActionBar` 的统一排版规范，实现取消、其他设备与主操作按钮在右侧紧凑排列。

### 4.3 唤起模态框即时响应与 isSubmitting 状态联动
- 在点击“创建通行密钥”触发 `handleOpenPasskeyModal` 时，显式维护 `isSubmitting = true`，使操作按钮在触发 `triggerScrim` 延迟过渡期间立即进入 `:loading` 状态，给予用户明确的点击即时反馈，随后平滑呈现弹窗。

### 4.4 右侧图标删除与文案重构
- 删除了右侧原本蓝色的巨大圆形装饰 SVG 图标（`w-16 h-16 rounded-full`）；
- 删除了原本内嵌的 H2 标题“使用设备生物识别或硬件密钥”以及冗长文案“通行密钥保存在您的个人设备中，具有独一无二的密码学防伪特性，能够有效防御钓鱼网站与凭据泄露。”；
- 重构为参考 `passkeyChallengeView.vue` 的结构化账号与引导信息卡：
  - **关联账号**：展示待注册账号（`signupFlowStore.username || signupFlowStore.email`）；
  - **密钥类型**：通行密钥（指纹 / 面容 / 屏幕锁）；
  - **引导说明**：`t('auth.signup.passkey_guide_text')`（“点击下方按钮以配置设备密钥。后续登录时，无需输入密码即可直接在此设备上安全进入 Volur。”）；
  - **错误提示**：通过 `useFormError` 动态承接并展示来自 WebAuthn 或接口的报错信息。

### 4.5 多语言国际化（i18n）双重键名兼容与覆盖
同步更新并补全了三大语言包文件：
1. [`zh-CN/auth/signup.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/auth/signup.json)
2. [`en-US/auth/signup.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/en-US/auth/signup.json)
3. [`zh-TW/auth/signup.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-TW/auth/signup.json)

确保 `passkey_protect_title`、`passkey_protect_desc` 与 `passkey_create_title`、`passkey_create_desc` 均具备规范的多语言值，彻底消除 i18n 丢失 key 原样输出的问题。

### 4.6 PasskeyModal 主按钮标签文案精简
- 将 [`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 中在 `create` 模式下主操作按钮文案 `passkey_modal_create_btn` 从原本的“创建安全密钥”精炼修改为“创建”（英文：`Create`，繁体中文：`建立`），使模态框在紧凑右对齐排版下更加精致简洁。

### 4.7 代码质量与构建测试
- **纯粹代码与零注释**：组件与配置文件中无任何行内注释；
- **编译构建验证**：执行 `npm --prefix src/volur-lite-view-NEXT run build`（`vue-tsc --noEmit && vite build`），2915 个模块编译通过，0 错误，打包耗时 6.25s。
