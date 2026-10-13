# CtfCompetitionScoreboardView 榜单排名勋章与分数排版重构设计文档

## 1. 变更背景与需求分析
在 Volur Lite CTF 赛事大厅的综合排行榜（`CtfCompetitionScoreboardView.vue`）中，原榜单前三名采用简单的彩色数字圆形徽标进行标识，分数值及题目分值展示带有 "pts" / "p" 冗余单位后缀且采用等宽蓝色粗体字，未能与赛事卡片视觉风格达成统一。为提升排行榜的专业质感与排版精致度，本次重构聚焦于以下两项具体诉求：
1. **排名 1、2、3 勋章化**：
   - 参考 `CtfCompetitionChallengeCard.vue` 中的 Medal 样式，将前三名（冠、亚、季军）由原来的纯数字圆形徽章替换为 `@vicons/fa` 的 `Medal` 实体勋章图标；
   - 金牌（第一名）、银牌（第二名）、铜牌（第三名）分别采用金、银、铜专属色彩等级（金：`text-amber-500 dark:text-amber-400`；银：`text-slate-400 dark:text-slate-300`；铜：`text-orange-500 dark:text-orange-400`）；
   - 保持外层 `VTooltip` 冠军/亚军/季军提示能力，第 4 名及后续队伍依然保持纯数字排名呈现。
2. **分数排版与单位后缀优化**：
   - 字体与字重调整：分数字体由原 `font-mono font-bold`（粗体等宽）调整为无衬线字体 `Inter, sans-serif`，字重统一设定为 `font-weight: 400`；
   - 色彩统一：亮色模式下严格使用 `#202124`，并适配暗色模式阶梯色彩（`#e3e3e3`）；
   - 彻底移除单位后缀：删除总分列中的 "pts" 字样（原 `${row.score} pts` 改为纯数字 `${row.score}`），删除赛题表头二级标题中的 "p" 字样（原 `${ch.currentPoints}p` 改为纯数字 `${ch.currentPoints}`），并同步清理相关 Tooltip 内的冗余后缀。

---

## 2. 详细改造方案

### 2.1 引入 Medal 勋章并重构 Rank 列渲染
- 从 `@vicons/fa` 中显式导入 `Medal` 图标组件；
- 新增 `getMedalIconColor(rank: number)` 颜色映射函数：
  - `rank === 1`：金牌，映射为 `'text-amber-500 dark:text-amber-400'`；
  - `rank === 2`：银牌，映射为 `'text-slate-400 dark:text-slate-300'`；
  - `rank === 3`：铜牌，映射为 `'text-orange-500 dark:text-orange-400'`。
- 重构 `columns` 的 `rank` 列渲染器：
  - 前三名统一渲染包含 `Medal`（`size: 18`）的内联容器，外层保持悬停提示对应荣誉头衔；
  - 4 名及后续排名保持原排版，并补齐深色模式文字颜色类 `dark:text-google-text-darkMuted`。

### 2.2 总分列与题目分值样式收敛 (.scoreboard-score)
- 在 `<style scoped lang="scss">` 中新增 `.scoreboard-score` 样式类：
  ```scss
  .scoreboard-score {
    font-family: Inter, sans-serif;
    color: #202124;
    font-weight: 400 !important;
  }

  html.dark .scoreboard-score {
    color: #e3e3e3;
  }
  ```
- 总分列（`key: 'score'`）：
  - 单元格文字改用 `.scoreboard-score` 类；
  - 内容由 `${row.score} pts` 调整为 `row.score`；
  - `VTooltip` 提示内容同步去除 `pts`。
- 题目二级表头分值（`ch.currentPoints`）：
  - 表头副标题文字改用 `.scoreboard-score` 类；
  - 内容由 `${ch.currentPoints}p` 调整为 `ch.currentPoints`；
  - 表头 `VTooltip` 提示内容中的 `(${ch.currentPoints} pts)` 同步调整为 `(${ch.currentPoints})`。
- 解题记录提示（`getChallengeSolveTooltip`）：
  - 同步清理内部加分 `(+${pts} pts)` 与未解出分值 `(${ch.currentPoints} pts)` 中的冗余 `pts` 字样。

---

## 3. 规范与安全合规性检查
- **零注释原则 (Zero Comment)**：严格遵守规范，未在 Vue/TS 代码中插入任何解释性注释，文档全部收敛于本文档；
- **类型安全**：执行 `vue-tsc --noEmit` 全量静态类型检查，0 错误通过；
- **视觉主题对齐**：完全契合 Google Workspace 色彩规范（亮色 `#202124`，暗色 `#e3e3e3`），支持明暗主题无缝自适应切换。
