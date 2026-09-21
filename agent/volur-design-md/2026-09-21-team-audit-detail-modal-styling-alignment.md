# 管理后台战队详情模态框排版与 TeamFormModal 样式对齐记录

## 1. 变更背景
为了统一赛事报名前端体验与管理后台审查视图的界面风格与信息架构，将 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 经过多次迭代打磨的左右 50% 顶部分栏、竞赛须知卡片、参赛成员与领队名册分流表格、动态工号/学号/部门/学院列头以及文字化角色标签的全新设计规范，完整复刻至管理端 [`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 中的“查看队伍详情”（`team-detail-modal`）模态框。

## 2. 改动详情

### 2.1 模板排版重构 (`TeamAuditTab.vue`)
1. **历史驳回提示横幅 (`reject-alert-banner`)**：
   - 处于审核被驳回（`AUDIT_REJECTED`）且存在驳回原因（`auditReason`）时，在弹窗最上方以高亮警告色块呈现历史原因。
2. **顶部双栏布局 (`team-meta-container`)**：
   - **左栏 50% (`meta-left-panel`)**：包含队伍基础属性，以优雅紧凑的表单展示：
     - Row 1：队伍头像 (`UserAvatar`, 52px) + 队伍名称只读框；
     - Row 2：队伍单位 + 单位类型只读下拉框（支持学校、企业单位、军工、能源、金融、科研院所、其他）；
     - Row 3：队伍简介（多行文本展示，若空显示“暂无简介”）。
   - **分割线 (`meta-divider`)**：自适应拉伸垂直分割线（响应式下折叠）。
   - **右栏 50% (`meta-right-panel`)**：嵌入“竞赛报名要求与须知”卡片（`requirements-card`）：
     - 报名时间区间、比赛时间区间；
     - 人数要求：去除 `(含队长)` 冗余字样，直接展示 `每队 X ~ Y 人`；
     - 领队要求：去除 `(可选填)` 冗余字样，直接展示 `最多 N 名指导老师/领队`；
     - 特别说明：管理员自定义要求滚动容器，无内容时提示“暂无额外补充要求”。
3. **参赛成员与领队分流表格 (`roster-header` / `roster-table`)**：
   - 将原有单一名册拆分为两套分流表格：
     - **参赛成员名册**：根据 `detailMembersOnly`（非领队角色），队长行置顶高亮；包含姓名/账号、性别、角色（普通文字 `role-text` 标签，区分队长与队员）、学号/工号、手机号、邮箱、身份证号、学院/部门、班级/职位及加入状态；
     - **领队/指导老师名册**：仅在存在领队数据时（`detailLeadersOnly.length > 0`）以虚线分割标题展示，列头自动适配工号/部门/职位或学号/学院/班级。
4. **底部操作栏 (`modal-actions`)**：
   - 左侧展示当前审核状态 Tag、队长账号、报名提交时间；
   - 右侧保留关闭按钮与审核操作按钮（通过/驳回）。

### 2.2 脚本逻辑补全 (`TeamAuditTab.vue`)
1. 导入修复：引入 `formatAvatarUrl`（来自 `@/utils/imageCache`）、`UserAvatar`、`InformationCircleOutline`；
2. 计算属性补充：
   - `detailTeamDisplayAvatarUrl`：通过头像缓存工具解析战队头像可访问地址；
   - `detailMembersOnly`：收敛非领队成员，且队长自动前置排序；
   - `detailLeadersOnly`：收敛领队/指导老师角色成员；
   - `organizationTypeOptions`：提供 7 种单位类型的键值对；
   - `formatDateTime`、`formattedRegTimeRange`、`formattedCompTimeRange`、`adminCustomRequirements`：动态从 `props.initialData` 格式化提取并呈现；
3. 清理已废弃的单列表排序变量 `sortedDetailMembers` 与未使用的 `compMaxTotalRows`。

### 2.3 SCSS 样式与暗色模式全面升级
1. 亮色模式：严格遵守 `#000000` 纯黑按钮、`#444444` 标签描述文本、表格吸顶头等视觉规范；
2. 暗色模式（`html.dark, [data-theme='dark']`）：完整适配曜石黑阶梯背景、微透明分割线、文字反色与纯白主按钮高对比规范。

## 3. 验证情况
- 执行前端完整构建命令 `npm run build`（`vue-tsc --noEmit && vite build`），0 error，顺利通过打包。

## 4. 影响范围
- 前端管理后台赛事队伍审核选项卡：[`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)
- 无破坏性变更，与后端 VO 及前端统一实体完全契合。
