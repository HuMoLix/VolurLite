# 施工与审计记录：前台主布局管理员面板入口颜色规范化对齐

## 1. 基本信息

- **任务名称**：前台主布局 (`AppLayout.vue`) 管理员面板入口色彩规范化改造
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-lite-view` (前台通用主布局)
- **关联文件**：
  - [`src/volur-lite-view/src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)

---

## 2. 变更背景与需求

此前在 2026-08-27 的侧边栏入口重构中，为区分前台普通业务菜单与管理员入口，底部【管理员面板】快捷按钮使用了琥珀橙色（`#f59e0b` / `rgba(245, 158, 11, 0.08)`）。

为了保持 Volur Lite 极简设计规范（黑白灰基底 + 统一的主题设计令牌），现将【管理员面板】入口样式从琥珀橙色调整为与平台主色彩体系对齐，并支持日间与夜间模式自适应。

---

## 3. 具体改动内容

### 3.1 [`AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue) 样式重构
- **移除硬编码琥珀色**：彻底移除 `color: #f59e0b;` 及 `background-color: rgba(245, 158, 11, 0.08);`；
- **对齐平台主题文本与悬浮状态**：
  - 默认文字颜色调整为 `var(--v-text-desc, #444444)`，字重调整为 `500`（与导航及后台返回按钮标准统一）；
  - Hover 悬浮态文字颜色切换至 `var(--v-text-primary, #111111)`；
  - 亮色模式悬浮底色采用轻量 `rgba(0, 0, 0, 0.04)`；
  - 引入 `@include theme-dark`，在暗黑模式下悬停底色自动切换为 `rgba(255, 255, 255, 0.08)`；
- 同时在展开态 (`.admin-panel-btn-expanded`) 与收缩态 (`.admin-panel-btn-collapsed`) 应用该规范。

---

## 4. 验证结论

- **前端打包编译**：执行 `npm run build` (`vue-tsc --noEmit && vite build`) 构建通过（耗时 8.08s），零类型与编译错误；
- **视觉体验**：前台侧边栏底部管理员入口与菜单整体风格浑然一体，在亮色/暗色模式下均呈现一致的视觉与交互反馈。
