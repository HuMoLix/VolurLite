# VPagination 分页组件设计与 CTF 赛事视图集成方案

## 1. 变更背景与目标
为实现平台通用组件库的自主收敛，提升整体 UI 的统一性并深度契合 Google Workspace / Material Design 视觉风格：
1. 以 Naive UI 的 `NPagination` 为功能基准模型，自主研发平台通用分页组件 `VPagination.vue`（收敛于 `@/components/common`）；
2. 在 `VPagination.vue` 内部，深度集成已有的自主研发基础组件：
   - 页码按钮与前后翻页导航按钮统一采用 `VBtn.vue`；
   - 快速跳页（Quick Jumper）数字输入框统一采用 `VInput.vue`；
   - 每页容量切换下拉选框（Size Picker）统一采用 `VSelection.vue`；
3. 将 CTF 赛事大厅队伍详情页（`src/views/competition/tabs/CtfCompetitionTeamView.vue`）中答题记录下方的 `NPagination` 整体替换为 `VPagination`；
4. 在 CTF 赛事排行榜（`src/views/competition/tabs/CtfCompetitionScoreboardView.vue`）的表格下方集成 `VPagination`，支持海量队伍榜单的分页流转展示与大容量切换；
5. **解耦表格与分页容器层级**：纠正此前将 `VPagination` 写入 `VTable` 内置 `#footer` 插槽导致分页栏被表格边框（`.v-table` / `.v-data-table`）硬性包裹的缺陷，将 `VPagination` 提升至外层卡片级容器中，独立呈现于列表下方，彻底去除表格边框的捆绑约束。

---

## 2. 核心架构与设计实现

### 2.1 组件架构 (`src/components/common/VPagination.vue`)
- **双向数据绑定**：
  - 支持 `v-model:page`（或 `v-model:page-size`），并兼容原生 `page`、`pageSize` 受控传入；
  - 自动响应 `itemCount` 与 `pageCount`，精确计算总页数；
  - 具备防越界约束机制：当切换 `pageSize` 导致总页数变小时，自动将当前页限制或重置为第 1 页。
- **分页槽位算法 (Slotted Window)**：
  - 针对 `pageSlot`（默认 7 槽位）自动自适应边界：
    - 当总页数 <= `pageSlot` 时，全量展示所有连续页码；
    - 当处于起始页段时，展示前段页码、快进按钮（`...`）及末页；
    - 当处于中间页段时，展示首页、快退按钮、当前页滑动窗口、快进按钮及末页；
    - 当处于末尾页段时，展示首页、快退按钮及末尾连续页码；
  - 快进 / 快退点击可快速向前 / 后步进 5 页。
- **子组件协同集成**：
  - **按钮 (`VBtn`)**：当前激活页采用 `clz="primary"`（Google Blue `#1a73e8`，暗色高对比 `#8ab4f8`），非激活页采用 `clz="normal"` 搭配透明悬浮灰底（`#f1f3f4` / `#303134`）；
  - **选项框 (`VSelection`)**：配置 `pageSizeOptions` 供用户快速切换 `[10, 20, 50, 100]` 条/页；
  - **输入框 (`VInput`)**：支持用户输入页码回车（Enter）或失焦（blur）快速跳转指定页码。
- **尺寸体系与主题响应**：
  - 提供 `small` (28px)、`medium` (32px)、`large` (38px) 三档排版规格；
  - 自动响应 `appStore.theme` 并在亮色/深色主题之间平滑过渡，字体统一锁定 `Inter, sans-serif`。

### 2.2 类型与导出配置
- 在 `src/components/common/types.ts` 中导出 `VPaginationSize`、`VPaginationSizeOption`、`VPaginationProps`；
- 在 `src/components/common/index.ts` 中导出 `VPagination`。

### 2.3 队伍视图解耦与集成 (`CtfCompetitionTeamView.vue`)
- 将 `VPagination` 从 `VTable` 的 `#footer` 槽位移出，作为与 `VTable` 平级的同级子元素置于外层 Section 卡片中：
  ```vue
  <VTable ... />

  <div v-if="teamSolves.length > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
    <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
      显示第 {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, teamSolves.length) }} 条，共 {{ teamSolves.length }} 条
    </div>
    <VPagination
      v-model:page="currentPage"
      v-model:page-size="pageSize"
      :item-count="teamSolves.length"
      :page-sizes="[10, 20, 50]"
      show-size-picker
      show-quick-jumper
      size="small"
    />
  </div>
  ```

### 2.4 排行榜列表解耦与集成 (`CtfCompetitionScoreboardView.vue`)
- 在榜单状态机中增加 `currentPage`（默认 1）与 `pageSize`（默认 20）；
- 通过 `paginatedScoreboard` 计算属性对榜单切片后提供给 `VTable` 渲染，保留 `row.rank` 全局精准排名字段；
- 顶部趋势折线图（`VCtfScoreboardChart`）继续接收完整榜单数据，保证全局趋势不受分页切片影响；
- 将 `VPagination` 置于 `VTable` 外部的卡片底栏中，彻底摆脱表格自身的边框限制：
  ```vue
  <VTable ... />

  <div v-if="scoreboard.length > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
    <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
      显示第 {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, scoreboard.length) }} 支队伍，共 {{ scoreboard.length }} 支队伍
    </div>
    <VPagination
      v-model:page="currentPage"
      v-model:page-size="pageSize"
      :item-count="scoreboard.length"
      :page-sizes="[10, 20, 50, 100]"
      show-size-picker
      show-quick-jumper
      size="small"
    />
  </div>
  ```
- 配合 `watch(pageSize, () => { currentPage.value = 1 })` 避免越界。

---

## 3. 影响范围与规范对齐
- **模块对齐**：完全遵循 `GEMINI.md` 与 `view-NEXT-rules.md`；
- **视觉分离**：表格仅聚焦数据行与列的网格呈现，底部分页栏作为独立卡片底栏布局，层级分明；
- **零注释原则**：所有 `.vue` 与 `.ts` 文件无冗余行内解释性注释，保证极致纯净。
