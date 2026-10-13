# CTF 竞赛排行榜折线图与多维榜单选择器重构记录

## 1. 变更背景与需求概述
在 `volur-lite-view-NEXT` CTF 竞赛大厅的计分排行榜模块中，原有的榜单展示形态较为单一，仅具备扁平表格列表展示，缺乏顶层积分动态趋势可视化与多维度数据视角分类。
根据需求，本次重构对 `CtfCompetitionScoreboardView.vue` 及相关组件进行了以下四大升级：
1. **前十名解题积分趋势折线图**：在排行榜上方新增前十名战队解题积分趋势折线图（`VCtfScoreboardChart.vue`），支持动态曲线/阶梯图、交互式浮动数据十字轴与各队伍显隐交互切换；
2. **排行榜标题与描述精简**：将原“实时解题天梯榜”文案规范化为“排行榜”，并移除右侧“1 支战队上榜”统计字样；
3. **排行榜列表表头与结构革新**：将原表头字段“名次”调整为“排名”，“战队”调整为“队伍名称”，紧接着为“总分”，随后为“题目列表”，并重构成**一行两列（2-column）**的标准现代复合表头。左列集成战队排名、队伍名称与当前总分，右列按 Misc、Web、Crypto 等分类全面呈现题目完成状态与解题积分徽章；
4. **多维榜单选择器组件抽取**：参考 `VCtfCategory.vue` 的交互设计与主题样式，独立封装 `VCtfScoreboardSelector.vue` 侧边栏选择器，提供“比赛概况”、“积分总榜”、“解题总榜”、“题目榜单”、“单人总榜”、“题型榜单”等六大维度切换能力。

---

## 2. 核心改动点详述

### 2.1 抽取 `VCtfScoreboardSelector.vue` 榜单分类选择器
- **文件路径**：`src/components/competition/ctf/VCtfScoreboardSelector.vue`
- **设计标准**：
  - 参考 `VCtfCategory.vue` 的交互架构与 `.challenge-btn` 样式体系；
  - 纯 CSS 与 Google 扁平灰色阶梯无缝兼容，支持亮色模式与深曜炭黑（Obsidian）暗色模式；
  - 提供 6 个标准选项与对应矢量图标：
    - `比赛概况` (`AnalyticsOutline`)
    - `积分总榜` (`TrophyOutline`, 默认选中项)
    - `解题总榜` (`RibbonOutline`)
    - `题目榜单` (`GridOutline`)
    - `单人总榜` (`PersonOutline`)
    - `题型榜单` (`LayersOutline`)
  - 导出至 `src/components/competition/ctf/index.ts`。

### 2.2 抽取 `VCtfScoreboardChart.vue` 前十名积分趋势折线图
- **文件路径**：`src/components/competition/ctf/VCtfScoreboardChart.vue`
- **实现特性**：
  - 基于高精度纯 SVG 渲染，无第三方臃肿图表库依赖；
  - 自动提取排行榜前 10 支队伍，赋予 Google 标准色阶梯（Google Blue、Red、Yellow、Green、Cyan 等）；
  - 支持阶梯式积分步进（Step-after line curve），高度符合 CTF 解题即时累加积分的竞技真实规律；
  - 响应式计算时间跨度（比赛起止时间与队伍提交时序），支持 Y 轴自适应网格刻度与 X 轴时间格式化；
  - 交互式 Crosshair 十字虚线与 Hover Tooltip 浮层，鼠标滑动实时显示各队伍在当前时间节点的相对得分与排名；
  - 图例支持队伍点击切换显隐（Toggle visibility）与悬停聚焦高亮（Highlight focus）。

### 2.3 重构 `CtfCompetitionScoreboardView.vue`
- **文件路径**：`src/views/competition/tabs/CtfCompetitionScoreboardView.vue`
- **结构升级**：
  - 采用 `aside (VCtfScoreboardSelector)` + `main` 的响应式左右两列栅格布局；
  - 当处于“积分总榜”（默认项）时：
    1. 顶部加载 `VCtfScoreboardChart`；
    2. 下方呈现排行榜卡片，标题更新为“排行榜”，彻底移除“X 支战队上榜”多余文案；
    3. 表头重构为**一行两列**结构：
       - **第一列（左侧）**：合并“排名”、“队伍名称”、“总分”三大核心属性；
       - **第二列（右侧）**：呈现“题目列表”，按题型分类（如 Misc、Web、Crypto、Pwn、Reverse）折叠展示赛题芯片标签。已解出赛题以 Google 翡翠绿底色搭配勾选图标与获得分数显式高亮，未解出赛题显示柔和灰调微显标签；带有高交互 Tooltip 气泡提示；
  - 支持在侧边选择器切换至“比赛概况”、“解题总榜”、“题目榜单”、“单人总榜”与“题型榜单”不同视图模块。

### 2.4 上游传参扩展与国际化同步
- **上游路由联动**：在 `CtfCompetitionView` 中为 `CtfCompetitionScoreboardView` 补充透传 `:challenges="challenges"` 与 `:competition="competition"`，确保排行榜能够即时获取赛题列表和比赛时间区间；
- **国际化词条补充**：在 `src/locales/zh-CN/competition.json`、`en-US/competition.json`、`zh-TW/competition.json` 中全量增补 `scoreboard_title`、`score_chart_title`、`challenge_list` 等多语言词条。

---

## 3. 规范自查与架构设计合规性
1. **ES2026 与纯净代码（零注释铁律）**：
   - 所有新建与修改的代码文件（`.vue`、`.ts`）均无任何行内注释或冗余解释，严格践行文档外置与自解释命名原则；
2. **Google 主题体系规范**：
   - 文本绝不使用纯黑 `#000000`，严格采用 Google Gray 体系；
   - 品牌色与状态色对齐 Google Blue (`#1a73e8` / `#8ab4f8`)、Green (`#34a853` / `#81c995`) 等；
3. **类型安全与零 Mock 原则**：
   - 严格基于后端数据模型 `CtfScoreboardItemVO`、`CtfCompetitionChallengeVO`、`CompetitionDetailVO` 运作，杜绝任何假数据兜底；
4. **编译与构建验证**：
   - 经 `vue-tsc --noEmit` 与 `vite build` 严格验证，编译与打包全量通过（Exit code 0）。
