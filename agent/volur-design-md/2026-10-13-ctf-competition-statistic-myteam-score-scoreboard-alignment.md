# CTF 竞赛侧边栏队伍统计分值与权威排行榜对齐修复方案

## 1. 变更背景与问题溯源

在 CTF 竞赛挑战页中，左侧信息面板组件 `VCtfCompetitionStatistic`（队伍统计卡片）展示的队伍得分（显示为 2000 分）与排行榜页面（`Scoreboard`）及“我的队伍”页面（`TeamView`）展示的队伍得分（显示为 2200 分）存在数据不一致现象。

通过对全链路前后端数据流与计算逻辑的追踪，定位出导致数据不一致的根本原因如下：
1. **一血加成（First Blood Bonus 1.1x）被遗漏**：
   - 后端在队伍提交正确 Flag 时（`CtfCompetitionServiceImpl.java`），对斩获一血的赛题颁发 1.1 倍的基准分值奖励（`isFirstBlood ? (int)(newPoints * 1.1) : newPoints`），并持久化在 `CtfCompetitionSolved.pointsAwarded` 中；
   - 后端排行榜接口（`CompetitionScoreboardServiceImpl.java`）与队伍解题记录接口通过累加各题实际获得的 `pointsAwarded` 计算总分，因此对于两道各 1000 分的一血题目，权威得分为 `1100 + 1100 = 2200` 分。
2. **前端统计卡片分数源自粗暴的客户端基准分纯累加**：
   - 在 `CtfCompetitionChallengeListView.vue` 与父容器 `CtfCompetitionView.vue` 中，`myTeamScore` 仅仅通过对赛题列表的客户端过滤：
     ```typescript
     props.challenges.filter((ch) => ch.isSolvedByMyTeam).reduce((acc, curr) => acc + (curr.currentPoints || 0), 0)
     ```
   - 该计算仅累加了题目的基准当前分值（`1000 + 1000 = 2000`），完全没有包含一血加成，也没有优先复用已传入组件的权威排行榜数据；
   - 与此对比，`CtfCompetitionTeamView.vue` 则优先复用了排行榜中的队伍条目分值（`currentScoreboardItem.value?.score`），从而获得了 2200 分。

---

## 2. 改造方案与改动点

### 2.1 `CtfCompetitionChallengeListView.vue`
1. **优先复用权威排行榜数据（Single Source of Truth）**：
   - 提取计算属性 `myTeamScoreboardItem`，根据当前队伍的 `teamId` 与 `teamName` 在 `props.scoreboard` 中进行高容错查找；
   - `myTeamScore` 优先读取 `myTeamScoreboardItem.value?.score`；若排行榜尚未加载完毕或无对应条目，再安全降级回退至赛题基准分累加；
   - `solvedChallengesCount` 优先读取 `myTeamScoreboardItem.value?.solvesCount`，若无则回退为解题列表过滤长度；
   - `myTeamRank` 复用 `myTeamScoreboardItem.value?.rank ?? '-'`，保持逻辑聚合一致。

### 2.2 `CtfCompetitionView.vue`
1. **聚合视图中对 `myTeamScore` 进行对齐改造**：
   - 同步改造 `CtfCompetitionView.vue` 中的 `myTeamScore` 计算属性，优先在 `scoreboard.value` 中匹配当前队伍条目的真实 `score`；
   - 在提交 Flag 乐观更新时，兼容字符串与数字类型的 `teamId` 比较，保证即时响应与随后刷新后端排行榜（`refreshScoreboard`）平滑衔接。

### 2.3 `CtfCompetitionChallengeModal.vue`
1. **弹窗提交 Flag 获得积分提取**：
   - 提交成功后重新拉取赛题详情（`detailRes`）时，若存在当前队伍的解题记录（`solves`），提取该条目的 `points`（包含一血奖励），使向外触发的 `flag-solved` 事件携带真实的颁发分数，保证乐观更新的分值精准。

---

## 3. 规范自查与验证

1. **零注释铁律**：所有修改的 Vue 组件与 TypeScript 文件中严禁编写解释性注释与冗余备注，所有设计考量完全收敛至本文档。
2. **强类型与快速失败**：全流程采用严格的强类型推导与容错比对，无 `any` 滥用。
3. **工程编译验证**：经 `vue-tsc --noEmit && vite build` 生产构建验证，全项目 0 错误编译通过。
