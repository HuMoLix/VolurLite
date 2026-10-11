# 暗色模式遮罩层（Mask / Scrim / Blocker）色彩优化施工记录文档

本文档记录针对 Volur Lite NEXT 前端工程中各类暗色模式遮罩层（全屏流转遮罩、卡片阻塞遮罩、多语言加载遮罩及模态弹窗遮罩）色彩方案的系统化统一。

---

## 1. 变更背景与需求

在暗色模式（Dark Mode）下，原各类遮罩层背景色散落在 `rgba(32, 33, 36, 0.6)`、`rgba(30, 31, 32, 0.6)`、`rgba(40, 42, 45, 0.6)` 等多种偏灰色彩中，视觉沉浸感不足且不同组件间透明度与色相不统一。
根据用户确认的最新规范，全系统暗色模式遮罩层统一采用标准高沉浸半透明纯黑：
- **目标色彩**：`rgba(0, 0, 0, 0.6)`（通用标准沉浸式黑色遮罩）。

---

## 2. 改动点清单与实现细节

### 2.1 认证主布局遮罩 (`src/components/layouts/auth/AuthLayout.vue`)
- **全屏流转遮罩 (`.auth-scrim`)**：
  - 由原先的 `rgba(30, 31, 32, 0.6)` 更新为 `rgba(0, 0, 0, 0.6)`，确保在认证步骤跳转或异步流转触发全屏 Scrim 时背景深度压暗。
- **卡片级阻塞遮罩 (`.auth-card__blocker`)**：
  - 由原先的 `rgba(14, 14, 14, 0.6)` 统一为 `rgba(0, 0, 0, 0.6)`。

### 2.2 全局多语言切换遮罩 (`src/App.vue`)
- **全局语言切换加载遮罩 (`.global-lang-loading-mask`)**：
  - 由 `rgba(32, 33, 36, 0.75)` 统一为 `rgba(0, 0, 0, 0.6)`。
- **Naive UI 全局 Modal 遮罩 (`themeOverrides.Modal`)**：
  - 在 `App.vue` 的 `themeOverrides` 中显式注入 `Modal.maskColor`：暗色模式为 `rgba(0, 0, 0, 0.6)`，亮色模式为 `rgba(0, 0, 0, 0.4)`。

### 2.3 通行密钥弹窗 (`src/components/security/PasskeyModal.vue`)
- **弹窗全局背景遮罩 (`.passkey-modal-overlay`)**：
  - 由 `rgba(32, 33, 36, 0.6)` 统一为 `rgba(0, 0, 0, 0.6)`。
- **弹窗卡片阻塞遮罩 (`.passkey-modal__blocker`)**：
  - 由 `rgba(40, 42, 45, 0.6)` 统一为 `rgba(0, 0, 0, 0.6)`。

### 2.4 人机验证弹窗 (`src/components/security/RecaptchaModal.vue`)
- **验证弹窗背景遮罩 (`.rc-modal-backdrop`)**：
  - 由 `rgba(0, 0, 0, 0.45)` 调整为统一标准的 `rgba(0, 0, 0, 0.6)`。

---

## 3. 验证与审计

- **构建验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），4259 个模块构建成功，类型安全且零报错；
- **视觉一致性**：全系统各级遮罩层在暗色模式下拥有完全统一的纯黑遮光度（`0.6` 不透明度）与微模糊滤镜效果，消除了跨组件色调撕裂；
- **代码整洁度**：遵循纯粹代码零注释原则，所有代码修改均不包含解释性行内注释。
