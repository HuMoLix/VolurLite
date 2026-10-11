# AppLayout 响应式应用布局组件设计与方案 B 路由级集成实现

## 1. 变更背景与需求说明
为了实现 Volur Lite NEXT 平台主框架视图的标准化收敛，需要在 `src/components/layouts` 目录下创建通用主应用布局组件 `AppLayout.vue`，并将 `@/components/sidebar/SideBar.vue` 引入并集成到该布局体系中。

整体采用 Flexbox 响应式布局：
- 桌面端（电脑端，宽度 >= 840px / md 断点）：左侧为 `SideBar` 导航栏，固定或粘性布局；右侧为页面主要内容区域；
- 移动端（宽度 < 840px）：侧边栏平滑降级为底部导航栏（BottomBar），右侧内容区域自适应全屏宽度并在底部预留安全避让间距（`pb-16 md:pb-0`）。

根据架构决策，`/competitions` 路由采用**方案 B（路由级统配）**接入 `AppLayout`，消除子视图手写侧边栏样板代码。

## 2. 核心架构设计与改动点

### 2.1 组件实现 (`src/components/layouts/AppLayout.vue`)
1. **Flex 结构与响应式容器**：
   - 根容器采用 `min-h-screen flex bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300`；
   - 桌面端左侧承接 `SideBar` 组件（通过 `SideBar.vue` 自带的 `display: contents` 与桌面端 sticky 状态无缝成为 Flex 左侧主导航）；
   - 右侧承载 `.app-main` 容器，配置 `flex-1 flex flex-col min-w-0 pb-16 md:pb-0`，确保内容自适应拉伸并防溢出；
   - 提供 `<main class="app-content">` 并配置 `<slot><router-view /></slot>`，既支持作为父级路由组件渲染子路由，也支持作为独立布局组件包裹页面内容。
2. **SideBar 属性与事件全量透传**：
   - 完整继承与透传 `SideBarProps` 配置（宽、折叠宽度、Logo 配置、路由、主题、操作栏、搜索配置等）；
   - 双向绑定 `v-model:collapsed` 与 `appStore.isSidebarCollapsed`；
   - 双向绑定 `v-model:value` 并结合当前激活路由（`/competitions`、`/announcements`）提供开箱即用的高亮与路由跳转能力；
   - 完整透传所有事件（`select`、`collapse`、`expand`、`logo-click`、`search-*`、`theme-toggle`、`user-click`、`logout` 等）；
   - 完整透传所有插槽（`#header`、`#footer`、`#sidebar`、`#logo`、`#menu`、`#user-profile`、`#search-dialog` 等）。
3. **实例方法暴露 (`defineExpose`)**：
   - 暴露 `sideBarRef`、`isCollapsed`、`toggleCollapse`、`expand`、`collapse`、`openSearch`、`closeSearch`、`scrollToTop`。

### 2.2 模块统一导出 (`src/components/layouts/index.ts`)
- 导出 `AppLayout`；
- 导出 `AuthLayout`；
- 导出 `AppLayoutProps` 类型声明。

### 2.3 方案 B 路由层统配与视图重构
1. **路由层统一挂载 (`src/router/routes/competition.ts`)**：
   - 将 `competitionRoutes` 顶级配置为 `path: '/'`，其 `component` 统一引入 `AppLayout.vue`；
   - 子路由包含 `competitions`（赛事大厅）、`competitions/:id`（赛事工作台）与 `c/:id`（赛事短链），自动挂载在 `AppLayout` 的 `<router-view />` 视口中。
2. **视图层瘦身 (`src/views/competition/CompetitionListView.vue`)**：
   - 彻底移除页面外层冗余的 `<div class="min-h-screen flex ...">` 与 `<SideBar>` 声明；
   - 移除已无用的 `SideBar`、`useAppStore`、`activeNavKey`、`navMenuOptions` 与 `handleNavSelect` 状态与逻辑；
   - 页面由 239.59 kB 瘦身至 144.96 kB，侧边栏逻辑与页面纯业务逻辑彻底解耦。

## 3. 代码纯洁性与规范审计
- **零注释铁律**：所有代码文件（`AppLayout.vue`、`index.ts`、`competition.ts`、`CompetitionListView.vue`）完全遵循零注释原则，无多余行内注释与解释性备注；
- **类型安全**：全面遵循 TypeScript 5.7+ 严格模式与 ES2026 规范，未使用的变量全部清理，无 `any` 泛滥；
- **工程构建**：通过 `vue-tsc --noEmit && vite build` 生产级构建验证，零报错通过。
