# CtfCompetitionScoreboardView 排名、战队、总分数据排版微调与 Inter 字体全局统一设计文档

## 1. 变更背景与需求分析
根据最新的视觉规范要求，对 CTF 赛事大厅的排行榜数据列表（`CtfCompetitionScoreboardView.vue`）进行以下进一步精细化对齐：
1. **全局统一字体为 `Inter, sans-serif`**：
   - 表格内全量文字层级（包括表头标题、题型、题目名称、题目分值、表格排名数值、战队名称、总分、备用列解题数与最后提交时间，以及未解出占位符等）彻底废除 `font-mono`，统一显式锁定为 `font-family: Inter, sans-serif;`；
   - 深入 Naive UI `n-data-table` 的底层 DOM（`.n-data-table`、`.n-data-table-th`、`.n-data-table-th__title-wrapper`、`.n-data-table-th__title` 与 `.n-data-table-td`）注入全局 `font-family: Inter, sans-serif;`；
2. **核心数据文字排版指标**：
   - 排名（Rank）、队伍名称（Team Name）与总分（Score）文字颜色严格使用 `#3c4043`（暗色模式自适应适配 Google Text DarkDesc `#bdc1c6`）；
   - 字重统一设定为 `font-weight: 300 !important;`（极细体）；
   - 字号统一锁定为 `font-size: 14px;`；
3. **取消队伍名称 Hover 变色**：
   - 移除鼠标悬停时的蓝色变色（`hover:text-google-blue dark:hover:text-google-blue-dark`），保持页面视觉的沉稳与一致性，同时保留点击与 Tooltip 交互。

---

## 2. 详细实现细节

### 2.1 样式类抽取与 Inter 字体强化
在 `<style scoped lang="scss">` 中统一配置：
```scss
:deep(.n-data-table) {
  font-family: Inter, sans-serif;

  .n-data-table-th {
    font-family: Inter, sans-serif;
    color: #202124;
    text-align: center;

    .n-data-table-th__title-wrapper {
      font-family: Inter, sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      text-align: center;
      color: #202124;
    }

    .n-data-table-th__title {
      font-family: Inter, sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      text-align: center;
      color: #202124;
    }
  }

  .n-data-table-th,
  .n-data-table-td {
    font-family: Inter, sans-serif;
    border-right: 1px solid #e0e2e6;
    text-align: center;

    &:last-child {
      border-right: none;
    }
  }

  .n-data-table-td {
    font-family: Inter, sans-serif;
    color: #3c4043;
  }
}

.scoreboard-cell-text {
  font-family: Inter, sans-serif;
  color: #3c4043;
  font-weight: 300 !important;
  font-size: 14px;

  html.dark & {
    color: #bdc1c6;
  }
}

.scoreboard-score-cell {
  font-family: Inter, sans-serif;
  color: #3c4043;
  font-weight: 300 !important;
  font-size: 14px;

  html.dark & {
    color: #bdc1c6;
  }
}
```

### 2.2 彻底清理代码中的 font-mono
- 从 `rank` 单元格中移除 `font-mono`；
- 从未解题占位符 `—` 中移除 `font-mono`；
- 从备用列 `solvesCount` 与 `lastSolveTime` 中移除 `font-mono`；
- 确保榜单整体视觉风格呈现统一的现代现代无衬线 Inter 排版质感。

---

## 3. 规范与安全合规性检查
- **零注释原则 (Zero Comment)**：代码中杜绝任何行内注释，技术演进全程收敛于文档；
- **类型安全**：通过 `vue-tsc --noEmit` 全量静态类型检查，无任何编译错误；
- **主题规范**：完全契合 Google Workspace 色彩体系与 `view-NEXT-rules.md` 规范。
