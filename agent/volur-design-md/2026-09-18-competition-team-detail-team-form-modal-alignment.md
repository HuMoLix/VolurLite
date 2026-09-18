# 参赛队伍详情模态框全面对齐 TeamFormModal 设计规范记录

**施工日期**：2026-09-18  
**涉及模块**：前端竞赛管理队伍审核模块 (`volur-lite-view`)  
**目标文件**：
- [`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)
**对齐参照文件**：
- [`src/volur-lite-view/src/views/competition/components/TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue)

---

## 1. 变更背景与设计动机

在此前的重构中，管理员端参赛队伍详情卡片存在视觉层级杂乱、与选手端已有成熟组件风格不统一的问题。
为了贯彻平台统一的交互和视觉规范，用户明确指示**全面参考 `TeamFormModal.vue` 的结构与排版样式**，对 `TeamAuditTab.vue` 中的“参赛队伍详情”模态框进行彻底重塑。

---

## 2. 核心改动点说明

### 2.1 模态框规格与容器规范对齐
- 规格调整为：`style="width: 1200px; max-width: 96vw; border-radius: 12px;" :bordered="false"`，并附加 `class="team-detail-modal"`，使整体弹窗尺寸、内外边距与圆角曲率与 `TeamFormModal` 100% 对齐。

### 2.2 顶部队伍身份元信息区重构 (`.team-meta-panel`)
- 替代了原先冗余繁重的 `n-descriptions` 边框网格，采用统一的卡片头部面板：
  - **战队基础身份标识**：48px 圆形头像 (`AppAvatar`) + 战队名称（加粗 16px）+ 审核状态胶囊 Tag；
  - **核心元数据条**：包含队长账号、报名提交时间、所属赛事等关键字段，采用优雅的竖线分隔点阵；
  - **历史驳回警告条**：当队伍为 `AUDIT_REJECTED` 且存在 `auditReason` 时，以柔和警示色背景展示驳回原因。

### 2.3 参赛成员资质名册排版重构 (`.roster-header` + `<n-table>`)
- 采用与 `TeamFormModal.vue` 一致的名册标题栏：
  - `.roster-header` -> `.roster-title-area` -> `.roster-title` (15px/加粗) + `.roster-count` (当前成员计数)；
- 弃用配置繁重的 `<n-data-table>`，全面改用与 `TeamFormModal` 完全一致的轻量化模板式原生表格：
  - `<div class="table-scroll-container">` + `<n-table :bordered="true" :single-line="false" size="small" class="roster-table">`；
  - 规范化表头（`th` 浅灰底色 `#fafafc`、加粗 600、粘性吸顶 `sticky top: 0`）；
  - 精确列宽与舒适单元格内边距（`8px 10px`）；
  - **队长行微高亮**：对队长行应用 `tr.captain-row`，底色呈现微质感层次；
  - **9列规范字段**：
    1. 成员姓名/账号（展示 28px 圆形头像、真实姓名与 `@username`）；
    2. 性别（迷你胶囊 Tag，男：info / 女：error）；
    3. 队内角色（战队队长：primary / 带队领队：warning / 参赛队员：default）；
    4. 学号/工号（等宽字体展现）；
    5. 联系电话（等宽字体展现）；
    6. 电子邮箱（长邮箱自动换行）；
    7. 身份证件号码（微质感底色 Badge 徽标包裹）；
    8. 所属院系（单行省略与悬浮提示）；
    9. 加入时间（本地格式化日期居中对齐）。

### 2.4 底部审核动作栏对齐 (`.modal-actions`)
- 采用与 `TeamFormModal` 统一的 `.modal-actions` 两端对齐布局：
  - 左侧 `.actions-left`：展示当前审核状态标识（`status-indicator`）；
  - 右侧 `.actions-right`：保留“关闭”常规按钮、“驳回报名”次级警示按钮以及纯黑高对比主要操作按钮“通过资质审核”（`.theme-primary-btn`，背景 `#000000`，文字 `#ffffff`，悬浮 `#222222`）。

### 2.5 代码清理与类型精简
- 从 `naive-ui` 导入中剔除了未使用的 `NDescriptions`、`NDescriptionsItem`、`NDataTable`，引入 `NTable`；
- 移除了由 `<n-data-table>` 渲染函数生成的冗余配置数组 `detailMemberColumns`，保留强类型派生计算属性 `sortedDetailMembers = computed<CompetitionMemberVO[]>`；
- 全局 CSS 作用域使用 `:deep(.team-detail-modal)` 进行精准样式隔离。

---

## 3. 安全隐患与影响范围说明

- **安全性分析**：本变更纯属前端视图层展示与组件重构，未触碰后端接口通信、权限控制及数据结构，敏感数据（如身份证号）仍遵照平台原有展示范围。
- **影响范围**：仅影响后台赛事管理中队伍审核详情弹窗（`TeamAuditTab.vue`），不影响其他标签页及前台业务。
- **编译与构建测试**：在 `src/volur-lite-view` 下执行 `npm run build` 全量打包测试，TypeScript 静态类型检查 0 错误，Vite 构建一次性打包成功。
