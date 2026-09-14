# 管理后台竞赛列表视图组件重命名（AdminCompetitionListView）与字段截断优化审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「分析修改 @src/volur-lite-view/src/views/admin/competition/AdminCompetitionView.vue 修改为 AdminCompetitionListView.vue，然后统一对列表中的内容进行对长度超出的部分进行截断。然后竞赛标题中不要显示短链接。」

在管理后台竞赛模块升级为 SPA 路由架构后，原有的 `AdminCompetitionView.vue` 仅负责呈现竞赛列表，组件命名为 `AdminCompetitionListView.vue` 更符合语义化与模块对称性。同时，为了避免标题过长或内容超出撑破表格排版，需要开启全局列截断（Ellipsis + Tooltip）能力，并从标题列中移除重复的短链接展示。

---

## 2. 核心架构与代码改动点

### 2.1 组件规范命名与路由对齐
1. **文件重命名与 Git 历史留存**：
   - 将 [`src/views/admin/competition/AdminCompetitionView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue) 通过 `git mv` 重命名为 [`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue)；
   - 内部根容器与样式选择器统一重构为 `.admin-competition-list-view`。
2. **路由引用更新 (`src/router/routes.ts`)**：
   - 将 `/admin/competitions` 对应的动态组件导入更新为：
     `component: () => import('@/views/admin/competition/AdminCompetitionListView.vue')`。

### 2.2 表格内容长度超出统一截断与 Tooltip
1. **表格布局锁定 (`table-layout="fixed"`)**：
   - 在 `<n-data-table>` 容器上显式配置 `table-layout="fixed"`，保证各列宽度约束生效，杜绝无固定布局导致的列宽被长文本撑破。
2. **多列配置 Ellipsis 与悬浮气泡**：
   - **ID 列 (`col_id`)**：`width: 80, ellipsis: { tooltip: true }`；
   - **标题列 (`col_title`)**：`minWidth: 200, ellipsis: { tooltip: true }`；
   - **赛制列 (`col_type`)**：`width: 120, align: 'center', ellipsis: { tooltip: true }`；
   - **状态列 (`col_status`)**：`width: 100, align: 'center', ellipsis: { tooltip: true }`；
   - **赛程时间列 (`col_time`)**：`width: 260, ellipsis: { tooltip: true }`，起止时间单行文本截断；
   - **队伍/人数规模列 (`col_scale`)**：`width: 130, ellipsis: { tooltip: true }`；
   - **操作栏列 (`col_actions`)**：`width: 200, fixed: 'right'`。

### 2.3 竞赛标题移除短链接
- **原逻辑**：在标题单元格中通过两个 `div` 垂直同时渲染 `row.title` 和小号浅灰色的 `/${row.slug}`；
- **新逻辑**：彻底剔除短链接 `/${row.slug}`，仅渲染标题文本 `row.title`。标题支持点击快速进入编辑页，长标题通过 `...` 截断，鼠标悬停时自动通过 Naive UI Tooltip 完整提示。

---

## 3. 构建与验证结果

1. **前端工程构建与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态检查 100% 通过（0 错误，0 告警）；
     - `AdminCompetitionListView` 打包体积为 **6.14 kB** (gzip: 2.62 kB)；
     - 4.16s 构建成功。

---

## 4. 影响范围与审计结论

- **影响文件**：
  - `src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue`（由原 `AdminCompetitionView.vue` 重命名并优化）
  - `src/volur-lite-view/src/router/routes.ts`
- **审计结论**：重命名完全符合命名规范，表格列全量启用了内容超出截断与气泡提示，标题栏短链接已干净剔除，视觉排版整齐规范。
