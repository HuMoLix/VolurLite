# AdminLayout 后台独立双栏滚动模式与滚动槽位稳定化重构

## 1. 变更背景与架构演进

在后台工作台界面中，左侧为固定的全局侧边栏导航，右侧为主工作内容区（包含各类数据管理表格、图表、配置表单等）。

在原有实现中：
1. `AdminLayout` 采用常规的 `min-h-screen` 流式布局，当右侧业务内容超出视口时，滚动条寄生于全局 `html / body` 视口；
2. 全局视口默认未配置固定滚动槽位，当不同模块在“高度不足一屏（无滚动条）”与“长数据列表（产生滚动条）”之间切换时，由于右侧 8px 滚动条物理宽度的突变，会导致通过 `max-w-[100rem] mx-auto` 居中的主内容容器发生明显的横向像素级抖动（Cumulative Layout Shift）；
3. 打开模态对话框或执行全局交互锁屏时，`html / body` 滚动条的突发移除也会引起整体页面轻微位移。

为彻底解决该问题，将后台工作台演进升级为现代企业级 Web 控制台标准的**独立双栏独立滚动模式（App-like Dual-Pane Independent Scroll）**。

## 2. 核心设计与改造方案

### 2.1 整体容器锁定与视口隔离
- 将 `.admin-layout` 样式约束为严格的屏幕视口尺寸：
  - `height: 100vh; height: 100dvh; max-height: 100vh; max-height: 100dvh; overflow: hidden;`
  - 彻底切断后台工作台向外层 `html / body` 溢出滚动条的通道，实现视口级别的干净隔离。

### 2.2 右侧主容器独立滚动与槽位稳定化
- 右侧主容器 `.admin-main`：
  - 配置 `h-full overflow-y-auto overflow-x-hidden;` 与 `-webkit-overflow-scrolling: touch; overscroll-behavior-y: contain;` 承载右侧独立滚动与平滑移动端惯性；
  - 注入关键 W3C 规范属性 `scrollbar-gutter: stable;`，在容器右侧建立稳定预留槽位；无论子内容长度如何变化，可用内容宽度始终保持恒定；
  - 统一应用系统级细滚动条规范（8px 宽度、圆角滑块与微交互高亮）。

### 2.3 路由切换平滑复位与方法暴露
- 在 `AdminLayout.vue` 脚本层引入 `adminMainRef`：
  - 监听 `route.path` 变更，路由切换时自动平滑复位容器滚动位置至顶部（`scrollTo({ top: 0, behavior: 'auto' })`）；
  - 实现并向外暴露 `scrollToTop(options?: ScrollToOptions)` 方法，保证与平台布局组件接口的一致性。

### 2.4 全局视口解耦
- 保持全局 `src/styles/index.scss` 中 `html, body` 的纯净，避免在全局根节点与双栏容器间产生重复的多重空白槽位干扰。

## 3. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/components/layouts/AdminLayout.vue`
  - `src/volur-lite-view-NEXT/src/styles/index.scss`
- **构建与校验**：
  - `npm run build` 执行成功，通过全部 TypeScript 类型检查与 Vite 打包。
  - 路由在不同长短数据页面间跳转切换时，右侧主内容区无任何横向跳动与晃动；左侧侧边栏与右侧工作台各自拥有独立的滚动生命周期，互不干扰。
