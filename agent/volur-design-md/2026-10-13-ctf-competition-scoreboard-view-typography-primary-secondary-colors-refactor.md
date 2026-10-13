# CtfCompetitionScoreboardView 文本主次色彩体系规范重构设计文档

## 1. 变更背景与需求分析
根据最新的视觉与色彩规范，对 CTF 赛事大厅的排行榜数据列表（`CtfCompetitionScoreboardView.vue`）进行全文字层级的色彩对齐与重构：
1. **主次双色体系确立**：
   - **主颜色 (Primary Color)**：严格使用 `#202124`（暗色模式自适应适配 Google Text DarkPrimary `#e3e3e3`）；
   - **次颜色 (Secondary Color)**：严格使用 `#5f6368`（暗色模式自适应适配 Google Text DarkMuted `#9aa0a6`）；
2. **表头区域文字色彩收敛**：
   - 表头内所有列级标题文字（排名、队伍名称、总分、题型大类名称、具体题目名称等）全量统一采用**主颜色**（`#202124` / `#e3e3e3`）；
   - 题目名称下方的题目分值（`ch.currentPoints`）采用**次颜色**（`#5f6368` / `#9aa0a6`）；
3. **表格数据体（单元格数据）文字色彩收敛**：
   - 表格内所有数据呈现（排名数值、战队名称、总分数值、解题数、最后提交时间等）全量统一采用**次颜色**（`#5f6368` / `#9aa0a6`）；
4. **图标不变原则**：
   - 维持 Medal（金/银/铜奖牌颜色）与 FlagOutline（红色实心填充）图标色彩不变。

---

## 2. 详细改造方案

### 2.1 表头标题与题目分值色彩映射
- 基础列标题重构为渲染函数：
  - 排名列：`title: () => h('span', { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' }, t('competition.rank', '排名'))`；
  - 战队列：`title: () => h('span', { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' }, t('competition.team_name', '队伍名称'))`；
  - 总分列：`title: () => h('span', { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' }, t('competition.score', '总分'))`；
  - 备用列（解题数、最后提交）：同样应用 `text-[#202124] dark:text-[#e3e3e3]`；
- 题型大类（第一行表头）：`text-[#202124] dark:text-[#e3e3e3]`；
- 题目名称（第二行表头标题）：`text-[#202124] dark:text-[#e3e3e3]`；
- 题目分值（第二行表头副标题）：通过 `.scoreboard-score` 类统一继承次颜色。

### 2.2 表格单元格数据次颜色映射
- 排名列数值（`row.rank`）：应用 `text-[#5f6368] dark:text-google-text-darkMuted`；
- 队伍名称列（`row.teamName`）：由原主颜色调整为 `text-[#5f6368] dark:text-google-text-darkMuted`（悬停保持 `hover:text-google-blue dark:hover:text-google-blue-dark`）；
- 总分列数值（`row.score`）：通过 `.scoreboard-score` 类统一调整；
- 备用列（解题数、最后提交时间）：数据单元格全量应用 `text-[#5f6368] dark:text-google-text-darkMuted`；
- CSS 基础兜底：在 `:deep(.n-data-table)` 中配置 `.n-data-table-td { color: #5f6368; }` 及暗色模式 `color: #9aa0a6;`。

### 2.3 分数类 (.scoreboard-score) 样式调整
- 在 `<style scoped lang="scss">` 中统一更新：
  ```scss
  .scoreboard-score {
    font-family: Inter, sans-serif;
    color: #5f6368;
    font-weight: 400 !important;
  }

  html.dark .scoreboard-score {
    color: #9aa0a6;
  }
  ```
- 确保表头题目分值与表格内总分数值同时遵从次颜色 `#5f6368`（暗色 `#9aa0a6`）。

---

## 3. 规范与安全合规性检查
- **零注释原则 (Zero Comment)**：代码中杜绝任何行内注释，技术演进全程收敛于文档；
- **类型安全**：通过 `vue-tsc --noEmit` 全量静态类型检查，无任何编译错误；
- **主题规范**：完全契合 Google Workspace 色彩体系与 `view-NEXT-rules.md` 规范。
