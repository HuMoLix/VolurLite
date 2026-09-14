# 通用列表与数据表格组件 AppTableCard / CommonTableCard 抽象提取与规范化施工文档

## 1. 变更背景与需求概述
在 Volur Lite 前端工程视图中，存在大量数据列表与表格渲染场景，主要涵盖：
1. `AdminUserView.vue`（系统用户管理列表）
2. `AdminCompetitionListView.vue`（系统竞赛管理列表）
3. `AdminBroadcastView.vue`（系统广播与通告历史列表）
4. `NodesTab.vue`（集群节点管理表格）
5. `AdminContainerView.vue`（运行容器实例监控列表）
6. `AdminImageView.vue`（私有 Registry 镜像仓库列表）
7. `AdminLogView.vue`（审计与操作日志列表）
8. `AdminTaskView.vue`（后台异步任务调度列表）
9. `AdminEmailView.vue`（邮件发送历史列表）

经全景分析，上述视图在列表渲染与样式实现上存在显著的重复性代码与样板模式：
- **容器与卡片样板**：各个管理端视图统一通过 `<n-card class="table-card" :bordered="false">` 或类似卡片包裹列表，且重复编写 `.table-card` 基础样式；
- **工具栏与查询栏结构高度同构**：统一采用 `.filter-header` 承载 `.filter-left`（输入框、选择器、日期器、查询/重置按钮）与 `.filter-right`（新增、批量操作、同步/刷新按钮）；
- **基础样式类重复定义**：`.theme-primary-btn`（纯黑背景操作按钮）、`.mono-text` / `.id-text`（Fira Code 等宽字体）、`.truncate`（防溢出截断）、`.table-action-btns`（操作列按钮容器）、`.text-muted` 与 `.pagination-bar` 在 9 个文件中重复声明定义；
- **展示形态的多样性需要兼顾**：大多数为标准服务端受控分页的 `n-data-table` 表格，部分为独立底部分页（如 `AdminImageView.vue`）或固定节点列表（如 `NodesTab.vue`）。

为此，本任务抽取并实现全工程通用的列表与数据表格卡片组件 [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue) 及互通别名 [`CommonTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/CommonTableCard.vue)，同时收敛提取公共样式表 [`table.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/table.scss)。

---

## 2. 九大视图列表结构与样式特征全景对比

| 视图文件 | 列表承载容器 | 工具栏/过滤头结构 | 数据展示核心组件 | 分页机制 | 高频复用样式与特殊细节 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`AdminUserView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` (remote) | 受控分页对象 `pagination` | 搜索/角色/状态/注册时间范围；用户信息纵向单元格；纯黑按钮 |
| **`AdminCompetitionListView.vue`** | `n-card.content-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` (remote) | 受控分页对象 `pagination` | 竞赛关键词与赛制过滤；新建竞赛跳转；无实线表格 |
| **`AdminBroadcastView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` (remote) | 受控分页对象 `pagination` | 关键词/分类/展现形态；双行截断摘要 `.content-snippet` |
| **`AdminContainerView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` (remote, selection) | 受控分页对象 `pagination` | 多选勾选列；剩余时间动态警报色；雪花 ID 快捷复制 |
| **`AdminImageView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` | 外部独立底部分页 `.pagination-bar` | 4 列紧凑排版；镜像名单行截断 + 复制；操作列文字+图标 |
| **`AdminLogView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left`) | `n-data-table` (remote) | 受控分页对象 `pagination` | 日志级别/模块/状态多条件；操作人双行单元格 |
| **`AdminTaskView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` (remote) | 受控分页对象 `pagination` | 任务进度条单元格；生命周期耗时；清空历史 popconfirm |
| **`AdminEmailView.vue`** | `n-card.table-card` | `.filter-header` (`.filter-left` + `.filter-right`) | `n-data-table` (remote) | 受控分页对象 `pagination` | 邮件类型/状态；发件人单行截断；重发操作列 |
| **`NodesTab.vue`** | 外层 Card 容器 | `.nodes-toolbar` (`.toolbar-title` + `.toolbar-actions`) | `n-data-table` | 无分页（固定节点列表） | 标题与按钮并排；健康巡检与加入节点操作 |

---

## 3. 改动点清单与技术实现

