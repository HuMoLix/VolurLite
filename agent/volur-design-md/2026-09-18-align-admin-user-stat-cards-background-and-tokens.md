# 对齐 AdminUserView 统计卡片背景与 VolurTableCard 主题令牌规范记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`  
**参考组件**：`src/volur-lite-view/src/public/volurEx/components/VolurTableCard.vue`、`src/volur-lite-view/src/public/volurEx/styles/theme.scss`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与现象分析

在后台管理界面的用户管理页面（`AdminUserView.vue`）中，顶部的数据指标统计卡片（包含“系统总用户”、“管理员数”、“正常启用”、“违规封禁”）的背景色理应与下方的主体表格卡片（`VolurTableCard.vue`）保持统一（亮色模式下为纯白 `#ffffff`）。但在实际渲染中发现统计卡片背景泛灰，未能与 `VolurTableCard` 视觉统一。

### 核心根因剖析 (Root Cause)
1. **CSS 变量级联继承污染**：
   - 页面由 `AdminLayout.vue` 承载，外层使用了 Naive UI 的 `<n-layout-content class="admin-content">` 组件。
   - Naive UI 的 `n-layout-content` 会在自身元素上注入主题 CSS 变量 `--n-color: #f8f9fa`（对应浅灰底色）。
   - 原 `AdminUserView.vue` 中的统计卡片 `.stat-mini-card` 错误使用了 `background-color: var(--n-color, #ffffff);`。由于 CSS 变量具有向下级联继承特性，DOM 树上级存在的 `--n-color`（即 `#f8f9fa`）被成功命中，导致后面的默认后备值 `#ffffff` 彻底失效，使卡片背景变灰并与页面底色融合。
2. **主题设计令牌未对齐**：
   - `VolurTableCard.vue` 严格采用 Volur 平台的全局设计系统主题变量 `var(--v-bg-card, #ffffff)`，在日间模式下精确为纯白色 `#ffffff`，在暗夜模式下自适应提升为深曜炭黑高层级表面色 `#151821`。
   - 卡片边框使用了写死的 `rgba(0, 0, 0, 0.06)`，在夜间模式下无法正常自适应，而 `VolurTableCard` 使用了自适应的 `var(--v-border-base, #e5e4e7)`。
   - 圆角与动效过渡亦存在细微差异（`8px` vs `10px`）。

---

## 2. 核心改动点说明

在 `src/volur-lite-view/src/views/admin/user/AdminUserView.vue` 的样式作用域中，将 `.stat-mini-card` 及其相关属性全面对齐 `VolurTableCard` 与 `volurEx` 主题令牌系统：

1. **背景颜色对齐**：
   - 将 `background-color: var(--n-color, #ffffff);` 调整为 `background-color: var(--v-bg-card, #ffffff);`，解除对 Naive UI 布局父级变量 `--n-color` 的错误继承，日间模式下呈现纯白背景，夜间模式下自适应为 `#151821`。
2. **边框体系与圆角对齐**：
   - 边框调整为 `border: 1px solid var(--v-border-base, #e5e4e7);`；
   - 悬浮边框调整为 `border-color: var(--v-border-focus, #bbbbbb);`；
   - 卡片圆角统一为 `10px`（对齐 `VolurTableCard` 的 `border-radius: 10px`）；
   - 阴影统一使用 `var(--v-shadow-sm)` 与 `var(--v-shadow-md)`。
3. **文本颜色规范对齐**：
   - 将标签说明文字颜色由固定 `#444444` 调整为 `var(--v-text-desc, #444444)`，满足黑夜模式下的低眩光高对比度排版规范。

---

## 3. 影响范围与验证结果

- **影响范围**：仅作用于 `AdminUserView.vue` 顶部的四个统计卡片视觉样式，业务逻辑、响应式布局、交互状态均未受破坏。
- **构建测试**：
  在 `src/volur-lite-view` 目录下执行 `npm run build`，编译顺利通过（0 错误，打包耗时 5.47s）。
