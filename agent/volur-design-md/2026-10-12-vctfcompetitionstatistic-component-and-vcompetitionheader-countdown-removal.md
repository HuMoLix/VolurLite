# VCtfCompetitionStatistic 战队统计组件实现与 VCompetitionHeader 倒计时迁移方案

## 1. 变更背景与目标

在 CTF 竞技场页面中，为了强化选手对当前战队竞技状态（积分、解题数、天梯排名）及竞赛剩余时间的感知，同时避免顶部标题栏信息过于拥挤，进行以下重构：
1. 在 `CtfCompetitionView` 下辖的赛题挑战侧边栏 `<aside>` 中新增战队数据统计卡片组件 `VCtfCompetitionStatistic.vue`；
2. 组件采用 Flex 弹性卡片布局：
   - 第一行：展示队伍头像（`VAvatar`）与队伍名称；
   - 第二行：采用 Flex Column 纵向列表结构，展示当前队伍得分、解题数量以及天梯排名；
   - 第三行（最后一行）：展示实时竞赛倒计时（含低时警示与时间截止事件回调）；
3. 将倒计时功能从全局顶部栏 `VCompetitionHeader.vue` 中彻底移除，消除顶部中区及移动端悬浮倒计时条的冗余展示。

---

## 2. 核心改动点

### 2.1 新增组件 `VCtfCompetitionStatistic.vue`
- 文件路径：`src/components/competition/ctf/VCtfCompetitionStatistic.vue`
- 结构与样式：
  - 外层卡片：`.v-ctf-competition-statistic`，采用 `rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark p-4 shadow-sm flex flex-col gap-3.5`；
  - 第一行（头像与名称）：`flex items-center gap-3`，结合 `VAvatar` 渲染圆形战队头像或首字母徽标，右侧展示战队名称及所属机构；
  - 第二行（统计数据列表）：`flex flex-col gap-2 p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border`，纵向列表展示：
    - 队伍得分：格式化为 `pts` 等宽数值；
    - 解题数量：统计当前已攻克题目总数；
    - 当前排名：展示格式化名次（如 `#4` 或 `-`）；
  - 第三行（倒计时）：`pt-3 border-t flex items-center justify-between`，结合 `HourglassOutline` 图标根据剩余时间（待赛、进行中、已结束、低时警示态）动态驱动色彩切换，并在时间截止时向外抛出 `time-up` 事件；
  - 生命周期管理：在 `onMounted` 开启 1s 定时器并在 `onUnmounted` 中精准释放。

### 2.2 导出扩展 `src/components/competition/ctf/index.ts`
- 导出 `VCtfCompetitionStatistic` 组件及其 Props 强类型接口 `VCtfCompetitionStatisticProps`。

### 2.3 集成至 `CtfCompetitionChallengeListView.vue` 与 `CtfCompetitionView`
- `CtfCompetitionChallengeListView.vue`：
  - 在侧边栏 `<aside class="w-full lg:w-60 flex-shrink-0 flex flex-col gap-4">` 中，按如下顺序排布组件：
    1. `<VCtfCategory>`（题目分类筛选）；
    2. `<VChallengeSearch>`（题目检索搜索框）；
    3. `<VCtfCompetitionStatistic>`（战队数据与倒计时卡片，紧随搜索框下方）；
  - Props 扩展接收 `myTeam`、`scoreboard`，动态计算 `solvedChallengesCount`、`myTeamScore`、`myTeamRank` 并透传；
  - 监听统计卡片的 `time-up` 事件并向父组件抛出。
- `CtfCompetitionView`：
  - 将当前视图内管理的 `myTeam`、`scoreboard` 传递给 `<CtfCompetitionChallengeListView>`，并绑定 `@time-up="handleTimeUp"`。

### 2.4 从 `VCompetitionHeader.vue` 中移除倒计时
- 文件路径：`src/components/competition/VCompetitionHeader.vue`
- 模板改动：
  - 移除中区槽位默认渲染的倒计时徽章胶囊，保留纯粹的 `<slot name="header-center" />`；
  - 移除移动端视口下的独立倒计时条 `.v-competition-countdown-bar`；
- 脚本改动：
  - 移除 `HourglassOutline` 图标导入；
  - 移除 `countdownText`、`countdownLabel`、`isTimeLow`、`timerStatus`、`timerId` 等状态变量及 `updateCountdownTimer`、`formatDiff` 等计算函数；
  - 移除 `onMounted` 与 `onUnmounted` 中对倒计时定时器的注册与监听，彻底卸载计时负载。

### 2.5 国际化语言包扩充
- 在 `zh-CN`、`en-US`、`zh-TW` 的 `competition.json` 中统一扩充：
  - `my_team`
  - `current_score`
  - `solved_challenges`
  - `current_rank`

### 2.6 样式与文字排版精细化对齐
- **战队头像组件统一**：
  - 在 `VCtfCompetitionStatistic.vue` 与 `CtfCompetitionTeamView.vue` 中统一引入并使用 `VAvatar` 渲染圆形战队头像，支持首字母自动兜底与自定义尺寸。
- **排版权重与色彩校准**：
  - 队伍得分、解题数量、排名的标签与数值文字颜色统一调整为 `#3c4043`（夜间模式适配 `dark:text-google-text-darkPrimary` / `dark:text-google-text-darkDesc`），`font-weight` 显式设为 `400` (`font-normal`)；
  - 彻底移除“考试进行中 - 剩余时间”冗余文案标签，底部仅展示倒计时沙漏与剩余时间（`countdownText`），文字颜色同样设为 `#3c4043`，`font-weight` 设为 `400`。

---

## 3. 规范执行与验证

1. **纯粹代码与零注释铁律**：所有新增及修改的 Vue/TS 文件均无冗余行内解释性注释。
2. **构建与类型验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程打包 0 错误 0 告警通过。
