# AdminLayout Header 移除与 VSideBar Bottom Dock 返回前台按钮重构设计方案

## 1. 变更背景与需求目标
在 `AdminLayout.vue` 管理后台布局中，原有的页面顶部包含了一个带有管理后台徽标、页面标题和“返回前台”按钮的 `<header>` 元素。同时，管理后台左侧挂载的 `SideBar` 组件中，原先通过 `show-bottom-action` 在底栏用户信息下方渲染了一个大条目的“返回前台”操作按钮，且将全局搜索按钮隐藏（`:show-search="false"`）。

为进一步精简管理后台界面层级、提供更为通透的内容工作区并将页面全局导航操作整合收敛至侧边栏底部停靠坞（Bottom Dock）：
1. 移除 `AdminLayout.vue` 中的 `<header>` 顶栏结构及其附带的冗余逻辑与组件引入；
2. 升级 `VSideBar.vue` 底部的 Bottom Dock（`sidebar-action-box`），将 `showBottomAction` 对应的“返回前台按钮”移入 Bottom Dock 中，并固定于“通知按钮”的左侧（即原全局搜索按钮所在位置）；
3. 确保在侧边栏展开状态与折叠状态（Dock 工具弹出层）下，“返回前台”按钮均能以规范的图标按钮形态良好呈现并具备完整跳转与 Tooltip 交互能力；
4. 消除原底栏区域冗余的独立大按钮，保持布局简洁一致。

---

## 2. 核心改动点

### 2.1 AdminLayout.vue
1. **模板结构精简**：
   - 彻底删除 `<div class="admin-main">` 内部的 `<header class="admin-header ...">` 结构；
   - 保留 `<main class="admin-content ...">` 作为主要视图承载容器，内容区域直接向上平铺展开。
2. **脚本冗余清理**：
   - 移除因 `<header>` 删除而不再使用的 `VBtn` 组件导入；
   - 移除仅供顶栏标题展示的 `currentTitle` 计算属性；
   - 保留 `SideBar` 挂载属性（包括 `:show-bottom-action="true"`、`:bottom-action-text`、`:bottom-action-icon="ArrowBackOutline"`、`:bottom-action-route="'/competitions'"` 等）。

### 2.2 VSideBar.vue
1. **展开态 Bottom Dock (`action-box-expanded`)**：
   - 在通知按钮（`showNotifications`）左侧，新增基于 `showBottomAction` 的工具图标按钮；
   - 当 `showSearch` 为 `false` 且 `showBottomAction` 为 `true` 时，返回前台按钮恰好位于通知按钮左侧（原搜索按钮的位置），呈现为 32px 高度、圆角为 12px 的均分 Dock 图标按钮；
   - 按钮集成 `VTooltip`，hover 时提示 `bottomActionText`（或“返回前台”），点击触发 `handleBottomActionClick` 路由跳转。
2. **折叠态 Dock 工具弹出层 (`dock-apps-popover`)**：
   - 在折叠展开的 `dock-apps-popover` 中，同样在通知按钮左侧/上方加入 `showBottomAction` 对应的 `dock-tool-btn`；
   - 悬浮提示 `bottomActionCollapsedText || bottomActionText`，点击触发 `handleCollapsedBottomActionClick`，自动收起应用弹窗并执行路由跳转。
3. **底部操作栏 (`sidebar-bottom-action`) 改造**：
   - 移除原先在用户信息下方硬编码的大尺寸独立操作按钮；
   - 调整为仅当父组件显式传递 `$slots.bottom` 且非折叠时渲染该插槽容器，杜绝与 Bottom Dock 发生重复渲染。
4. **图标与方法补齐**：
   - 从 `@vicons/ionicons5` 导入 `ArrowBackOutline`，作为未传图标时的默认后备图标；
   - 新增 `handleCollapsedBottomActionClick` 处理函数，规范执行弹窗关闭与点击事件分发。

---

## 3. 安全隐患修复与兼容性评估
1. **零破坏性兼容**：未开启 `showBottomAction` 的普通前端视图（如赛事大厅等）不会受到任何影响，原搜索按钮与通知按钮位置保持原有行为；
2. **TypeScript 类型安全**：通过 `vue-tsc --noEmit` 严格类型检测，无任何未读属性或类型断言隐患；
3. **构建产物无损**：经 Vite 构建打包验证通过，无任何样式或打包异常。

---

## 4. 影响范围与涉及文件
- `src/components/layouts/AdminLayout.vue`
- `src/components/sidebar/VSideBar.vue`
- `agent/volur-design-md/2026-10-13-admin-layout-header-removal-and-sidebar-bottom-dock-return-action-refactor.md`
