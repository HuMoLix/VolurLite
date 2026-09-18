# VolurTableCard 快速跳页对齐修复与平台默认跳页机制重构记录

## 1. 变更背景与需求
- **问题一（样式污染与对齐失衡）**：在 [`AdminCtfChallengeView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeView.vue) 中开启外挂独立底部分页条（`standalone-pagination`）后，分页末尾渲染的“跳至 X 页”快速跳转输入框高度（36px）明显高于前后翻页按钮及页码项（28px），基线严重错位且行高失衡。
- **问题二（分页能力二元分化）**：在 [`AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue)、[`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue) 与 [`AdminBroadcastView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue) 等视图中，使用的是数据表格内置受控分页（`n-data-table` 内置分页），因 Naive UI `PaginationProps` 的 `showQuickJumper` 默认值为 `false`，导致这些页面均无跳页功能。
- **重构目标**：
  1. 修复快速跳转输入框的样式污染，确保输入框与分页按钮尺寸基线严格对齐；
  2. 采用“全平台默认开启快速跳页”规范，在组件底层统一收敛该能力，保持全平台行为一致。

---

## 2. 核心改动点说明

### 2.1 修复工具栏样式污染，规范分页器快速跳转器尺寸与对齐
- **约束输入框微调样式作用域**：
  在 [`VolurTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurTableCard.vue) 中，将原本作用于整个卡片根层级的 `:deep(.n-input:not(.n-input--textarea))` 严格收敛至 `.table-toolbar-custom, .filter-header`，杜绝其越界污染卡片内部底部分页器的输入框。
- **底部分页器专属样式防护**：
  针对外挂底部分页及内置分页（`:deep(.n-pagination)`），为其 `.n-pagination-quick-jumper` 与其子级 `.n-input` 设定精准的高度匹配（`--n-height: var(--n-item-size, 28px)`、`height: 28px`、`line-height: 28px`、文本居中、紧凑内边距），确保在任何页面下快速跳页输入框与页码块、上一页下一页按钮在垂直轴上保持像素级居中对齐。
- **优化 Title 侦听器查询范围**：
  将 `updateAllInputsTitle()` 内部的 DOM 查询收缩为 `.filter-header .n-input, .table-toolbar-custom .n-input`，消除对快速跳转数字框的不必要侦听。

### 2.2 平台默认跳页方案重构（内置受控分页与独立分页统一标准化）
- **Props 拓展与默认值收敛**：
  在 `VolurTableCardProps` 接口中显式增加 `showQuickJumper?: boolean`，并赋予默认值 `showQuickJumper: true`。
- **独立外挂分页绑定**：
  将独立分页模板内的硬编码改为受控绑定 `:show-quick-jumper="showQuickJumper"`。
- **内置分页默认能力注入**：
  在 `tablePagination` 计算属性中，当处理外部传入的 `pagination` 配置对象时：
  若外部未显式配置 `showQuickJumper`（即 `paginationConfig.showQuickJumper === undefined`），自动注入继承的 `props.showQuickJumper ?? true`；若外部显式配置了 `false`，则尊重业务端个性化设置。
- **典型管理视图显式同步**：
  在 `AdminUserView.vue`、`AdminCompetitionListView.vue` 与 `AdminBroadcastView.vue` 的 `pagination` 响应式对象中显式增加 `showQuickJumper: true`，保持类型与声明意图的高度透明。

---

## 3. 安全隐患与兼容性修复说明
1. **防止事件丢失或冲突**：
   Naive UI `n-pagination` 的快速跳转功能在输入合法页码并确认（回车或失焦）时，统一触发 `update:page`（或 `onChange`）事件。由于全平台 `VolurTableCard` 使用方均已规范监听 `@update:page`，开启跳页后与现有分页查询逻辑无缝集成，不会引入事件冲突。
2. **渐进增强与按需关闭**：
   保留业务视图对跳页能力的完全控制权。若后续某些特殊卡片因宽度极端受限需要隐藏跳页，仅需在组件上声明 `:show-quick-jumper="false"` 或在 `pagination` 对象中指定 `showQuickJumper: false` 即可优雅关闭。

---

## 4. 影响范围
- **平台自研组件库**：[`src/public/volurEx/components/VolurTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurTableCard.vue)
- **管理端业务视图**：
  - 用户管理：[`AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue)
  - 竞赛列表：[`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue)
  - 广播历史：[`AdminBroadcastView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue)
  - 赛题管理（CTF/理论）、容器管理、任务管理、镜像管理等全平台使用 `VolurTableCard` 的所有视图统一生效。
- **构建校验**：运行 `vue-tsc --noEmit && vite build` 检查 0 错误，前端构建完全通过。
