# CSS Token 与 color-scheme (light-dark) 规范实现记录

## 变更背景
为了对齐 Google Material Design 3、Google Workspace 与 Google Gemini/AI Studio 生产环境的前端主题色彩设计系统，并消除传统深浅色模式在多处重复声明选择器与变量的样板代码，引入基于 CSS Color Module Level 5 标准规范的 `light-dark()` 函数及 `color-scheme` 驱动机制，构建平台级统一 CSS Token 规范文件 `vcss.scss`。

## Google 前端 `:root .light-theme` 与 `:root .dark-theme` 架构机制解析
在 Google 的前端实现中，`:root .light-theme` 与 `:root .dark-theme` 拥有完全一致的 CSS 变量赋值，核心设计在于：
1. **单一真实源 (Single Source of Truth)**：`light-dark(lightVal, darkVal)` 函数天然封装了两个主题下的值。变量的定义本身不需要区分暗色或亮色，而是将分发判定交由浏览器的 `color-scheme` 上下文进行求值。
2. **选择器穿透与作用域岛屿 (Theme Scoping & Subtree Islands)**：
   - 当在根节点或任意容器挂载 `.light-theme` 时，配合声明 `color-scheme: light;`，该分支下的全部组件无论嵌套多深，均会自动计算亮色值；
   - 当挂载 `.dark-theme` 时，配合声明 `color-scheme: dark;`，全部组件自动计算暗色值；
   - 可以在深色页面中随时嵌套局部亮色预览容器（如 Markdown 文档预览），无需对具体 CSS 属性进行任何重写。
3. **选择器容错与全覆盖**：声明 `:root, :root .light-theme, :root .dark-theme, .light-theme, .dark-theme`，确保无论是全局默认环境、系统主题偏好驱动，还是通过主题 class / `data-theme` 受控，Token 均全局可用。

## 改动点
1. **完整收敛 Google V3 Tokens 至 `src/styles/vcss.scss`**：
   - 包含完整的 Google V3 表面阶梯（`--color-v3-surface`、`--color-v3-surface-container*`、`--color-v3-surface-left-nav*` 等）；
   - 按钮容器与状态文字 Token（`--color-v3-button-container*`、`--color-v3-text-on-button*` 等）；
   - 文本与排版阶梯（`--color-v3-text`、`--color-v3-text-var`、`--color-v3-text-disable`、`--color-v3-text-link` 等）；
   - 边框与外轮廓（`--color-v3-outline*`、`--color-v3-chat-separator` 等）；
   - 语义辅助色阶梯（`--color-v3-error-*`、`--color-v3-warning-*`、`--color-v3-accent-1~7` 等）；
   - 阶梯投影系统（`--v3-shadow-xs~xl`、`--v3-shadow-dropdown`、`--color-v3-shadow-card*` 等）；
   - 完整 Markdown / Docs 渲染系统（`--docs-card-*`、`--docs-table-*`、`--color-markdown-alert-*` 等）；
   - 搜索与浮层交互 Token（`--color-search-entry-point-*` 等）；
   - 平台现有 `--v-*` 系列变量平滑映射至该系统，确保已有业务组件完全无缝兼容。
   - 严格遵守纯粹代码与零注释铁律，代码文件内部不保留任何行内解释性注释。
2. **样式接入与构建验证**：
   - `src/styles/index.scss` 通过 `@use './vcss.scss';` 统一导入；
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全流程编译与打包 100% 通过无任何错误。

## 影响范围
- `src/styles/vcss.scss`
- `src/styles/index.scss`
- 全局基于 `--v-*`、`--color-v3-*`、`--docs-*` 以及 `light-dark()` 的所有视图组件。
