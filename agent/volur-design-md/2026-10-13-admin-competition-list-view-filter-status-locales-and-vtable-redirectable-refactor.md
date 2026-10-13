# 管理端竞赛列表视图状态过滤、语言包国际化与 VTable redirectable 链接样式重构

## 1. 变更背景与诉求

在 `src/volur-lite-view-NEXT` 项目中，针对管理端竞赛管理列表页面（`AdminCompetitionListView.vue`）及其底层通用表格（`VTable.vue`）和多语言系统实施了更深层次的体验与规范对齐重构：
1. **比赛状态过滤支持**：在顶栏“全部赛制”过滤框后，缺少赛事状态的过滤维度，需要引入 `VSelection` 组件支持按比赛状态（草稿、已发布、报名中、进行中、已封榜、已结束、已归档）进行实时筛选；
2. **多语言与选项文本规范化**：
   - 全局与各语言包中将竞赛搜索框占位文本从“搜索竞赛标题或短链接...”精简为“搜索竞赛标题”；
   - 去除赛制选项中括号内的中英文混排后缀（例如取消 `CTF 解题赛 (Jeopardy)` 这种形式，纯化为标准的 `CTF 解题赛`、`理论答题赛`、`AWD 攻防对抗`、`AWD+ 综合攻防`、`ISW 靶场实战`）；
3. **平台统一可跳转文本样式 `redirectable` 实现**：
   - 在平台通用组件 `VTable.vue` 中封装通用的 `.redirectable` 类，用于标识表格内可点击跳转的标题/链接文本；
   - 链接色必须严格采用平台设计系统定义的 `--color-v3-text-link`（来自 `src/styles/vcss.scss`，随浅暗色模式自适应切换）；
   - Hover 时显示下划线（`text-decoration: underline`），光标为 `pointer`，标题字重严格控制为 `font-weight: 500`；
   - 点击该标题可直接跳转至管理编辑页（`AdminCompetitionEditView.vue`）；
   - 标题下方的 slug 文本颜色必须严格使用 `--color-v3-text-var`，字号固定为 `12px`。

---

## 2. 变更内容与改动点

### 2.1 语言文件（Locales）规范化

- **`admin.json` (zh-CN / en-US / zh-TW)**：
  - 更新 `competitions.search_placeholder` 为纯粹的 `"搜索竞赛标题"`（英文为 `"Search by title"`）；
- **`competition.json` (zh-CN / en-US / zh-TW)**：
  - 增补 `"status_all"`（`"全部状态"` / `"All Status"`）；
  - 规范化 `type_theory`（`"理论答题赛"`）与 `type_isw`（`"ISW 靶场实战"`）。

### 2.2 VTable.vue 通用组件增强：`.redirectable` 样式

在 `src/components/common/VTable.vue` 样式中增加平台统一跳转类 `.redirectable`：
- **颜色**：`color: var(--color-v3-text-link) !important;`（浅色 `#2483e2`，暗色 `#87a9ff`）；
- **字重**：`font-weight: 500 !important;`；
- **交互**：`cursor: pointer;`、`transition: text-decoration 0.15s ease-in-out, color 0.15s ease-in-out;`；
- **Hover 态**：`text-decoration: underline !important;`；
- 支持穿透应用于 Naive UI 内部单元格以及外层槽位插槽。

### 2.3 AdminCompetitionListView.vue 重构

- **顶栏新增状态过滤框**：
  - 紧随 `filterType` 之后增加 `filterStatus` 下拉选择框（绑定 `VSelection` 组件，具备清空与即时过滤能力）；
  - `statusSelectOptions` 包含 `全部状态`、`草稿`、`已发布`、`报名中`、`进行中`、`已封榜`、`已结束`、`已归档`；
  - `fetchData` 统一联动 `filterType` 与 `filterStatus` 进行数据筛选，`handleReset` 同步清空两项筛选项；
- **赛制选项去混排**：
  - `typeSelectOptions` 彻底取消括号及英文内容，纯化为纯中文语义；
- **竞赛标题与 Slug 展示重构**：
  - 竞赛标题绑定 `class="redirectable"`，`font-weight: 500`，绑定点击事件跳转到 `/admin/competitions/${row.id}/edit`；
  - Slug 文本颜色设置 `--color-v3-text-var`，字号为 `12px`。

---

## 3. 影响范围

- **修改文件**：
  - `src/locales/zh-CN/admin.json`
  - `src/locales/en-US/admin.json`
  - `src/locales/zh-TW/admin.json`
  - `src/locales/zh-CN/competition.json`
  - `src/locales/en-US/competition.json`
  - `src/locales/zh-TW/competition.json`
  - `src/components/common/VTable.vue`
  - `src/views/admin/competition/AdminCompetitionListView.vue`
- **影响视图**：
  - 管理后台 - 竞赛管理页面 (`/admin/competitions`)。
- **兼容性与类型检查**：
  - 全工程执行 `npx vue-tsc --noEmit` 零报错通过，无类型兼容性隐患。
