# CtfCompetitionScoreboardView 单页化重构与 VTable 一行二列表头实现方案设计文档

## 1. 变更背景与诉求分析
在 Volur Lite CTF 赛事大厅中，榜单视图（`CtfCompetitionScoreboardView.vue`）原先采用侧边栏导航选择器（`VCtfScoreboardSelector.vue`）分割出 6 个互斥子视图（积分总榜、比赛概况、解题总榜、题目榜单、单人总榜、题型榜单），导致页面操作割裂、信息层级过深。根据最新设计规范与业务诉求：
1. **删除 `VCtfScoreboardSelector.vue` 组件**：彻底移除侧边栏多视图切换器与冗余多页，仅保留单一核心综合榜单页；
2. **Top 10 队伍积分折线图与统一按钮**：Top 10 队伍积分折线图内所有操作按钮（包括战队显隐切换按钮、全部显示/全部隐藏按钮等）统一采用通用规范组件 `VBtn.vue`；
3. **排行榜列表基于 `VTable.vue` 实现「一行二列」层级表头**：
   - 彻底废除原平铺原生 HTML `<table>`；
   - 采用通用数据表格组件 `VTable.vue`，利用 Naive UI 表头分组机制实现标准的一行二列多级表头：
     - **第一行（第一级表头）**：展示题型大类（如 Web、Pwn、Reverse、Crypto、Misc 等），横向跨越该分类下所有具体赛题；
     - **第二行（第二级表头）**：展示该分类下的具体赛题（如 web1、web2、web3 等）及其分值；
     - **固定前置列**：排名（Rank）、队伍名称（Team Name）、总分（Score）在左侧固定（`fixed: 'left'`），无缝支持横向滚动矩阵；
     - **单元格状态精准呈现**：各队伍在对应题目单元格下展示解出状态（首杀 First Blood 高亮徽章、普通解出徽章带加分、未解出横线占位），并在悬停时展示详尽 Tooltip 信息（解题时间、排名、一血标识）。

---

## 2. 详细方案设计与技术实现

### 2.1 移除侧边栏与实现单一聚焦视图
- 删除文件 `src/components/competition/ctf/VCtfScoreboardSelector.vue`，并清理 `src/components/competition/ctf/index.ts` 中的对外导出；
- `CtfCompetitionScoreboardView.vue` 剥离 `activeView` 状态机与 `<aside>` 侧边栏布局，重构为全宽单页纵向流式结构：
  - 上方：Top 10 队伍解题积分趋势折线图（`VCtfScoreboardChart.vue`）；
  - 下方：综合排行榜矩阵大表（`VTable.vue`）。

### 2.2 Top 10 折线图按钮重构为 VBtn
- 在 `VCtfScoreboardChart.vue` 中引入 `@/components/common` 的 `VBtn`；
- 将战队切换按钮全面重构为 `<VBtn size="small" round clz="normal">`，结合高亮 Ring 与透明度虚线区隔激活/禁用状态；
- 新增快捷批量操作按钮「全部显示」与「全部隐藏」，同样基于 `VBtn` 实现；
- 榜单表格右上角刷新操作统一采用带有加载态的 `<VBtn size="small" clz="normal" round :left-icon="RefreshOutline">`。

### 2.3 基于 VTable 的「一行二列」分组表头与网格实现
利用 `DataTableColumns` 的 `children` 树形结构原生构建两级表头：
1. **基础列**：
   - `rank`：固定列宽 `76px`，左固定，前三名渲染专用勋章（金/银/铜），其他展示等宽排名；
   - `teamName`：最小列宽 `170px`，左固定，支持点击触发队伍选择事件，当前登录队伍渲染「本队」标签；
   - `score`：固定列宽 `95px`，左固定，加粗蓝字高亮展示总分。
2. **题型分类与赛题二级分组列**：
   - 外层遍历 `groupedChallenges` 生成题型列（第一行表头）：`title: catGroup.name`（居中大写题型名称）；
   - 内层 `children` 遍历生成赛题子列（第二行表头）：
     - `title`: 包含赛题标题 `ch.title` 与当前分值 `${ch.currentPoints}p` 的双行信息，并挂载 `VTooltip`；
     - `width`: 固定 `96px`；
     - `render`: 根据队伍在该题的解出记录渲染状态徽章，若解出则展示带有 `CheckmarkCircle` 图标的加分药丸胶囊（一血呈现专属玫红渐变，普通解出呈现翡翠绿渐变），未解出展示 `—` 柔和占位符。
3. **横向滚动与粘性固定**：
   - 动态计算 `tableScrollX = Math.max(900, 350 + challengeCount * 100)`；
   - 确保当题目数量较多时，左侧排名/队伍/总分列稳固吸附，右侧题型网格平滑横向滚动。

