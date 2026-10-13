# Admin CTF 竞赛赛题详情视图文案及选择器下拉 z-index 修复文档

## 1. 变更背景与问题分析

在 `AdminCtfCompetitionChallengeDetailView.vue`（CTF 竞赛赛题管理详情视图）中存在以下交互与体验问题：
1. **文案冗余与不规范**：列表标题显示为“已挂载赛题列表 (0)”，操作按钮显示为“挂载题目”，与整体后台极简克制与“添加题目 / 赛题列表”的标准术语不一致。
2. **选择器下拉弹层截断隐患 (Modal Overflow & z-index)**：
   - 在添加题目对话框（`VModal`）中，“选择题库赛题”组件基于 `VSelection`（底层包装 Naive UI `NSelect`）。
   - 在 `NModal` 容器上下文中，Naive UI 内部 `useAdjustedTo` 机制默认将下拉菜单传送目标绑定至模态框主体（`.v-modal-panel`）；
   - 由于 `.v-modal-panel` 具有圆角与隐藏溢出约束（`overflow: hidden`），且内容区域 `.v-modal-container` 具有固定最大高度与局部滚动约束，当题库候选赛题列表较多展开下拉菜单时，菜单超出模态框高度部分被直接裁剪截断，导致选手无法看到或选择下方的题目；
   - 若仅配置传送目标为 `body` 而未提升菜单层级，由于模态框自身层级为 2000，下拉菜单在全局图层中可能被遮挡在遮罩层下方。

---

## 2. 核心改动点

### 2.1 赛题详情视图文案与配置调整 (`AdminCtfCompetitionChallengeDetailView.vue`)
- 将列表顶部标题从 `{{ t('admin.competitions.mounted_challenges', '已挂载赛题列表') }} ({{ mountedChallenges.length }})` 精简为 `{{ t('admin.competitions.mounted_challenges', '赛题列表') }}`；
- 将操作按钮文案从 `{{ t('admin.competitions.mount_challenge_btn', '挂载题目') }}` 更新为 `{{ t('admin.competitions.mount_challenge_btn', '添加题目') }}`；
- 在添加题目弹窗中的 `VSelection` 组件显式注入 `to="body"` 与 `:z-index="3000"`，确保下拉菜单逃逸出模态框的溢出隐藏限制，且层级高于模态框（2000）。

### 2.2 通用选择器组件扩展 (`VSelection.vue` & `types.ts`)
- 在 `VSelectionProps` 类型定义中新增 `to?: string | HTMLElement | false`、`zIndex?: number` 及 `menuProps?: Record<string, any>`；
- 在 `VSelection.vue` 中支持 `resolvedTo` 与 `resolvedMenuProps` 响应式计算：
  - 当显式声明 `zIndex` 时，自动合并至底层 `n-select` 的 `menuProps.style.zIndex`；
  - 当未指定 `to` 但指定了 `zIndex` 时，自动推断 `resolvedTo = 'body'`，彻底根除模态框内下拉菜单被截断的通用隐患；
  - 模板中显式绑定 `:to="resolvedTo"` 与 `:menu-props="resolvedMenuProps"`。

---

## 3. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/views/admin/competition/AdminCtfCompetitionChallengeDetailView.vue`
  - `src/volur-lite-view-NEXT/src/components/common/VSelection.vue`
  - `src/volur-lite-view-NEXT/src/components/common/types.ts`
- **验证结果**：
  - 执行 `vue-tsc --noEmit` 类型全量校验 0 错误通过；
  - 完整 `vite build` 生产打包编译通过。
