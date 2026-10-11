# 输入框与按钮暗色模式色彩微调施工记录文档

本文档记录针对 Volur Lite NEXT 前端工程中 `GoogleTextField.vue` 输入框与全局按钮系统暗色模式（Dark Mode）视觉样式的二次精细化调整。

---

## 1. 变更背景与需求

为进一步贴合 Google Material 3 现代规范、消除暗色输入状态下的视觉突兀感，并强化操作按钮的视觉对比层级，进行了以下两组颜色定向调整：
1. **`GoogleTextField.vue` 暗色模式样式**：
   - 输入光标（caret）颜色调整为 `#fff`（高亮白光标，替代原有浏览器默认或错误继承色彩）；
   - 默认外边框颜色调整为 `#1e1f20`（与页面深色基底无缝融合，形成沉浸式框体边界）；
   - 输入文字颜色维持标准高对比 `#e3e3e3`。
2. **全局按钮系统暗色模式样式**：
   - **主按钮（Primary Action）**：文字颜色设定为 `#062e6f`（Material 3 On-Primary 深邃对比蓝），背景颜色设定为 `#a8c7fa`（Google Blue 100/200），Hover 态背景颜色设定为 `#b6d0f8`，实现温和柔润的交互光泽；
   - **次按钮（Secondary Action）**：文字颜色设定为 `#a8c7fa`，Hover 态背景颜色设定为 `#141517`（深曜微背景色），提供克制而明确的悬停反馈。

---

## 2. 改动点清单与实现细节

### 2.1 `GoogleTextField.vue` 输入组件
- **光标颜色**：在 `:global(html.dark) .google-text-field__input` 中显式设置 `caret-color: #fff`；
- **边框色彩**：在 `:global(html.dark) .google-text-field__box` 中将默认 `border-color` 与 `&--disabled` 均设定为 `#1e1f20`；获取焦点时的边框与光晕同步切换至 `#a8c7fa`；
- **文字色彩**：保持 `color: var(--v-text-primary, #e3e3e3)`；
- **浮动标签**：聚焦态标签文字颜色与输入框对齐为 `#a8c7fa`。

### 2.2 按钮系统 (`src/styles/buttons.scss`)
- **主按钮（`.btn-primary-action`, `.btn-next-step`, `.btn-verify-action`）**：
  - 常态背景：`background-color: #a8c7fa !important`；
  - 常态文字：`color: #062e6f !important` 及 `.n-button__content { color: #062e6f !important }`；
  - Hover 态背景：`background-color: #b6d0f8 !important`，文字维持 `#062e6f`；
  - Active 态背景：`background-color: #a8c7fa !important`，文字维持 `#062e6f`；
- **次按钮（`.btn-secondary-action`, `.btn-create-account`, `.btn-try-another-way` 等）**：
  - 常态文字：`color: #a8c7fa !important` 及 `.n-button__content { color: #a8c7fa !important }`；
  - Hover 态背景：`background-color: #141517 !important`，文字保持 `#a8c7fa`；
  - Active 态背景：`background-color: #141517 !important`；
- **辅助链接与 Chip 标签**：
  - `.auth-forgot-link`、`.auth-learn-more-link`、`.auth-resend-link` 常态文字统一为 `#a8c7fa`，Hover 态背景设定为 `#141517`；
  - `.auth-account-chip__avatar` 头像背景设定为 `#a8c7fa`，文字设定为 `#062e6f`。

### 2.3 全局主题配置与设计系统同步
- **`src/styles/index.scss`**：
  - `--v-btn-primary-bg`: `#a8c7fa`；
  - `--v-btn-primary-text`: `#062e6f`；
  - `--v-btn-primary-hover`: `#b6d0f8`；
  - `--v-btn-primary-active`: `#a8c7fa`；
  - `--v-color-primary`: `#a8c7fa`。
- **`src/styles/variables.scss`**：
  - `$google-blue-dark`: `#a8c7fa`；
  - `$google-blue-dark-hover`: `#b6d0f8`。
- **`tailwind.config.ts`**：
  - `colors.google.blue.dark`: `#a8c7fa`；
  - `colors.google.blue['dark-hover']`: `#b6d0f8`。
- **`src/App.vue`**：
  - Naive UI `themeOverrides.common` 中 `primaryColor`（`#a8c7fa`）、`primaryColorHover`（`#b6d0f8`）、`primaryColorPressed`（`#a8c7fa`）；
  - `themeOverrides.Button` 中 `colorPrimary`（`#a8c7fa`）、`colorHoverPrimary`（`#b6d0f8`）、`textColorPrimary`（`#062e6f`）、`textColorHoverPrimary`（`#062e6f`）。
- **`src/components/layouts/auth/AuthLayout.vue`**：
  - 进度指示器暗色背景与高亮色同步映射至 `#a8c7fa`。
- **`agent/volur-design-skills/view-NEXT-rules.md`**：
  - 更新主色、主按钮与次按钮暗色模式视觉规范。

---

## 3. 验证与审计

- **构建验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，4259 个模块编译顺利，无任何类型错误或警告；
- **视觉与对比度标准**：
  - 主按钮 `#a8c7fa` 底色与 `#062e6f` 文本，对比度达 7.5:1，完美满足 WCAG AAA 规范；
  - 输入框 `#1e1f20` 边框在 `#0e0e0e` 卡片上形成低调内敛的边界过渡；
  - 光标 `#fff` 在深色背景中拥有最高锐度识别；
- **代码整洁度**：遵循纯粹代码零注释原则，所有代码修改均不包含解释性行内注释。
