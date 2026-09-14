# 竞赛管理五大子标签页通用表格卡片 (AppTableCard) 复用与横向滚动升级

## 1. 变更背景与设计目标

在 Volur Lite 竞赛编辑管理模块（`AdminCompetitionEditView.vue`）中，包含五大核心业务 Tab 子组件：
1. `ChallengeTab.vue`（赛题挂载管理）
2. `AnnounceTab.vue`（比赛专属公告通报）
3. `WriteupTab.vue`（选手 WP 提交与审核）
4. `ContainerTab.vue`（比赛靶机容器实例监控）
5. `TeamAuditTab.vue`（参赛战队人工资质审核）

此前，这五大 Tab 均采用手写 `n-card` + `n-spin` + `n-data-table` + `n-pagination` 的分散式样板代码，不仅表格边框、圆角、背景色、内部实线各不相同，且缺乏统一的工具栏卡槽排版，亦未显式配置 `:scroll-x` 与固定列。

本次改造将通用列表/表格卡片组件 [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue) 全面复用到这五大子视图中，实现全站统一的视觉质感（外层圆角微边框、消除内部实线划分）与完备的横向滚动支持。

---

## 2. 改动点汇总

### 2.1 赛题挂载标签页 ([`ChallengeTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/ChallengeTab.vue))
- **结构重构**：移除手动外层操作条与 `n-spin` + `n-data-table` 结构，替换为 `<AppTableCard>`。
- **插槽复用**：
  - 使用 `#toolbar` 承载赛题统计胶囊（已挂载数、总初始分、累计解出人次）及右侧刷新与「从题库导入赛题」按钮；
  - 使用 `#empty` 自定义无赛题提示。
- **列宽与横向滚动**：
  - 配置 `:scroll-x="960"`；
  - 操作列（配置、卸载）设置 `fixed: 'right'`。

### 2.2 比赛通报标签页 ([`AnnounceTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/AnnounceTab.vue))
- **结构重构**：替换为 `<AppTableCard>`。
- **插槽复用**：
  - 使用 `#table-header` 承载说明 Alert 提示；
  - 使用 `#toolbar` 承载已发布公告数量标签及「发布比赛公告」按钮；
  - 使用 `#empty` 自定义空提示。
- **列宽与横向滚动**：
  - 配置 `:scroll-x="920"`；
  - 操作列（置顶、编辑、删除）设置 `fixed: 'right'`。
- **清理冗余**：移除未使用的 `NSpin`、`NDataTable` 导入。

### 2.3 题解报告标签页 ([`WriteupTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/WriteupTab.vue))
- **结构重构**：将选手已提交题解列表卡片替换为 `<AppTableCard>`。
- **插槽复用**：
  - 使用 `#filter-left` 渲染列表标题与条数；
  - 使用 `#filter-right` 承载模糊搜索框、查询按钮与刷新按钮；
  - 使用 `#empty` 自定义空状态。
- **列宽与横向滚动**：
  - 配置 `:scroll-x="1150"`；
  - 操作列（在线预览、下载、评分、删除）设置 `fixed: 'right'`。
- **清理冗余**：移除未使用的 `NDataTable` 导入。

### 2.4 靶机容器标签页 ([`ContainerTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/ContainerTab.vue))
- **结构重构**：替换为 `<AppTableCard>`，启用内置的 `standalone-pagination`。
- **插槽复用**：
  - 使用 `#filter-left` 承载关键词搜索框、运行状态下拉选择框与查询按钮；
  - 使用 `#filter-right` 承载刷新容器列表按钮；
  - 使用内置分页事件 `@update:page` 与 `@update:page-size`，移除原手写的外挂分页条样板代码。
- **列宽与横向滚动**：
  - 配置 `:scroll-x="980"`；
  - 操作列（续期、强制销毁）设置 `fixed: 'right'`。
- **清理冗余**：移除未使用的 `NSpin`、`NDataTable`、`NPagination` 导入。

### 2.5 队伍审核标签页 ([`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue))
- **结构重构**：替换为 `<AppTableCard>`，启用内置 `standalone-pagination`。
- **插槽复用**：
  - 使用 `#filter-left` 承载队伍名/队长搜索框、审核状态筛选下拉框与查询按钮；
  - 使用 `#filter-right` 承载刷新列表按钮；
  - 简化分页代码，绑定当前页码与总条数。
- **列宽与横向滚动**：
  - 配置 `:scroll-x="970"`；
  - 操作列（通过、驳回、详情）设置 `fixed: 'right'`。
- **清理冗余**：移除未使用的 `NDataTable`、`NPagination` 导入。

---

## 3. 构建验证与影响范围

1. **类型与构建验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）顺利通过（0 错误，耗时 4.81s）。
2. **交互与用户体验提升**：
   - 彻底消除了各子 Tab 内部的实线网格感，保持外层卡片圆角和边框统一；
   - 保证所有表格均具备横向平滑滚动能力，操作列统一悬浮吸附在右侧，在各种尺寸显示屏下操作一致性大幅提升。
