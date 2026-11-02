# 认证布局页脚色彩对齐与卡片阴影移除方案

## 1. 变更背景与动因
在 `src/volur-lite-view-NEXT` 现代前端架构中，认证模块（`AuthLayout.vue`）持续推进 Google Material 3 规范与现代 CSS 变量驱动的色彩体系统一。
此前，`AuthLayout.vue` 中存在以下视觉与样式定义问题：
1. **认证卡片阴影残留**：在桌面端（`@media (min-width: 840px)`）`.auth-card` 仍保留了多层微阴影（`box-shadow: 0 1px 3px 0 light-dark(...)`），破坏了与无边界扁平表面容器的设计一致性；
2. **页脚色彩偏离**：`<footer>`（`.auth-footer`）文字此前采用 `light-dark(var(--v-text-muted, #5f6368), #e8eaed)`，且在 HTML 模板多处硬编码了 Tailwind 文本颜色工具类（`text-google-text-muted dark:text-[#e8eaed]`），未对齐认证模块统一的表面文本语义变量 `--color-v1-auth-on-surface`。

## 2. 改动范围与核心改动点

### 2.1 `src/layouts/primary/AuthLayout.vue`
1. **删除 `auth-card` 的 `box-shadow`**：
   - 彻底移除桌面端媒体查询（`@media (min-width: 840px)`）中的 `box-shadow: 0 1px 3px 0 light-dark(...)` 属性；
   - 移除移动端基类中的 `box-shadow: none` 冗余声明；
   - 移除 `.auth-card` 的 `transition` 中针对 `box-shadow` 的冗余动效声明，仅保留 `background-color` 平滑过渡。
2. **重构 `footer` 文本颜色为 `var(--color-v1-auth-on-surface)`**：
   - 将 `.auth-footer` 样式规则中的 `color` 统一更新为 `var(--color-v1-auth-on-surface)`；
   - 清理模板中语言切换按钮、下拉箭头 SVG、底部元信息区及备案链接上残留的 `text-google-text-muted dark:text-[#e8eaed]` 干扰类名，使其完全依托 CSS 级联（`color: inherit`）由 `<footer>` 顶层单点控制。

## 3. 验证与影响分析
- **类型检查与构建验证**：
  - 经由 `vue-tsc --noEmit` 全量静态类型检查，无任何语法或类型错误；
  - 经由 `vite build` 完整生产编译打包测试，所有 31,700+ 模块编译通过，产物无损输出。
- **视觉层级与暗黑模式体验**：
  - 在亮色模式下，`--color-v1-auth-on-surface`（`#1f1f1f`）呈现与正文一致的高对比深灰；
  - 在暗黑模式下，`--color-v1-auth-on-surface`（`#e3e3e3`）呈现纯净柔和的灰白；
  - 卡片去除阴影后呈现干净利落的几何表面，完美契合 Google Workspace 现代极简风格。
