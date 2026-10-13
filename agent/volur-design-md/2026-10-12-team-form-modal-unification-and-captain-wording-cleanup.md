# 竞赛队伍模态框统一合并（TeamFormModal）与详情页“队长”文案清理施工记录

## 1. 变更背景与需求概述

在 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中：
1. **组件职责割裂与能力缺失**：此前队伍创建报名与队伍查看被拆分成了两个组件（`TeamSignupModal.vue` 和 `TeamDetailModal.vue`），导致队长在战队审核通过或报名后无法在线更新队伍资料及成员信息（缺少编辑态）；而经典前端（`src/volur-lite-view`）中经过高度打磨的 `TeamFormModal.vue` 能够通过单一组件同时承载战队创建报名（`create`）、战队信息查看（`view`）以及战队信息编辑更新（`edit`）的全生命周期流程；
2. **“队长”文案过度暴露与视觉冗余**：在竞赛详情视图（`CompetitionDetailView.vue`）中，队伍卡片和规则说明多处出现“队长”字样（例如队伍状态旁的“队长”角标、报名提示“。队长填报后全队即可生效。”等），破坏了简洁界面的视觉体验，且与经典前端仅在非队长（领队/队员）场景下展示角标的设计语言脱节；
3. **重构目标**：
   - 彻底删除 `CompetitionDetailView.vue` 中所有“队长”字样，对齐经典前端角色徽标仅在领队/普通队员时渲染的标准；
   - 参考经典前端 `src/volur-lite-view/src/views/competition/components/TeamFormModal.vue`，将 `TeamSignupModal.vue` 与 `TeamDetailModal.vue` 完整统一合并为全新的 `TeamFormModal.vue`；
   - 彻底删除旧的 `TeamSignupModal.vue` 与 `TeamDetailModal.vue` 组件，保持单一真实数据源。

---

## 2. 核心改动明细

### 2.1 `CompetitionDetailView.vue` 中“队长”字样全量清理
- **报名说明文本**：将 `。队长填报后全队即可生效。` 优化为 `。填报后全队即可生效。`；
- **战队角色徽标**：将此前直接显示 `'队长'` 的逻辑重构为仅在非队长成员时显示身份标签：
  ```html
  <span
    v-if="myTeam.myRole && myTeam.myRole !== 'CAPTAIN'"
    class="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-google-blue dark:bg-blue-950/60 dark:text-google-blue-dark"
  >
    {{ myTeam.myRole === 'LEADER' ? '领队' : '队员' }}
  </span>
  ```
- **规则与须知列表**：
  - 组队规模说明：`<li>队长创建战队后可在线发送队员邀请并管理成员</li>` 调整为 `<li>创建战队后可在线发送队员邀请并管理成员</li>`；
  - 审核说明：`<li>队伍报名审核: {{ competition.requireTeamAudit ? '需平台管理员人工审核通过' : '队长创建战队即审核通过' }}</li>` 调整为 `<li>队伍报名审核: {{ competition.requireTeamAudit ? '需平台管理员人工审核通过' : '创建战队即审核通过' }}</li>`。

### 2.2 统一 `TeamFormModal.vue` 组件构建
创建 `src/volur-lite-view-NEXT/src/views/competition/components/TeamFormModal.vue`，实现一站式队伍全生命周期管理：
1. **多态支持 (`mode?: 'create' | 'view' | 'edit'`)**：
   - `create` 模式：初始化当前用户为队长填报，输入控件全部可用，提交调用 `signupTeamApi`；
   - `view` 模式：回填已有战队信息与成员名册，控件只读禁用（密码/身份证自动脱敏展示），队长可见“解散战队”与“编辑战队信息”入口；
   - `edit` 模式：从查看态一键进入，开放队伍单位、类型、简介及成员/领队增删改（战队名称在已有队伍下保持锁定），提交调用 `updateMyTeamApi`；
2. **完整参赛成员与领队老师表格**：
   - 参赛成员名册（姓名、性别、学号/工号、手机号、邮箱、身份证、学院/部门、班级/职位、加入状态）；
   - 领队/指导老师名册折叠与动态添加；
   - 支持已拒绝/已邀请成员的“重新邀请”（`reinviteMemberApi`）与“移除”（`cancelInvitationApi`）；
   - 队长支持一键解散战队（`deleteMyTeamApi`，带二次确认弹窗）；
3. **严格表单与业务规则校验**：
   - 队伍人数上下限与领队人数上限校验；
   - 手机号格式（`isValidChinaPhone`）、身份证号格式（`isValidIdCard`）、邮箱格式及成员间重复性查重。

### 2.3 彻底移除旧组件并完成调用方切换
- 删除物理文件：
  - `src/volur-lite-view-NEXT/src/views/competition/components/TeamSignupModal.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/components/TeamDetailModal.vue`
- 更新 `CompetitionDetailView.vue`：
  - 引入 `TeamFormModal`；
  - 统一通过 `teamFormModalVisible` 与 `teamFormModalMode` 控制模态框弹出与流转。

---

## 3. 质量与安全验证

- **零注释原则合规**：代码文件保持绝对纯净，未添加任何行内注释；
- **构建测试验证**：运行 `npx vite build` 构建通过，零语法与样式错误，所有 chunk 正确生成。
