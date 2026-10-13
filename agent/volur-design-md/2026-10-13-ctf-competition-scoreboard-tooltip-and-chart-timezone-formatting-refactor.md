# CTF 比赛排行榜单元格 Tooltip 清理与时区时间格式化重构方案

## 1. 变更背景与需求目标
在 CTF 比赛计分板视图（`CtfCompetitionScoreboardView.vue`）和解题积分趋势图表（`VCtfScoreboardChart.vue`）中，原有的交互提示与时间展示存在以下待改进点：
1. **排名与总分提示冗余**：表格中的排名列已采用统一的纯数字或奖牌区分，总分列已具备明确的数值显示，附带的 Tooltip 浮层信息冗余并对用户的日常浏览产生视线干扰。
2. **赛题解出信息格式过长**：原赛题项已解出的 Tooltip 显示为“`{teamName} 已解出 [{category}] {chName} (+{pts}) [一血/二血/三血] (ISO-UTC-时间)`”，信息冗长且时间为原始 UTC 字符串（如 `2026-10-12T11:26:27Z`），不便于选手直观查看本地解题时间与得分。
3. **图表时间刻度与游标缺少秒级与本地时区转换**：原积分走势图的横轴时间标签与悬浮十字线游标仅显示 `hh:mm`，需调整为统一转换用户本地时区后的 `hh:mm:ss` 格式。

## 2. 核心改动点说明

### 2.1 排行榜表格删除排名与总分的 Tooltip
- 在 `src/views/competition/tabs/CtfCompetitionScoreboardView.vue` 的 `columns` 配置中：
  - **排名列 (`rank`)**：移除包裹的 `VTooltip` 组件及多余的冠军/亚军文案判定，直接通过 `h('span', { class: 'scoreboard-cell-text cursor-default' }, row.rank)` 渲染数字排名；
  - **总分列 (`score`)**：移除包裹的 `VTooltip` 组件，直接通过 `h('span', { class: 'scoreboard-score-cell cursor-default' }, row.score)` 渲染总分。

### 2.2 赛题解出提示重构为加分与本地时区时间
- 在 `src/views/competition/tabs/CtfCompetitionScoreboardView.vue` 中新增 `formatSolveDateTime` 辅助函数：
  - 解析 `solveDetail?.solvedAt`，转换为用户本地时区时间；
  - 格式化输出为 `YYYY-MM-DD hh:mm:ss`（两位补齐，如 `2026-10-12 19:26:27`）；
- 修改 `getChallengeSolveTooltip`：
  - 若已解出，格式精简为 `+${pts} ${timeStr}`（如 `+1100 2026-10-12 19:26:27`）；若未记录解出时间则显示 `+${pts}`；
  - 未解出题目保持未解出提示。

### 2.3 积分走势图表时间转换为本地时区 `hh:mm:ss`
- 在 `src/components/competition/ctf/VCtfScoreboardChart.vue` 中修改 `formatTime` 函数：
  - 支持 `timestamp: number | string`；
  - 基于本地时区获取时、分、秒，统一采用 `String(n).padStart(2, '0')` 两位补齐；
  - 输出格式统一为 `hh:mm:ss`；
  - 同步生效于 X 轴时间刻度（`xTicks`）与鼠标悬浮游标（`hoverInfo.timeLabel`）。

## 3. 规范审计与影响范围
- **代码纯粹性（零注释铁律）**：相关 `.vue` 组件均保持纯净代码，无任何多余行内注释；
- **类型安全**：经 `vue-tsc --noEmit` 严格类型检测，0 错误；
- **影响范围**：
  - `src/views/competition/tabs/CtfCompetitionScoreboardView.vue`
  - `src/components/competition/ctf/VCtfScoreboardChart.vue`
