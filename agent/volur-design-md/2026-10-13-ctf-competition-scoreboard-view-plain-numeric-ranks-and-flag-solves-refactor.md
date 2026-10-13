# CtfCompetitionScoreboardView 纯数字排名、一二三血 Medal 与普通解题 FlagOutline 红色填充重构设计文档

## 1. 变更背景与需求澄清
在对 CTF 赛事大厅综合排行榜（`CtfCompetitionScoreboardView.vue`）中赛题解题状态图标设计的进一步明确中，排版需求精准澄清如下：
1. **排名列（Rank）保持统一纯数字**：
   - 移除前三名特殊的勋章/徽标形式，全量采用 `1`、`2`、`3`、`4` 等纯数字展示；
   - 保持外层 Tooltip 对于第 1、2、3 名的荣誉称号（冠军、亚军、季军）悬停展示。
2. **赛题解出状态呈现逻辑（一二三血 vs 普通解出）**：
   - **一血、二血、三血（Top 3 Solves）**：
     - 使用 `@vicons/fa` 的 `Medal` 实体奖牌勋章图标；
     - 一血（First Blood，首杀）：金牌色 `text-amber-500 dark:text-amber-400`；
     - 二血（Second Blood）：银牌色 `text-slate-400 dark:text-slate-300`；
     - 三血（Third Blood）：铜牌色 `text-orange-500 dark:text-orange-400`；
   - **普通解出（非一二三血）**：
     - 使用 `@vicons/ionicons5` 的 `FlagOutline` 旗帜图标；
     - 采用红色填充（`color: #ea4335`，暗色 `#f28b82`，并对 SVG 内部 path 设置 `fill: currentColor !important;` 实现纯红填充旗帜效果）；
   - **分数值与血次荣誉说明**：
     - 取消胶囊形式，数值统一收敛至悬停 Tooltip 中展示（如 `战队名 已解出 [Misc] 赛题名 (+100) [一血 / First Blood]`）；
   - **未解出赛题**：
     - 保持 `—` 柔和占位符与未解出提示。
3. **赛题分类与赛题项居中对齐**：
   - 第一级分类表头与第二级题目分值表头保持居中对齐；
   - 深度样式中对 `.n-data-table-th__title-wrapper` 与 `.n-data-table-th__title` 注入 `justify-content: center` 与 `text-align: center`，彻底根除多级表头的非居中偏移。

---

## 2. 详细实现细节

### 2.1 解题血次探测与图标分流渲染
- `getTeamSolveRecord` 综合考虑 `solvedTeamsMap` 字典以及 `ch.topTeams` 数组下标映射，确保血次（`solveRank` 1/2/3）在首屏和异步刷新期均可精准判定；
- 解题单元格渲染：
  ```ts
  const isTopThree = solveRank === 1 || solveRank === 2 || solveRank === 3

  isTopThree
    ? h(NIcon, {
        size: 18,
        component: Medal,
        class: `${getMedalIconColor(solveRank!)} shrink-0`
      })
    : h(NIcon, {
        size: 18,
        component: FlagOutline,
        class: 'solved-flag-icon text-google-red dark:text-google-red-dark shrink-0'
      })
  ```

### 2.2 旗帜红色填充样式 (.solved-flag-icon)
- 在 `<style scoped lang="scss">` 中配置：
  ```scss
  .solved-flag-icon {
    color: #ea4335;

    html.dark & {
      color: #f28b82;
    }

    :deep(svg path) {
      fill: currentColor !important;
    }
  }
  ```
- 配合 Naive UI `NIcon`，将 `FlagOutline` 的轮廓描边与内部闭合路径完全填充为 Google Red，直观呈现红色实心旗帜。

---

## 3. 规范与安全合规性检查
- **零注释原则 (Zero Comment)**：代码中杜绝任何行内注释，技术演进全程收敛于文档；
- **类型安全**：通过 `vue-tsc --noEmit` 全量静态类型检查，无任何编译错误；
- **主题规范**：完全契合 `view-NEXT-rules.md` 色彩与排版规范，明暗双主题自适应良好。
