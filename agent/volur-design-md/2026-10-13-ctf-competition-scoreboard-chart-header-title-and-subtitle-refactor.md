# CTF 比赛积分走势图表标题文案更新与副标题精简方案

## 1. 变更背景与需求目标
在 CTF 比赛榜单视图（`CtfCompetitionScoreboardView.vue`）所引用的解题积分趋势图表组件（`VCtfScoreboardChart.vue`）中，根据最新 UI 规范进行文案精简与视觉去噪：
1. **标题通用化**：将图表主标题由限定范围的“前十名解题积分趋势”修改为更简洁统一的“解题积分趋势”；
2. **副标题精简**：移除图表右上角/副区域中冗余的“Top 10 队伍积分折线图”字样，消除视觉噪点，保持卡片头部高度整洁。

## 2. 核心改动点说明

### 2.1 走势图组件头部结构与标题修改
- 文件：`src/components/competition/ctf/VCtfScoreboardChart.vue`
- 将原有的 `{{ t('competition.score_chart_title', '前十名解题积分趋势') }}` 修改为 `{{ t('competition.score_chart_title', '解题积分趋势') }}`；
- 彻底移除副标题结构 `<div class="text-xs text-google-text-muted dark:text-google-text-darkMuted font-mono">{{ t('competition.top10_teams_chart', 'Top 10 队伍积分折线图') }}</div>`。

### 2.2 多语言 Locale 同步更新
- **简体中文**（`src/locales/zh-CN/competition.json`）：
  - `"score_chart_title": "解题积分趋势"`
- **繁体中文**（`src/locales/zh-TW/competition.json`）：
  - `"score_chart_title": "解題積分趨勢"`
- **英语**（`src/locales/en-US/competition.json`）：
  - `"score_chart_title": "Score & Solve Trend"`

## 3. 规范审计与影响范围
- **代码纯粹性（零注释铁律）**：组件文件与 JSON 国际化字典保持完全零注释；
- **类型安全**：通过 `vue-tsc --noEmit` 严格类型检测；
- **影响范围**：
  - `src/components/competition/ctf/VCtfScoreboardChart.vue`
  - `src/locales/zh-CN/competition.json`
  - `src/locales/zh-TW/competition.json`
  - `src/locales/en-US/competition.json`