### 3.1 提取并创建公共列表样式表 [`src/styles/table.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/table.scss)
收敛了在全部 9 个视图中重复出现的列表 CSS 规则：
1. `.table-card`：统一背景色 `var(--n-color, var(--v-bg-card, #ffffff))` 与 10px 圆角；
2. `.filter-header`、`.filter-left`、`.filter-right`：标准化响应式两端对齐过滤工具栏；
3. `.theme-primary-btn`：标准化 Volur 规范主要操作按钮（纯黑底白字、悬停 `#222222`、圆角 6px、禁用透明度 0.5）；
4. `.mono-text` / `.id-text`：统一绑定 `var(--v-font-mono, 'Fira Code', monospace)`；
5. `.truncate`：单行文本超出省略；
6. `.time-text`、`.text-muted`：次要说明文本颜色规范；
7. `.table-action-btns` / `.action-btns`：操作列按钮群间距容器；
8. `.pagination-bar` / `.pagination-wrap`：底部分页条定位容器。
并在 [`src/styles/index.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/index.scss) 中通过 `@use './table.scss';` 进行工程级注入。

### 3.2 实现通用列表组件 [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue)
- **卡片/内嵌自适应**：支持 `embedded` 属性，为 `true` 时不渲染外层 `n-card`，完美适配 Tab 嵌入视图（如 `NodesTab.vue`）；
- **渐进式插槽体系**：
  - `#filter-left`：左侧查询条件（搜索框、选择器、时间选择器、查询/重置按钮）；
  - `#filter-right`：右侧功能按钮（新增、批量处理、同步/刷新）；
  - `#toolbar`：全行自定义工具栏（替换默认 `.filter-header`）；
  - `#default`：自定义列表插槽，支持非 `n-data-table` 的流式/卡片列表（如 `CompetitionListView.vue`）；
  - `#table-header` / `#table-footer`：表格上下附加元素插槽；
  - `#pagination`：独立底部分页插槽；
  - `#empty`：空数据视图插槽；
- **Naive UI 数据表格透传**：
  - 完整透传 `columns`、`data`、`loading`、`remote`、`pagination`、`row-key`、`checked-row-keys`、`bordered`、`size`、`max-height`、`scroll-x` 等参数；
  - 事件双向受控绑定：`update:page`、`update:pageSize`、`update:sorter`、`update:filters`、`update:checkedRowKeys`；
- **独立外部分页支持**：集成 `standalone-pagination`、`page`、`pageSize`、`itemCount`、`pageSizes` 属性，开箱即用支持外挂分页条。

### 3.3 实现别名组件 [`CommonTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/CommonTableCard.vue)
通过继承与透传属性/插槽，提供与 `AppTableCard.vue` 100% 同构的 `CommonTableCard` 引用能力，消除开发者引用习惯差异。

### 3.4 九大核心视图组件复用与样式清理

1. **`AdminUserView.vue`（用户管理列表）**：
   - 移除包裹卡片 `<n-card class="table-card" :bordered="false">` 与内置 `<n-data-table>`，统一替换为 `<AppTableCard>`；
   - 过滤工具栏条件迁移至 `#filter-left` 与 `#filter-right` 插槽；
   - 清除组件本地重复定义的 `.table-card`、`.filter-header`、`.filter-left`、`.filter-right`、`.theme-primary-btn`、`.mono-text`、`.id-text` 等全局样式类。
2. **`AdminCompetitionListView.vue`（竞赛管理列表）**：
   - 移除包裹卡片 `<n-card class="content-card" :bordered="false">` 与内置 `<n-data-table>`，替换为 `<AppTableCard>`；
   - 将关键词输入框、赛制类型下拉框与搜索按钮收敛到 `#filter-left` 插槽，将新建竞赛按钮放置于 `#filter-right`；
   - 移除本地重复的 `.content-card`、`.filter-header` 与 `.theme-primary-btn` 样式类；
   - 响应式接入分页变更处理器 `@update:page` 与 `@update:page-size`。
3. **`AdminBroadcastView.vue`（广播通知列表）**：
   - 采用 `<AppTableCard>` 承载广播分页表格，透传 columns、data、loading、pagination；
   - 过滤输入框、类型与形态下拉框收敛于 `#filter-left`，发布广播按钮置于 `#filter-right`；
   - 清理重复的 `.table-card`、`.filter-header`、`.theme-primary-btn`、`.table-action-btns` 等样式规则。
4. **`NodesTab.vue`（集群节点管理 Tab）**：
   - 采用 `<AppTableCard>` 渲染外层 Card 容器，统一整体列表卡片视觉规格；
   - 将节点标题、健康巡检、加入节点等头部动作放置于 `#toolbar` 插槽；
   - 内部数据表格通过无实线配置渲染，消除多余样式。
5. **`AdminContainerView.vue`（容器监控列表）**：
   - 使用 `<AppTableCard>` 接入，透传受控勾选 `v-model:checked-row-keys="selectedRowKeys"`，支持批量清理；
   - `#filter-left` 聚合关键词与竞赛类型过滤；`#filter-right` 聚合批量销毁与刷新按钮；
   - 清理重复容器与表格样式，保留特定剩余时间告警等业务样式。
6. **`AdminImageView.vue`（镜像仓库列表）**：
   - 启用 `standalone-pagination` 模式（配置 `:page="currentPage"`、`:page-size="pageSize"`、`:item-count="totalCount"` 以及 `@update:page`、`@update:page-size`）；
   - 彻底移除了视图内部冗余维护的手动 `n-pagination` 分页栏 HTML 与相关 CSS；
   - 清理多余的选择器残留与未引用组件依赖，实现极简声明。
7. **`AdminLogView.vue`（审计日志列表）**：
   - 替换为 `<AppTableCard>`，`#filter-left` 挂载模块、日志级别、执行状态选择器以及清空日志弹窗确认按钮；
   - 清理大量重复的表单与表格容器 CSS。
8. **`AdminTaskView.vue`（调度任务列表）**：
   - 替换为 `<AppTableCard>`，`#filter-left` 挂载任务类型、状态与执行结果下拉项，`#filter-right` 挂载清空历史任务 Popconfirm 按钮；
   - 清理组件内部重复的表格卡片与操作容器样式。
9. **`AdminEmailView.vue`（邮件日志列表）**：
   - 替换为 `<AppTableCard>`，`#filter-left` 挂载邮件类型、投递状态与时间段筛选，`#filter-right` 挂载手动刷新按钮；
   - 清理重复定义的 `.table-card`、`.filter-header`、`.table-action-btns` 样式。

### 3.5 外层 Card 与列表 Table 双层 Border 启用、圆角化与内部实线划分取消
为贯彻现代精致扁平化卡片列表设计语言，对通用组件与公共样式体系进一步硬化规范：
1. **统一渲染外层 Card（bordered: true，10px 圆角）**：
   - `AppTableCard.vue` 根节点始终渲染 `NCard`，且默认 `bordered: true`，各视图显式配置 `:bordered="true"`，为外层卡片提供清晰、精致的 1px 外围轮廓线与 10px 圆角；
   - 全部 9 大管理视图（用户、竞赛、广播、节点、容器、镜像、日志、任务、邮件）统一拥有 `:bordered="true"` 的外层卡片边界与层级，告别模糊边界；
2. **列表数据表格 Border 启用与圆角化（tableBordered: true，8px 圆角）**：
   - 数据表格外围边框启用（`tableBordered: true`），同时在 [`src/styles/table.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/table.scss) 与 [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue) 中为 `.n-data-table-wrapper` 统一施加 `border: 1px solid var(--n-border-color, var(--n-merged-border-color, #e0e0e6));` 与 `border-radius: 8px !important; overflow: hidden !important;`；
   - 使列表呈现为一个内嵌的、具备圆角边框包裹的精致数据盒；
3. **彻底取消表格内部单元格横纵实线划分**：
   - 数据表格配置 `:single-column="true"` 与 `:single-line="true"`；
   - 显式抹除 `.n-data-table-th`、`.n-data-table-td` 的内部 `border-bottom` 与 `border-right` 单元格实线，并移除 `::after` / `::before` 伪元素边框；
   - 形成“外层卡片精致描边（10px 圆角）+ 内部数据表圆角边框盒（8px 圆角）+ 数据表内部无横纵杂乱细线”的现代高级质感。

---

## 4. 验证情况与构建检查
- 运行 `npm run build`（调用 `vue-tsc --noEmit && vite build`）：
  - TypeScript 类型检查完全通过（0 错误，0 告警）；
  - 全工程 9 大视图及公共组件全部成功编译打包并生成生产构建产物（构建耗时 4.56s）；
  - 零侵入式插槽与属性设计，保证既有页面事件、受控分页及选中状态无缝兼容。
- 变更影响范围：
  - 公共组件：[`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue)、[`CommonTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/CommonTableCard.vue)；
  - 公共样式：[`table.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/table.scss)、[`index.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/index.scss)；
  - 目标视图：`AdminUserView.vue`、`AdminCompetitionListView.vue`、`AdminBroadcastView.vue`、`NodesTab.vue`、`AdminContainerView.vue`、`AdminImageView.vue`、`AdminLogView.vue`、`AdminTaskView.vue`、`AdminEmailView.vue`。
