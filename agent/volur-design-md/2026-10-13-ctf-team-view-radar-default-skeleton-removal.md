# CTF 队伍视图能力雷达默认轴骨架移除与真实分类动态计算方案

## 1. 变更背景与需求
在 `CtfCompetitionTeamView.vue` 中，原实现包含静态预设的默认题型维度轴：
```ts
const defaultCats = ['Web', 'Pwn', 'Reverse', 'Crypto', 'Misc']
```
该默认轴骨架在队伍尚未解题或比赛分类非传统 5 类时，会导致雷达图展示与当前赛事题目无关的预设方向。按照平台规范中的真实数据源驱动与杜绝隐式 Mock/伪造兜底原则，需彻底移除默认轴骨架，使雷达图仅基于本场比赛存在的真实题目分类（`props.challenges`）以及该队伍已解题目的实际分类（`teamSolves`）动态生成坐标轴。

## 2. 改动点清单

### 2.1 移除默认轴骨架并仅收集真实数据分类
- 文件：`src/views/competition/tabs/CtfCompetitionTeamView.vue`
- 改动：彻底移除 `defaultCats` 数组。优先从 `props.challenges` 中遍历比赛实际包含的分类名称，随后补充 `teamSolves` 中的分类（防止赛题在被解题后由于权限或下线缺失）。

### 2.2 雷达图边界保护与空状态呈现
- 文件：`src/views/competition/tabs/CtfCompetitionTeamView.vue`
- 改动：
  - 在几何算法中，雷达图多边形闭合至少需要 3 个独立维度（点或线段无法构成雷达多边形面积）。
  - 在 `radarSpokes`、`radarRings`、`scoreRadarPolygon`、`scoreRadarPoints`、`scoreRadarLabels`、`solvesRadarPolygon`、`solvesRadarPoints`、`solvesRadarLabels` 中增加 `total < 3` 的快速熔断逻辑，返回空数组或空字符串。
  - 在模板层为雷达图容器增加 `v-if="radarCategories.length >= 3"` 判断；当比赛分类不足 3 类时展示标准化空状态提示 `t('competition.no_radar_data', '暂无足够题目分类数据生成能力雷达')`。

## 3. 影响范围与验证
- 影响组件：`CtfCompetitionTeamView.vue`
- 构建验证：执行 `npm run build`，编译构建成功通过，无类型与语法缺陷。
