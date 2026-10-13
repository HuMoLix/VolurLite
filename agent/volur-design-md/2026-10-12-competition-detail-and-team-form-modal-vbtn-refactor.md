# 赛事详情与队伍管理模态框 VBtn 统一重构记录

## 1. 变更背景与需求
为了统一平台按钮的视觉规范与交互体验，遵循 Google Material / Workspace 风格标准，消除零散的样式类覆盖及不一致的按钮配色行为：
1. 在通用按钮组件 `VBtn` (`src/volur-lite-view-NEXT/src/components/common/VBtn.vue`) 及其类型定义 `types.ts` 中新增 `warning` 配色等级（支持 `#ea43350d` 背景、`#ea43354d` 边框及 `#ea4335` 文字色，并对齐 Hover、Disabled 及夜间暗黑模式下的色彩渐变）。
2. 在 `CompetitionDetailView.vue` 赛事详情视图中：
   - 将原“查看战队信息”按钮统一重构为通用组件 `<VBtn clz="normal" :left-icon="EyeOutline">`，文案规范化为“查看队伍信息”。
   - 将“进入竞赛”行动按钮统一重构为 `<VBtn clz="primary" :left-icon="RocketOutline" size="large">`。
3. 在 `TeamFormModal.vue` 队伍管理与报名模态框中：
   - 将原“解散战队”按钮文案调整为“解散队伍”，且统一使用 `<VBtn clz="warning" :left-icon="TrashOutline">`。
   - 将“编辑战队信息”按钮重构为 `<VBtn clz="primary" :left-icon="CreateOutline">`，文案规范化为“编辑队伍信息”。
   - 将创建模式与编辑模式下的“提交报名”与“提交更新”主操作按钮重构为 `<VBtn clz="primary" :loading="submitting">`。
   - 将模态框中的其余操作按钮（“添加成员行”、“添加领队行”、“取消”、“关闭”、“取消修改”）统一重构为 `<VBtn clz="normal">`。
   - 清理模态框组件中冗余的 `.competition-btn`、`.competition-btn-primary`、`.competition-btn-danger` 局部样式。

---

## 2. 详细改动点

### 2.1 通用组件类型扩展与样式增强
1. **`src/volur-lite-view-NEXT/src/components/common/types.ts`**：
   - 将 `export type VBtnClz = 'primary' | 'normal'` 扩展为 `export type VBtnClz = 'primary' | 'normal' | 'warning'`。
2. **`src/volur-lite-view-NEXT/src/components/common/VBtn.vue`**：
   - 新增 `.v-btn-warning` 样式规则：
     - 日间模式：`background-color: #ea43350d`、`border: 1px solid #ea43354d`、`color: #ea4335`；Hover 触发时背景为 `rgba(234, 67, 53, 0.1)`、边框为 `rgba(234, 67, 53, 0.45)`。
     - 夜间模式：`background-color: rgba(242, 139, 130, 0.08)`、`border-color: rgba(242, 139, 130, 0.3)`、`color: #f28b82`；Hover 触发时背景为 `rgba(242, 139, 130, 0.16)`、边框为 `rgba(242, 139, 130, 0.45)`。
     - Focus 与 Active 状态：边框高亮同步对齐危险色。

### 2.2 赛事详情视图按钮重构 (`CompetitionDetailView.vue`)
1. 导入并在模板中使用 `VBtn` 组件。
2. “查看队伍信息”：替换原原生按钮为 `<VBtn clz="normal" :left-icon="EyeOutline" @click="openTeamDetailModal">{{ t('competition.view_team_btn', '查看队伍信息') }}</VBtn>`。
3. “进入竞赛”：替换原高内联类按钮为 `<VBtn clz="primary" :left-icon="RocketOutline" size="large" @click="enterArena">{{ t('competition.enter_arena', '进入竞赛') }}</VBtn>`。

### 2.3 队伍模态框按钮全量重构 (`TeamFormModal.vue`)
1. 导入并在模板中使用 `VBtn` 组件。
2. 成员与领队动态增行按钮：
   - “添加成员行”：使用 `<VBtn v-if="isEditing" clz="normal" :left-icon="AddOutline" :disabled="isMaxMembersReached" @click="addMemberRow">`。
   - “添加领队行”：使用 `<VBtn v-if="isEditing" clz="normal" :left-icon="AddOutline" :disabled="isMaxLeadersReached" @click="addLeaderRow">`。
3. 模态框底部操作区按钮：
   - “解散队伍”：使用 `<VBtn clz="warning" :left-icon="TrashOutline" :disabled="disbanding">`，同时将 Popconfirm 文案同步调整为“确认解散队伍”及“队伍成员将自动移出”。
   - “编辑队伍信息”：使用 `<VBtn v-if="isCaptain" clz="primary" :left-icon="CreateOutline" @click="startEditing">`。
   - “提交报名”与“提交更新”：使用 `<VBtn clz="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">`，依托 `VBtn` 内置 loading 状态与禁用特性。
   - “取消”、“关闭”、“取消修改”：统一使用 `<VBtn clz="normal" ...>`。
4. 清理组件内原有废弃的 `.competition-btn`、`.competition-btn-primary` 与 `.competition-btn-danger` 局部 CSS 规则，减少样式体积并防止样式冲突。

---

## 3. 规范遵循与代码审计
1. **纯粹代码与零注释原则**：所有被修改的 `.ts` 与 `.vue` 代码文件严格保持 0 行注释。
2. **构建验证**：在 `src/volur-lite-view-NEXT` 运行 `vue-tsc --noEmit && vite build`，编译通过（0 错误 0 告警）。
