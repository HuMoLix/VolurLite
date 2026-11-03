# AppContent 内边距重构与 volur-ui VViewlayout 统一视图布局组件落地

## 变更背景
在 `/competitions` 赛事大厅页面及全局主布局（`AppLayout.vue`）的迭代与审查中，发现视图与内容容器存在多层内边距嵌套与职责冗余问题：
1. **多层内边距叠加 (Double Padding)**：在超宽屏与常规视口下，`app-content` 承载各路由页面，但此前在 `CompetitionListView.vue` 的根样式 `.competition-list-view` 和内部容器 `ContainerLayout` (`secondary-container-layout`) 中各自定义了 `padding: 48px 24px 24px 24px` 与 `padding="large"`，导致样式分散且内边距重复嵌套；
2. **中间容器层过度设计**：`CompetitionListView.vue` 内部引入了次级布局 `secondary-container-layout`（`ContainerLayout`），仅用于承载 `<header>`、主体内容和 `AppFooter`，增加了不必要的 DOM 层级与样式耦合；
3. **全局通用视图布局缺失**：平台各业务视图在 `app-content` 内部缺乏统一的标准轻量 Flex 视图布局容器；
4. **页脚组件职责错位**：`AppFooter.vue` 原位于 `src/layouts/components/AppFooter.vue`，作为独立碎片组件散落，未被收敛入通用 UI/Layout 体系。

---

## 核心设计与重构方案

### 1. `app-content` 统一内边距与视图层内边距剥离
- 在 [`src/layouts/primary/AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/layouts/primary/AppLayout.vue) 的 `.app-content` 样式中统一配置 `padding: 48px 24px 24px 24px; box-sizing: border-box;`，确保所有挂载在 `app-content` 下的页面视图在顶级获得一致的标准上外边距与横向安全间距；
- 在 [`src/views/competition/CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue) 中彻底移除 `.competition-list-view` 上硬编码的 `padding: 48px 24px 24px 24px`，避免多层内边距叠加。

### 2. 剥离 `secondary-container-layout` 引用
- 在 [`src/views/competition/CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue) 中移除 `<ContainerLayout>` 引用及 `@/layouts` 导入，消除产生的 `.secondary-container-layout` 冗余 DOM 节点。

### 3. `volur-ui` 布局体系扩充：`VViewlayout` 与 `VFooter`
- 在 [`src/volur-ui/layouts/`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/volur-ui/layouts) 下新增布局体系：
  - **`VViewlayout.vue`**：专为一般页面视图设计的通用布局容器。因直接置于 `app-content` 内部，自身无需设置额外 padding；采用 `flex flex-col flex-1 min-w-0 w-full` 纯净排版，通过语义化 `<header>`、`<main>`、`<footer>` 标签清晰组织视图结构。
  - **`VFooter.vue`**：将原 `AppFooter.vue` 中的备案号（ICP）、动态版权信息、工信部图标及链接统一迁移至 `volur-ui` 布局体系中，作为 `VViewlayout` 默认页脚渲染载荷，并支持全局独立引用。
  - **`index.ts`**：统一导出 `VViewlayout` 与 `VFooter`，并注册至 `VolurUI` 全局插件。
- 彻底删除孤立的 [`src/layouts/components/AppFooter.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/layouts/components/AppFooter.vue) 及其父目录，同步更新全局所有旧引用处（`CompetitionLayout.vue`、`CompetitionDetailView.vue`、`NotFoundView.vue`、`src/layouts/index.ts`），杜绝死代码与旧命名别名。

---

## 影响范围与验证结果

1. **涉及修改文件**：
   - `src/layouts/primary/AppLayout.vue`：添加 `padding: 48px 24px 24px 24px`；
   - `src/views/competition/CompetitionListView.vue`：移除 padding，替换为 `VViewlayout`；
   - `src/volur-ui/layouts/VViewlayout.vue`：新增视图布局组件；
   - `src/volur-ui/layouts/VFooter.vue`：承接原页脚逻辑与视图；
   - `src/volur-ui/layouts/index.ts`：导出新增布局；
   - `src/volur-ui/index.ts`：注册并导出布局组件；
   - `src/layouts/components/AppFooter.vue`：彻底删除；
   - `src/layouts/components/index.ts`：彻底删除；
   - `src/layouts/index.ts`：清理 components 导出；
   - `src/layouts/primary/CompetitionLayout.vue`：迁移至 `VFooter`；
   - `src/views/competition/CompetitionDetailView.vue`：迁移至 `VFooter`；
   - `src/views/error/NotFoundView.vue`：迁移至 `VFooter`。
2. **构建与类型验证**：
   - 全链路执行 `vue-tsc --noEmit && vite build`，编译完全通过，0 错误、0 警告。
