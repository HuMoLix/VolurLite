# Passkey 安全核验重构与 PasskeyModal 模态组件实现

## 1. 变更背景与需求概述

在 `/v1/signin/challenge/pk`（通行密钥挑战页面）中，原先右侧表单上方展示了静态提示条（“使用通行密钥验证：使用您的指纹、面容、屏幕锁定或 USB 硬件安全密钥确认您的身份。”）。同时，点击“验证”按钮后直接调用了原生的 WebAuthn 浏览器接口，缺乏统一的模态交互引导。

为对齐平台安全核验与 Google Material Design 交互规范，本次调整包含以下内容：
1. 移除 `passkeyChallengeView.vue` 右侧静态提示横幅，简化视图结构；
2. 在安全机制组件目录 `src/volur-lite-view-NEXT/src/components/security/` 中实现无头逻辑引擎 `passkey.vue`，集中封装通行密钥的创建/保存与断言/验证核心逻辑；
3. 构建 `PasskeyModal.vue` 卡片模态框组件，采用 `#e9eef6`（暗色模式自适应 `#282a2d`）背景与圆角卡片布局，包含大标题、Google 官方 SVG 插图、介绍文案以及靠右对齐的操作按钮（“取消”、“选择其他设备”、“创建安全密钥”）；
4. 在 `passkeyChallengeView.vue` 中点击“验证”按钮时唤起 `PasskeyModal.vue`，由模态组件调用 `passkey.vue` 执行 WebAuthn 验证流并推进状态机。

---

## 2. 核心改动点

### 2.1 本地静态资源归档
- 从 Google 官方凭据资源服务下载 SVG 插图并持久化保存至 `src/volur-lite-view-NEXT/public/static/svg/passkeys_scene_add_account.svg`。

### 2.2 无头逻辑组件 `passkey.vue`
- 位置：`src/volur-lite-view-NEXT/src/components/security/passkey.vue`
- 职责：
  - 封装 ArrayBuffer 与 Base64URL 相互转换算法（`bufferToBase64Url`、`base64UrlToBuffer`、`copyToPureArrayBuffer`）；
  - 运行时环境探针（`window.PublicKeyCredential`、`navigator.credentials`、IP 地址边界校验）；
  - `createPasskey`：拉取 `/captcha/challenge/pk` 创建选项，组装 `CredentialCreationOptions` 并调用 `navigator.credentials.create`，提交至 `/captcha/challenge/pk` 注册接口；
  - `verifyPasskey`：拉取 `/captcha/challenge/pk/assertion-options` 断言选项，组装 `CredentialRequestOptions` 并调用 `navigator.credentials.get`，调用 `signinFlowStore.submitChallenge('pk', ...)` 校验；
  - 对外通过 `defineExpose` 导出 `createPasskey`、`savePasskey`、`verifyPasskey`、`isSupported`、`loading`、`error`，同时支持插槽和事件传递。

### 2.3 模态交互组件 `PasskeyModal.vue`
- 位置：`src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue`
- 视觉与交互规范：
  - 容器样式：`<teleport to="body">` 遮罩层配合居中卡片，背景色 `#e9eef6`，暗色模式切换为 `#282a2d`，`border-radius: 28px`，`box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.18)`；
  - 顶部（Header）：标题 `Create a passkey for your Volur Account`（24px / 500 字重）；
  - 中部（Body）：居中展示 `/static/svg/passkeys_scene_add_account.svg`，正文显示 `Create a passkey to start signing in with just your fingerprint, face, or screen lock. You can create a passkey on this device or use another device, like a hardware security key.`，下方承载错误提示；
  - 底部（Footer）：右对齐按钮组，包含“取消”与“选择其他设备”（透明次级文字按钮）、“创建安全密钥”（Google 蓝 `#0b57d0` 主要操作按钮）；
  - 逻辑委派：内置 `<Passkey ref="passkeyRef" />`，点击按钮分别分发至 `cross-platform` 与 `platform` 认证模式。

### 2.4 安全组件根导出 `index.ts`
- 位置：`src/volur-lite-view-NEXT/src/components/security/index.ts`
- 集中导出 `Passkey` 与 `PasskeyModal`。

### 2.5 视图集成 `passkeyChallengeView.vue`
- 位置：`src/volur-lite-view-NEXT/src/views/auth/signin/challenge/passkeyChallengeView.vue`
- 变更：
  - 移除右侧重复的 `auth-challenge-banner` 区域；
  - 点击“验证”按钮触发 `handleOpenPasskeyModal`，弹出 `PasskeyModal`；
  - 监听模态框的 `@success` 与 `@error` 事件，根据服务端响应完成后续页面跳转或错误展示；
  - 消除视图内冗余的 Base64 转换与底层 WebAuthn 调用。

---

## 3. 安全与架构影响分析
- **单一职责与高内聚**：将底层 WebAuthn 编解码与 API 请求从视图层剥离至 `passkey.vue`，实现挑战与注册场景的逻辑复用；
- **纯粹代码与零注释**：所有涉及的代码文件均严格执行无注释铁律；
- **构建验证**：前端工程通过 `vue-tsc --noEmit && vite build`，零 TypeScript 类型报错。
