# 通用输入框组件 VInput 禁用（Disabled）状态体系完善施工记录

## 1. 变更背景与目标

在推进 `TeamFormModal.vue` 等表单视图全量迁移至 `VInput.vue` 之后，在非编辑态或特定表单项禁用场景下，输入框需具备符合 Google Material Design / Workspace 视觉规范的高保真禁用（disabled）表现：
1. 解决原禁用状态下文字未沉降变灰、光标与交互手势未阻断的体验缺陷；
2. 消除禁用状态下悬停（hover）与聚焦（focus）意外触发边框激活高亮的反模式；
3. 补齐亮色模式与暗夜模式下的专用禁用底色、边框色、占位符色与字数统计（word-count）色彩阶梯。

---

## 2. 核心改动点

### 2.1 Naive UI 主题覆盖层 (inputThemeOverrides) 补齐
在 `VInput.vue` 的 `inputThemeOverrides` 计算属性中，完整注入禁用状态的主题变量：
- `borderDisabled`: 亮色 `1px solid #d7d8da80`，暗夜 `1px solid #262626`；
- `colorDisabled`: 亮色沉降底色 `#f5f6f7`，暗夜浅层低透明灰黑 `rgba(255, 255, 255, 0.04)`；
- `textColorDisabled`: 亮色 `#80868b`，暗夜 `#73777d`；
- `placeholderColorDisabled`: 亮色 `#bdc1c6`，暗夜 `#55595d`；
- `iconColorDisabled`: 亮色 `#9aa0a6`，暗夜 `#55595d`；
- `countTextColorDisabled`: 亮色 `#9aa0a6`，暗夜 `#55595d`。

### 2.2 Scoped SCSS 样式防护与状态隔离
- **状态选择器隔离**：
  - 将原 `&:hover` 约束为 `&:hover:not(.n-input--disabled)`；
  - 将原 `&.n-input--focus, &:focus-within` 约束为 `&.n-input--focus:not(.n-input--disabled), &:focus-within:not(.n-input--disabled)`；
  - 彻底杜绝输入框处于禁用态时，鼠标悬停或内部聚焦引起边框颜色激活跳变。
- **显式 `.n-input--disabled` 强化样式**：
  - 容器及内部 `.n-input-wrapper`、`.n-input__input-el`、`.n-input__textarea-el` 强制设置 `cursor: not-allowed !important`；
  - 隐藏状态边框层 `.n-input__state-border { display: none !important; }`；
  - 文本及占位符注入 `-webkit-text-fill-color` 属性，确保 iOS/Safari 及各主流浏览器均呈现稳定统一的禁用灰度；
  - 前缀及后缀容器（`.n-input__prefix`, `.n-input__suffix`）设置 `pointer-events: none`，阻断不可编辑状态下的任何误点击交互。
- **暗夜模式适配**：
  - 针对 `theme-dark` 及 `html.dark` 上下文，同步为 `.n-input--disabled` 注入高对比度暗黑背景色与 muted 灰色系阶梯。

### 2.3 业务组件 VSearch.vue 协同防护
- 当 `VSearch` 处于 `disabled` 状态时：
  - 点击前缀搜索图标与回车动作直接拦截阻断（快速返回，不触发 `@search` 与 `focus`）；
  - 前缀图标自动呈现 `cursor: not-allowed !important` 与 `pointer-events: none`。

---

## 3. 规范审计与验证

1. **纯粹代码与零注释原则**：所有源码文件（`VInput.vue`、`VSearch.vue`）严格贯彻无行内解释性注释；
2. **全端构建与类型验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全端 TypeScript 类型检查与生产打包一次性 0 错误通过；
3. **交互与视觉测试**：实测在亮色/暗夜模式下，输入框处于 disabled 时底色微灰、文字变浅、光标变为禁止符号、悬浮无边框颜色突变，完美契合 Google 极简设计语言。
