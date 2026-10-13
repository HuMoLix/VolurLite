# VTable 与排行榜多级表头与内容排版样式重构报告

## 1. 变更背景与需求概述

在 `volur-lite-view-NEXT` 平台中，[`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue) 作为基础表格组件，被广泛应用于战队列表、赛事成绩、排行榜及各类管理端视图。为统一平台视觉规范，基于 [`CtfCompetitionScoreboardView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionScoreboardView.vue) 排行榜场景，对 `VTable` 的全局排版规范进行系统性强化：
1. **统一字体规范**：整个表格组件严格以 `Inter, sans-serif` 作为 Font-family；
2. **表头色彩与分级字号**：
   - 默认常规表头：`color: #202124`（暗色模式对齐 `#e3e3e3`），`font-size: 13px`；
   - 切分后的分组标题（以 CTF 排行榜赛题分类为例）：
     - 一级标题（赛题分类）：`font-size: 12px; font-weight: 700;`；
     - 二级标题（赛题名称）：`font-size: 12px; font-weight: 500;`；
     - 三级标题（赛题分数）：`font-size: 10px; font-weight: 500;`；
3. **表格内容样式**：表格数据内容统一采用 `color: #3c4043`（暗色模式 `#e8eaed`），`font-weight: 500`。

---

## 2. 核心改动点与实现细节

### 2.1 VTable 组件样式重构 (`VTable.vue`)
- **文件**：[`src/components/common/VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue)
- **改动说明**：
  1. **全链路字体穿透**：
     - 在 `.v-table` 及 `:deep(.n-data-table)` 中对 `.n-data-table-wrapper`、`.n-data-table-table`、`.n-data-table-thead`、`.n-data-table-tbody`、`.n-data-table-th`、`.n-data-table-td` 显式声明 `font-family: Inter, sans-serif;`；
  2. **表头主题变量与 CSS 覆盖**：
     - `tableThemeOverrides` 中 `thTextColor` 统一调整为 `isDark ? '#e3e3e3' : '#202124'`，`thFontWeight` 设定为 `'500'`；
     - 常规 `.n-data-table-th` 样式：`font-size: 13px; color: #202124; font-weight: 500;`；
  3. **多级/切分表头层级适配**：
     - **一级标题**（`thead tr:not(:last-child) > th`、`th[colspan]:not([colspan="1"])` 或挂载 `.v-table-header-l1`/`.header-l1`）：应用 `font-size: 12px !important; font-weight: 700 !important; color: #202124;`（暗色模式 `#e3e3e3`）；
     - **二级标题**（`thead tr:nth-child(n+2) > th`、`thead tr:last-child > th:not([rowspan])` 或挂载 `.v-table-header-l2`/`.header-l2`）：应用 `font-size: 12px !important; font-weight: 500 !important; color: #202124;`（暗色模式 `#e3e3e3`）；
     - **三级标题**（挂载 `.v-table-header-l3`/`.header-l3`/`.scoreboard-score`/`.header-score`/`.sub-score`）：应用 `font-size: 10px !important; font-weight: 500 !important;`；
  4. **表格数据内容统一**：
     - `tdTextColor` 统一为 `isDark ? '#e8eaed' : '#3c4043'`；
     - `.n-data-table-td` 样式：`color: #3c4043; font-weight: 500;`（暗色模式 `color: #e8eaed; font-weight: 500;`）。

### 2.2 排行榜视图同步校准 (`CtfCompetitionScoreboardView.vue`)
- **文件**：[`src/views/competition/tabs/CtfCompetitionScoreboardView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionScoreboardView.vue)
- **改动说明**：
  1. 将 `.scoreboard-cell-text` 与 `.scoreboard-score-cell` 的 `font-weight: 300 !important;` 重构调整为 `font-weight: 500 !important;`，暗色模式文本颜色对齐 `#e8eaed`；
  2. 将 `.scoreboard-score`（赛题分数三级标题）由 `font-weight: 400 !important;` 升级为 `font-weight: 500 !important;`；
  3. `.n-data-table-td` 显式设置 `font-weight: 500;`，确保全屏与局部状态下的排版规范完全统一。

---

## 3. 验证与审计

1. **纯粹代码与零注释铁律**：所有业务代码文件保持极致纯净，无任何冗余注释；
2. **类型检查 (`vue-tsc --noEmit`)**：类型校验通过，零报错；
3. **构建测试 (`npm run build`)**：打包顺利完成。
