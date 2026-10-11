# AppLayout 与全局右侧滚动条 Google 现代流线型风格优化设计记录

## 1. 变更背景与问题分析

### 1.1 现状与视觉痛点
1. **右侧原生滚动条粗重突兀**：
   - 全局主布局 `AppLayout.vue` 采用 `min-h-screen flex` 双栏弹性流布局；
   - 左侧侧边栏 `SideBar.vue`（`VSideBar.vue`）已配置 `scrollbar-width: none`，滚动条隐形；
   - 右侧主体 `.app-main` 承载内容未限制局部高度与 overflow，超出视口高度时触发的是全局 `html / body` 视口级滚动条；
   - 在 Windows Chrome / Edge 渲染引擎下，默认滚动条宽度通常为 15px~17px，且带有灰色宽轨道与上下三角按钮，视觉粗粝硬直。
2. **暗色模式破坏沉浸感**：
   - 在暗色模式（Dark Mode，基底 `#1e1f20` / `#191919`）下，未显式声明 `color-scheme: dark` 与滚动条配色，导致右侧出现刺眼的浅亮色原生系统滑块与轨道，严重破坏 Google Workspace 暗曜设计规范。
3. **AppLayout 的 `scrollToTop` 方法语义缺失**：
   - 此前 `AppLayout.vue` 中的 `scrollToTop` 仅委托调用了左侧 `sideBarRef.value?.scrollToTop()`，并未同时驱动承载主业务内容区所在的全局 Window 滚动置顶。

---

## 2. 核心优化方案与改动细节

### 2.1 变量系统扩充 (`src/styles/variables.scss`)
- 在 SCSS 变量层新增 Google Workspace 风格的滚动条滑块半透明调色阶梯：
  - `$scrollbar-thumb-light: rgba(60, 64, 67, 0.28);`（Google Gray 700 柔和微透明滑块）；
  - `$scrollbar-thumb-light-hover: rgba(60, 64, 67, 0.45);`（鼠标悬停加深态）；
  - `$scrollbar-thumb-dark: rgba(255, 255, 255, 0.22);`（暗色模式半透明白滑块）；
  - `$scrollbar-thumb-dark-hover: rgba(255, 255, 255, 0.38);`（暗色模式悬停加深态）。

### 2.2 全局视口与通用滚动条重构 (`src/styles/index.scss`)
1. **显式声明浏览器引擎主题模式**：
   - 在 `:root, [data-theme='light']` 注入 `color-scheme: light;`；
   - 在 `html.dark, [data-theme='dark']` 注入 `color-scheme: dark;`；
   - 注入 CSS 变量 `--v-scrollbar-thumb` 与 `--v-scrollbar-thumb-hover`，确保主题切换时动态响应。
2. **标准属性与 WebKit 渲染引擎无缝兼顾**：
   - 配置标准 CSS 滚动条规范：`scrollbar-width: thin; scrollbar-color: var(--v-scrollbar-thumb) transparent;`；
   - 配置 WebKit 拟态胶囊滑块：
     - `::-webkit-scrollbar`：统一收缩至 8px 流线宽度；
     - `::-webkit-scrollbar-track`：完全透明背景（`transparent`），消除突兀轨道；
     - `::-webkit-scrollbar-thumb`：圆角胶囊形态（`border-radius: 9999px`），利用 `border: 2px solid transparent` 配合 `background-clip: padding-box` 提供呼吸留白；
     - `::-webkit-scrollbar-corner`：转角透明处理。
3. **全局作用域与通用类**：
   - 规则同时覆盖 `html, body` 视口级以及 `.custom-scrollbar` 通用局部滚动容器；
   - 保持与 Vue Router `scrollBehavior() { return { top: 0 } }` 100% 天然兼容，路由切换时自动置顶，零二次状态同步成本与零移动端软键盘挤压副作用。

### 2.3 AppLayout 置顶语义补全 (`src/components/layouts/AppLayout.vue`)
- 完善 `AppLayout.vue` 暴露的 `scrollToTop` 方法：在调用 `sideBarRef.value?.scrollToTop()` 的同时，通过 `window.scrollTo(options ?? { top: 0, behavior: 'smooth' })` 安全驱动整页平滑滚动置顶。

---

## 3. 规范审计与验证

1. **零注释铁律**：所有修改的源码文件（`variables.scss`、`index.scss`、`AppLayout.vue`）严格剔除多余行内注释，设计细节外置收敛于本设计文档；
2. **Google 风格色彩对齐**：色彩数值完全基于 Google Gray 阶梯与透明度体系；
3. **工程构建验证**：执行 `vue-tsc --noEmit && vite build` 全量生产编译通过（Exit code 0），类型推导安全。
