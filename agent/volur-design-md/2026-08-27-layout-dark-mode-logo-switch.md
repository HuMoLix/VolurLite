# 全局与认证布局暗夜模式 Logo 响应式切换审计记录

## 1. 变更背景与动因
在平台端前台布局（`AppLayout.vue`）、管理端后台布局（`AdminLayout.vue`）以及认证登录/注册布局（`AuthLayout.vue`）中，此前固定使用了亮色 Logo。
当系统切换至暗夜/暗色模式（`appStore.theme === 'dark'`）时，Logo 颜色未随之自适应为暗色系资源（`long-logo-dark.svg` 与 `favicon-dark.svg`），影响视觉一致性。

---

## 2. 改动点详述

### 2.1 认证布局 Logo 暗夜模式适配（`src/layout/AuthLayout.vue`）
- 引入 `computed` 动态计算 `logoUrl`：
  - 暗夜模式：优先读取系统配置 `PLATFORM_LOGO_DARK`，缺省为 `/long-logo-dark.svg`；
  - 亮色模式：读取系统配置 `PLATFORM_LOGO`，缺省为 `/long-logo.svg`；
- 顶部 Logo 图片绑定 `:src="logoUrl"`。

### 2.2 平台与管理端侧边栏 Logo 暗夜模式适配（`AppLayout.vue` / `AdminLayout.vue`）
- 展开状态：动态绑定 `:src="appStore.theme === 'dark' ? '/long-logo-dark.svg' : '/long-logo.svg'"`；
- 折叠状态：动态绑定 `:src="appStore.theme === 'dark' ? '/favicon-dark.svg' : '/favicon.svg'"`；
- 动态 Transition Key 驱动平滑过渡动画。

---

## 3. 影响范围与回归验证
- **受影响文件**：
  - [`src/volur-lite-view/src/layout/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue)
  - [`src/volur-lite-view/src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)
  - [`src/volur-lite-view/src/layout/AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)
- **编译与构建验证**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 错误，打包构建成功通过。