### 2.4 主题色彩与规范严格对齐
- 全面遵循 `view-NEXT-rules.md`：
  - 核心主色使用 Google Blue（亮色 `#1a73e8` / 暗色 `#a8c7fa`）；
  - 表格卡片背景亮色 `#ffffff`，暗色 `#292a2d`；
  - 本队行通过 `:deep(.my-team-row)` 注入柔和蓝色高亮背景（`rgba(26, 115, 232, 0.08)`）；
  - 遵循零注释铁律，代码内杜绝冗余行内解释注释。

### 2.5 标题区微调、本队标签移除与全列表居中对齐、纵向边框落地
- 移除排行榜标题旁的队伍数量标注 `(X 支队伍)`，保持顶部标题简洁利落；
- **移除「本队」标签**：战队名称列直接居中展示队伍名称链接，移除内部「本队」Tooltip 药丸标签，仅通过整行柔和蓝色背景底色高亮当前队伍行；
- **全列表项居中对齐**：
  - 将排名、队伍名称、总分以及题型、题目二级列全部统一设置为 `align: 'center'`；
  - 队伍名称单元格容器由左对齐调整为 `flex items-center justify-center`，确保队名在单元格中严正居中；
- **单元格纵向右边框 (`border-right`) 支持**：
  - 给 `VTable` 配置 `:single-line="false"`；
  - 在 Scoped SCSS 中为 `.n-data-table-th` 与 `.n-data-table-td` 显式添加 `border-right: 1px solid #e0e2e6`（暗色模式为 `#3c4043`），末列 `:last-child` 排除双边框；
  - 使固定列（排名、队名、总分）以及题目网格列均具备清晰整齐的纵向网格分隔线；
- **队伍名称列宽度机理解析**：
  - 队伍名称列之前仅配置了 `minWidth: 170` 而未配置定宽 `width`；
### 2.6 队伍名称列宽锁死、赛题列宽限制与 VTooltip 全量覆盖
- **队伍名称列定宽截断**：
  - 配置 `width: 200, maxWidth: 200`，内层采用 `max-w-[190px]` 居中容器与 `truncate max-w-[180px]` 文本单行省略截断；
  - 彻底解决因弹性宽度吸收导致的队名列被异常撑开拉长的问题；
  - 外部完整包裹通用提示组件 `VTooltip.vue`，悬停时完整浮现完整队名。
- **赛题列弹性宽度与截断**：
  - 各具体题目列配置 `minWidth: 96, maxWidth: 156`；
  - 表头赛题名称使用 `truncate max-w-[140px]` 单行截断，并由 `VTooltip` 覆盖提示题型、完整题名及当前题目分值；
  - 单元格内解题加分徽章配置 `max-w-[140px] truncate`，防止长文本溢出。
- **全表 Tooltip 统一覆盖为 VTooltip**：
  - 排名列：冠军/亚军/季军及普通排名字段全部覆盖 `<VTooltip>`；
  - 队伍名称列：覆盖 `<VTooltip>` 展示完整队伍名称；
  - 总分列：覆盖 `<VTooltip>` 展示分数详情；
  - 赛题二级表头：覆盖 `<VTooltip>` 展示类别、赛题全称与分值；
  - 赛题解出/未解出单元格：覆盖 `<VTooltip>` 精准提示解题战队、题目名称、加分分值、首杀/解题位次及提交时间；
  - 题目为空时的回退列（解题数、最后提交时间）：同样包裹 `<VTooltip>`。

---

## 3. 改动影响范围与清单
1. **删除文件**：
   - `src/components/competition/ctf/VCtfScoreboardSelector.vue`
2. **修改文件**：
   - `src/components/competition/ctf/index.ts`：移除 `VCtfScoreboardSelector` 及类型导出；
   - `src/components/competition/ctf/VCtfScoreboardChart.vue`：按钮重构为 `VBtn`，增加全选/全隐操作；
   - `src/views/competition/tabs/CtfCompetitionScoreboardView.vue`：单页化重构，接入 `VTable` 与一行二列表头，移除标题队伍数量标注与「本队」徽章，所有列配置居中对齐并补充单元格 `border-right` 竖线，队名最长固定 200px 截断，赛题列宽 96px~156px 截断，全表 Tooltip 采用 `VTooltip.vue` 覆盖；
   - `src/components/layouts/AdminLayout.vue`：修复未读取的 `_key` 参数以保证全局类型校验通过；
   - `src/types/competition/index.ts`：补齐 CTF 相关领域类型定义。
