# VolurEx 顶部栏通用组件 VolurHeader 提取与主题色完全对齐重构审计文档

## 1. 变更背景与架构动机

### 1.1 背景说明
在平台前端组件库 `volurEx`（`src/volur-lite-view/src/public/volurEx/`）的规范演进中，侧边栏已成功抽取并收敛为平台级通用组件 [`VolurSidebar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurSidebar.vue)。然而，前台布局 [`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue) 与后台管理布局 [`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue) 中的顶部导航栏（`<n-layout-header>`）存在大量重复样板代码：
1. **代码冗余度高**：包括面包屑计算（平台名/详情页/管理中心/父级路由/当前路由）、中英文国际化语言切换按钮、亮暗主题切换按钮、用户信息展示（`AppAvatar` 与昵称）、用户下拉菜单（个人中心、返回前台、退出登录）等逻辑在两个布局中各自独立维护；
2. **主题色彩割裂（核心痛点）**：
   - `VolurSidebar` 中使用了标准设计令牌与 Naive UI 容器色（日间模式下为纯白 `#ffffff`，黑夜模式下为深曜炭黑 `#121826`）；
   - 而原 `AppLayout` 和 `AdminLayout` 的顶栏背景则使用了 `--v-bg-header`（日间模式为半透明灰色 `rgba(250, 250, 250, 0.85)`，黑夜模式为 `rgba(11, 12, 16, 0.82)`），与侧边栏的 Logo 区域（高 64px）在左上角拼接交界处产生明显的色差断层，未能达成设计系统的一体化视觉标准。

---

## 2. 核心改动点

### 2.1 新建自研组件 `VolurHeader.vue` (`@/public/volurEx/components/VolurHeader.vue`)
参考 `VolurSidebar.vue` 的设计范式与工程规范，实现高度解耦、高复用、开箱即用的顶部导航栏组件：
1. **模式化自动适配 (`mode: 'app' | 'admin' | 'custom'`)**：
   - `'app'` 模式：自动处理前台面包屑导航逻辑（支持 `systemStore.platformName` 首页跳转、竞赛详情页 slug/短链层级解析、常规页面父级路由与当前路由动态回显），默认提供用户中心（个人中心、退出登录）与站内通知支持；
   - `'admin'` 模式：自动处理管理后台面包屑导航（`[管理中心]` -> 父级模块 -> 当前路由），默认提供后台用户下拉选项（返回平台、退出登录）；
   - `'custom'` 模式：支持通过 `breadcrumbItems` prop 传入自定义路由路径数组。
2. **主题色与 VolurSidebar 100% 严格一致**：
   - **日间模式**：背景色统一为 `var(--n-color, #ffffff)`，下边框为 `var(--v-border-base, #efeff5)`，完全消除过去的灰白杂质色；
   - **黑夜模式**：背景色统一为 `var(--n-color, #121826)`，下边框为 `var(--v-border-base, #2e303a)`，与 `VolurSidebar` 的背景色与边框色形成完美的无缝横向延伸；
   - 支持毛玻璃与吸顶扩展样式。
3. **极具弹性的插槽体系**：
   - `#left` / `#breadcrumb`：自定义左侧内容与面包屑；
   - `#center` / `#default`：中间插槽（供全局搜索、竞技场倒计时使用）；
   - `#right` / `#tools` / `#extra` / `#notifications` / `#user`：覆盖与扩展右侧工具栏、语言切换、主题切换、通知与用户资料。
4. **移动端响应式优化 (`@media (max-width: 768px)` 与 `520px`)**：
   - 移动端减小内边距，限制面包屑单项最大宽度（防止撑破视口）；
   - 超小屏下自动折叠用户文本昵称仅保留头像，避免横向挤压溢出。

### 2.2 全局主题设计令牌与 Naive UI 主题覆盖对齐
1. **`theme.scss` (`src/public/volurEx/styles/theme.scss`)**：
   - 日间模式下将 `--v-bg-header` 对齐调整为 `#ffffff`；
   - 黑夜模式下将 `--v-bg-sidebar` 与 `--v-bg-header` 统一规范为 `#121826`。
2. **`App.vue` (`src/volur-lite-view/src/App.vue`)**：
   - 对齐 Naive UI `Layout` 主题配置中的 `siderColor`、`headerColor`、`footerColor`，暗色模式统一采用 `#121826`。

### 2.3 布局组件重构瘦身 (`AppLayout.vue` & `AdminLayout.vue`)
1. **`AppLayout.vue`**：
   - 原 85 行的 `<n-layout-header>` 模板精简替换为 `<volur-header mode="app" show-notification />`；
   - 清理所有重复的路由计算属性与语言/主题/下拉逻辑，文件体积显著缩减。
2. **`AdminLayout.vue`**：
   - 原 47 行的 `<n-layout-header>` 模板精简替换为 `<volur-header mode="admin" />`；
   - 移除冗余的模板代码与样式，保持薄布局原则。
3. **组件库统一集中导出 (`@/public/volurEx`)**：
   - 在 `src/public/volurEx/components/index.ts` 中导出 `VolurHeader` 及其 TypeScript 接口类型 `VolurHeaderProps`、`VolurBreadcrumbItem`。

---

## 3. 影响范围与改动文件清单

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| [`src/public/volurEx/components/VolurHeader.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurHeader.vue) | **新增** | VolurEx 平台自研通用顶部导航栏组件 |
| [`src/public/volurEx/components/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/index.ts) | **修改** | 集中导出 `VolurHeader` 及相关 TS 类型 |
| [`src/public/volurEx/styles/theme.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/styles/theme.scss) | **修改** | 对齐 `--v-bg-header` 和 `--v-bg-sidebar` 主题色变量 |
| [`src/App.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/App.vue) | **修改** | 对齐 Naive UI `Layout` 的 `headerColor` 和 `siderColor` 主题覆盖 |
| [`src/public/volurEx/layouts/AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue) | **修改** | 引入 `VolurHeader`，精简顶栏模板与样式 |
| [`src/public/volurEx/layouts/AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue) | **修改** | 引入 `VolurHeader`，精简顶栏模板与样式 |

---

## 4. 安全隐患说明与兼容性验证

1. **类型安全性保障**：
   - 组件通过 `withDefaults(defineProps<VolurHeaderProps>(), ...)` 实现完备的默认值处理；
   - 运行 `npx vue-tsc --noEmit`，结果 **0 错误**。
2. **用户交互与权限无缝延续**：
   - 登录/未登录态判定、路由跳转、国际化切换、亮暗主题切换、退出登录状态清理完全平滑迁移，无任何功能丢失或行为断层。
3. **生产构建构建校验**：
   - 运行 `npm run build`，Vite 生产包构建顺利通过（**Built in 5.71s，Exit Code 0**）。
