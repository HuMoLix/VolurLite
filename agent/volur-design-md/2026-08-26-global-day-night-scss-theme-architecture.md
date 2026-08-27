# 全局日间与夜间模式 SCSS 主题体系施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/styles/theme.scss`、`src/volur-lite-view/src/styles/mixins.scss`、`src/volur-lite-view/src/styles/index.scss`、`src/volur-lite-view/src/store/modules/app.ts`、`agent/volur-design-skills/view-Rules.md`  
**责任主体**：Antigravity AI Agent

---

## 1. 架构目标与设计体系

为 Volur Lite 前端建立现代化、零闪烁、高性能的全局双模态（日间模式 Light / 夜间模式 Dark）SCSS 主题架构：
1. **CSS 自定义属性设计令牌 (Design Tokens)**：
   - 提取背景色系（`--v-bg-base`, `--v-bg-card`, `--v-bg-header`, `--v-bg-code`）；
   - 提取排版文字色系（`--v-text-primary: #111/#f1f5f9`, `--v-text-desc: #444/#9ca3af`, `--v-text-muted: #666/#6b7280`）；
   - 提取主要按钮色系（`--v-btn-primary-bg: #000/#fff`, `--v-btn-primary-text: #fff/#000`）；
   - 提取边框与阴影体系（`--v-border-base`, `--v-shadow-sm`, `--v-shadow-md`）。
2. **主题响应式 Mixins 工具库**：
   - `@mixin theme-dark` 与 `@mixin theme-light`；
   - `@mixin v-card`、`@mixin v-primary-button`、`@mixin v-text-desc`。
3. **DOM 与 Pinia 状态双向同步**：
   - `useAppStore` 监听主题状态，自动为 `<html>` 挂载 `class="dark"` 与 `data-theme="light|dark"` 属性。

---

## 2. 核心改动点与实施细节

### 2.1 全局主题文件构建 ([`src/styles/theme.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/styles/theme.scss))
- 完整定义 `:root, [data-theme='light']` 与 `html.dark, [data-theme='dark']` 变量映射。

### 2.2 工具混入扩展 ([`src/styles/mixins.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/styles/mixins.scss))
- 新增 `theme-dark`、`theme-light`、`v-card`、`v-primary-button`、`v-text-desc` 等开箱即用主题混入。

### 2.3 根样式接入与持久化 ([`src/styles/index.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/styles/index.scss) & [`src/store/modules/app.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/app.ts))
- 在 `index.scss` 中引入 `theme.scss` 并配置 body 全局平滑过渡；
- 在 `useAppStore` 中配置 `applyThemeToDom` 确保 DOM 类名与 `localStorage` 同步。

### 2.4 规范文档更新 ([`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md))
- 在 `view-Rules.md` 第 10 节补充了完整的双模态设计令牌对照矩阵表与 Mixins 编写示范。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~5.21s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/styles/theme.scss`
  - `src/volur-lite-view/src/styles/mixins.scss`
  - `src/volur-lite-view/src/styles/index.scss`
  - `src/volur-lite-view/src/store/modules/app.ts`
  - `agent/volur-design-skills/view-Rules.md`
- **体系稳定性**：
  - 各组件无论是通过 Naive UI `themeOverrides`、SCSS Mixins 还是直接使用 CSS 变量，均能实现平滑无缝的主题切换。
