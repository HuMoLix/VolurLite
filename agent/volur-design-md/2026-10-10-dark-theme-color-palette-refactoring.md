# 系统暗色模式配色调整施工记录文档

本文档记录针对 Volur Lite NEXT 前端工程（`src/volur-lite-view-NEXT`）暗色模式（Dark Mode）配色体系的重构与视觉精细化调整。

---

## 1. 变更背景与需求分析

根据视觉规范演进及交互对比度优化需求，系统暗色模式（Dark Mode）对整体背景色、认证布局容器、登录框体卡片、核心正文文字及底栏辅助说明文本进行了系统化重构对齐：
1. **系统基底与认证布局背景**：调整为 Google Material 3 沉浸式深暗色容器基底 `#1e1f20`（消除刺眼死黑或过浅灰阶，构建统一背景基线）；
2. **认证登录框（`AuthLayout.vue` 卡片）**：在桌面端（`min-width: 840px`）明确采用 `#0e0e0e` 深黑独立卡片表面，与 `#1e1f20` 底色形成清晰的分层表面高度（Elevation Hierarchy）；
3. **登录框核心文字色彩**：标题、正文及输入框文字在暗色模式下统一设定为 `#e3e3e3`，达到约 15:1 的顶级高对比度无障碍阅读标准（WCAG AAA）；
4. **登录框底栏辅助文字（语言选择、帮助、隐私、条款、版权与备案）**：统一调整为 Google Dark 高亮辅助文字颜色 `#e8eaed`，确保外围辅助功能在 `#1e1f20` 背景上保持清晰辨识度。

---

## 2. 改动点清单与实现细节

### 2.1 全局 SCSS 变量与设计系统基线 (`src/styles/variables.scss` & `src/styles/index.scss`)
- **`$bg-color-dark`**：由原 `#202124` 统一更新为 `#1e1f20`；
- **`$bg-color-dark-card`**：由原 `#292a2d` 统一更新为 `#0e0e0e`；
- **`$text-color-dark-primary`**：由原 `#e8eaed` 更新为核心正文色 `#e3e3e3`；
- **CSS Custom Properties (`index.scss`)**：
  - `--v-bg-base`: `#1e1f20`；
  - `--v-bg-card`: `#0e0e0e`；
  - `--v-bg-surface`: `#1e1f20`；
  - `--sys-color-surface-container`: `#1e1f20`；
  - `--v-text-primary`: `#e3e3e3`；
  - 底栏链接 hover 态保持 `#e8eaed`。

### 2.2 Tailwind CSS 配置同步 (`tailwind.config.ts`)
- **`colors.google.surface.dark`**：映射至 `#1e1f20`；
- **`colors.google.surface.darkCard`**：映射至 `#0e0e0e`；
- **`colors.google.text.darkPrimary`**：映射至 `#e3e3e3`。

### 2.3 Naive UI 全局主题覆盖 (`src/App.vue`)
- **`common.baseColor` & `common.bodyColor`**：暗色模式更新为 `#1e1f20`；
- **`common.cardColor` & `Card.color`**：暗色模式更新为 `#0e0e0e`；
- **`common.textColorBase` & `common.textColor1` & `Card.titleTextColor`**：暗色模式更新为 `#e3e3e3`；
- **`Layout.color`**：暗色模式更新为 `#1e1f20`。

### 2.4 认证布局架构组件 (`src/components/layouts/auth/AuthLayout.vue`)
- **底栏文本样式 (`auth-footer`)**：
  - 语言切换按钮、下拉箭头 SVG、元信息链接（帮助/隐私/条款/备案）及版权信息文本，在暗色模式下统一应用 `dark:text-[#e8eaed]`；
  - 在 SCSS `:global(html.dark) .auth-footer` 中显式指定 `color: #e8eaed` 以及下属链接与 SVG 的着色规则。
- **登录卡片表面 (`auth-card`)**：
  - `:global(html.dark) .auth-card` 在桌面端（`@media (min-width: 840px)`）强制配置 `background-color: #0e0e0e !important;` 与柔和暗黑阴影 `box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4) !important;`；
  - 卡片内文本与标题 `auth-card__title` 在暗色模式下应用 `color: #e3e3e3;`；
  - 加载遮罩阻塞层 `auth-card__blocker` 在暗色模式下背景调整为 `rgba(14, 14, 14, 0.6)`，与 `#0e0e0e` 卡片底色完全契合；
  - 全屏加载遮罩 `auth-scrim` 在暗色模式下背景调整为 `rgba(30, 31, 32, 0.6)`，与 `#1e1f20` 背景无缝契合。

### 2.5 文本输入组件联动 (`src/components/google/GoogleTextField.vue` & `buttons.scss`)
- **`GoogleTextField.vue`**：
  - 暗色模式下输入文本默认 fallback 更新为 `var(--v-text-primary, #e3e3e3)`；
  - 浮动标签背景遮罩在暗色桌面端更新为 `var(--v-bg-card, #0e0e0e)`，完美融入卡片背景；
  - 移动端浮动标签背景在暗色模式下更新为 `#1e1f20`；
- **`buttons.scss`**：
  - 账号选择 Chip 标签 `.auth-account-chip__label` 暗色 fallback 同步为 `#e3e3e3`。

### 2.6 规范基线同步 (`agent/volur-design-skills/view-NEXT-rules.md`)
- 更新暗色模式（Google Dark Workspace）色值表，将基底背景、卡片表面、主要文本及底栏辅助文字明确记录于规范中。

---

## 3. 影响范围与验证

- **工程构建测试**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），4259 个模块顺利完成构建，零错误零告警；
- **无障碍（A11y）色彩对比度**：
  - 卡片底色 `#0e0e0e` 搭配文字 `#e3e3e3`，对比度约 15:1（远超 WCAG AAA 7:1 规范）；
  - 背景底色 `#1e1f20` 搭配底栏文字 `#e8eaed`，对比度约 13:1（远超 WCAG AAA 7:1 规范）；
- **代码整洁度**：遵循纯粹代码与零注释原则，所有代码修改均不包含解释性行内注释。
