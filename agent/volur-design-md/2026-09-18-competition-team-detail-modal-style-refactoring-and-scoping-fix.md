# 参赛队伍详情模态框样式重构与作用域修复记录

**施工日期**：2026-09-18  
**涉及模块**：前端竞赛管理队伍审核模块 (`volur-lite-view`)  
**目标文件**：
- [`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)
**对齐参照文件**：
- [`src/volur-lite-view/src/views/competition/components/TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue)

---

## 1. 变更背景与核心痛点

在此前的样式演进中，管理员端在 [`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 弹出的“参赛队伍详情”模态框存在以下核心问题：
1. **Scoped CSS 样式穿透失效（关键 Bug）**：
   - 此前样式采用了 `:deep(.team-detail-modal)` 作为最外层选择器。由于 Naive UI 的 `<n-modal>` 默认渲染并传送（Teleport）至 `document.body` 根节点下，其外层并不带有当前 Vue 组件的 `data-v-xxxx` 作用域哈希标识。
   - Vue 3 的 Scoped CSS 编译器将 `:deep(.team-detail-modal)` 编译为 `[data-v-xxxx] .team-detail-modal`，导致外层选择器永远无法命中，模态框内部所有的表格排版、颜色、边框、卡片背景及间距样式完全失效（表现为未渲染样式的原始 HTML 状态）。
2. **布局结构与 `TeamFormModal.vue` 存在偏离**：
   - 底部操作按钮误放在了 `<template #footer>` 插槽中，导致 Naive UI 的 `n-card` 生成了额外的卡片页脚分割线与边距，与 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 流式内嵌在主体容器底部的 `.modal-actions` 风格不一致；
   - 成员名册统计文本未显示领队人数及赛事设定的上下限人数规则（`minTeamMembers` ~ `maxTeamMembers`、`maxLeaders`）；
   - 表格缺少“加入状态”（`MemberStatus`）列，与选手端名册列定义不对齐；
   - 缺乏针对平台深曜黑夜模式（`html.dark`, `[data-theme='dark']`）的完整语义化变量适配。

---

## 2. 核心改动点与重构说明

### 2.1 样式作用域与 Teleport 命中修复
- 彻底移除失效的 `:deep(.team-detail-modal)` 根包裹层。
- 模态框内部默认插槽元素 `<div class="team-detail-content">` 直接位于组件模板中，由 Vue 自动赋予 `data-v-xxxx` 作用域属性。
- 将样式定义直接收敛在 `.team-detail-content { ... }` 作用域下，确保其子级的所有排版规则（包括 `.team-meta-panel`、`.roster-header`、`.table-scroll-container`、`.roster-table`、`.member-profile-cell`、`.modal-actions` 等）100% 准确命中并生效。
- 增加非 Scoped `<style lang="scss">` 块专门针对传送至 `body` 的 `.team-detail-modal` 容器设置全局统一的 `border-radius: 12px !important;`。

### 2.2 顶部队伍身份元信息区微调 (`.team-meta-panel`)
- 头像尺寸调整为 46px（`AppAvatar`），更贴合弹窗比例；
- **移除队伍名称旁的审核状态胶囊**：由于弹窗底部 `.modal-actions` 左侧已常驻当前审核状态指示器，顶栏队伍名称旁不再放置重复的“已通过/待审核/已驳回”胶囊，使战队名称呈现更加纯粹、大方；
- 队长、报名提交时间与所属赛事等元信息采用点阵竖线分割，文字颜色统一使用平台规范描述色 `var(--v-text-desc, #444444)`；
- 保留针对历史驳回状态（`AUDIT_REJECTED`）的警告横幅（带 `CloseCircleOutline` 图标与柔和警示背景）。

### 2.3 名册头部人数与限制规则对齐 (`.roster-header`)
- 扩展响应式计算属性：
  - `detailMemberCount`：计算非领队成员数量；
  - `detailLeaderCount`：计算领队成员数量；
  - `compMinMembers` / `compMaxMembers` / `compMaxLeaders` / `compMaxTotalRows`：自当前赛事元数据 `props.initialData` 提取限制规则；
- 渲染与 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 1:1 对齐的人数展示条：
  ```
  当前成员: 3 / 限制: 1 ~ 4 人 (领队: 1 / 1 人) （名册上限: 5 人）
  ```

### 2.4 名册表格结构与胶囊视觉调整 (`.roster-table`)
- 表格容器宽度设定为 `min-width: 1240px;`，与 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 完全一致，保证在宽屏与小屏滚动下各列均具有充裕间距；
- 表头样式统一：浅灰/暗夜沉浸底色、粘性置顶（`sticky top: 0`）、字重 600、文字截断防溢出；
- 单元格内边距调整为 `padding: 6px 8px; vertical-align: middle;`，队长行添加 `tr.captain-row` 专属底色高亮；
- **成员名册胶囊与字段排版优化**：
  1. **成员姓名 / 账号**（155px）：26px 圆形头像 + 真实姓名（13px/600）+ `@username`（11px 辅助文本）；
  2. **性别**（70px 居中）：**去除刺眼的红/蓝状态胶囊，改为优雅的普通文本展示**（`.gender-text`，男 / 女 / —），消除单行内过多状态徽标带来的视觉疲劳；
  3. **队内角色**（85px 居中）：**胶囊文案精简**为“队长 / 领队 / 队员”（战队队长：primary / 带队领队：warning / 参赛队员：default），圆角微型胶囊；
  4. **学号 / 工号**（130px）：`Fira Code` 等宽字体展示；
  5. **联系电话**（135px）：`Fira Code` 等宽字体展示；
  6. **电子邮箱**（165px）：支持长文本 Tooltip 提示；
  7. **身份证件号码**（175px）：`Fira Code` 等宽字体配轻质感徽标底色；
  8. **所属院系**（140px）：标准描述文本配 Tooltip；
  9. **加入状态**（95px 居中）：保留圆角状态胶囊（采用与 `TeamFormModal` 完全同构的 `getMemberStatusTag` 逻辑，映射已加入 success、待确认 warning、已拒绝 default 等）；
  10. **加入时间**（105px 居中）：本地格式化日期展示。

### 2.5 底部操作栏对齐与无缝衔接 (`.modal-actions`)
- 彻底移除 `<template #footer>` 插槽，将 `.modal-actions` 移入主体容器 `.team-detail-content` 底部，彻底消除冗余边框与内边距断层；
- 左侧 `.actions-left`：展示“当前审核状态：”及状态胶囊标签；
- 右侧 `.actions-right`：提供“关闭”常规按钮、“驳回报名”警示按钮（`type="error" secondary`）与高对比度主要操作按钮“通过资质审核”（`.theme-primary-btn`）；
- 主要按钮遵循平台设计规范：亮色模式下纯黑 `#000000` 背景、纯白 `#ffffff` 文字；夜间模式反色倒置纯白底色、曜黑文字。

### 2.6 黑夜模式深度适配 (Dark Mode Tokens)
- 遵循 `view-Rules.md` 与 `theme.scss` 规范，对 `.team-detail-content` 增加 `html.dark, [data-theme='dark']` 深度适配：
  - 卡片与表头底色接入 `--v-bg-muted`（`#12141c`）；
  - 边框采用细微透光半透明线条（`rgba(255, 255, 255, 0.09)`）；
  - 队长行微光高亮（`rgba(255, 255, 255, 0.03)`）；
  - 身份证 Badge 采用沉浸式半透明白底（`rgba(255, 255, 255, 0.06)`）；
  - 主要操作按钮反色亮显为纯白底黑字。

---

## 3. 验证与测试结果

1. **静态类型检查 (`vue-tsc`)**：
   - 执行 `npx vue-tsc --noEmit`，零类型报错，所有 Props、Emits、计算属性及类型安全均 100% 校验通过。
2. **工程打包构建 (`npm run build`)**：
   - 执行 `npm run build`，Vite 打包构建耗时 5.57s 成功输出产物，无任何样式语法错误或 Rollup 构建异常。
3. **影响范围评估**：
   - 改动仅限定在后台赛事详情页队伍审核标签页（[`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)）的详情模态框，未对任何后端接口及其他组件造成副作用。
