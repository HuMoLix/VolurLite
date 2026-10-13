# CTF 竞技场赛题卡片与详情模态框组件抽取与架构重构方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）前端工程中，CTF 竞技场（`CtfCompetitionView` 与 `CtfCompetitionChallengeListView.vue`）负责赛题挑战的展示、动态靶机调度及 Flag 提交交互。原先实现在 `CtfCompetitionChallengeListView.vue` 中将赛题卡片列表与底层弹窗直接内联平铺，存在以下工程与交互缺陷：
1. **职责过载与耦合度高**：赛题卡片渲染、靶机生命周期（启动/续期/销毁/倒计时）、Flag 提交校验以及详情模态框逻辑混合在单一列表组件中，难以独立维护与单元测试；
2. **卡片视觉呈现需升级**：
   - 题目分类此前使用胶囊式边框标签（Tag/Badge），视觉层级略显杂乱，需改为精简现代的「Icon + 文字描述」扁平化风格；
   - 动态容器（Docker 靶机）需通过官方辨识度高的 `LogoDocker` 图标显式标注；
   - 题目中层需清晰呈现题目标题及其分值（pts）；
   - 卡片下层需展示前三个解出该题目的战队（Top 3 Solvers / First Blood），并在无解出时友好显示等待一血占位；
   - 卡片最底部需具备贯穿式的状态条指示器（已攻克展示绿色，未攻克展示品牌蓝 `#1765CC`）。

本方案对右侧赛题网格中的单个题目卡片抽象为 `CtfCompetitionChallengeCard.vue`，并将点击卡片展开的弹窗提炼为受控组件 `CtfCompetitionChallengeModal.vue`，同时统一收敛导出至 `@/views/competition/components`。

---

## 2. 核心改动点与技术实现

### 2.1 赛题卡片组件抽取 (`CtfCompetitionChallengeCard.vue`)

新建组件 `src/views/competition/components/CtfCompetitionChallengeCard.vue`，严格遵循以下设计要求：

1. **Flex 上中下三层流式布局**：
   - 最外层容器采用 `flex flex-col justify-between overflow-hidden rounded-2xl`，设置柔和的亮色/暗色边框与阴影悬浮位移过渡（`hover:-translate-y-0.5 hover:shadow-md`）；
2. **上层（分类与靶机标记）**：
   - **左侧分类**：彻底移除外围胶囊边框与药丸背景，采用纯粹的 `n-icon + 文字描述` 结构，根据题目分类（Web: `GlobeOutline`、Pwn: `TerminalOutline`、Reverse: `GitBranchOutline`、Crypto: `KeyOutline`、Misc: `ExtensionPuzzleOutline`）映射对应的图标与 Google 主题高辨识色彩；
   - **右侧容器标记**：针对 `isContainer === true` 的动态题目，引入 `@vicons/ionicons5` 的 `LogoDocker` 图标进行蓝调着色（`#2496ed`）标注，配合 `VTooltip` 提示「动态靶机环境 (Docker)」；同时已攻克题目展示绿色对勾状态徽章；
3. **中层（标题与分值）**：
   - 题目名称采用截断控制（`line-clamp-2`）与 Google 文本对比阶梯，悬停变为主色蓝；
   - 标题正下方紧随题目当前分值（`currentPoints pts`，等宽数字字体强化）与解出总队伍数（`solvesCount`）；
4. **下层（Top 3 解题队伍）**：
   - 读取题目的 `topTeams` 数组，展示前三名解出战队：
     - 第 1 名（一血）：金色奖章高亮徽标（`#fbbc04` / Amber 色阶）；
     - 第 2 名（二血）：银色徽标（Slate / 银灰色阶）；
     - 第 3 名（三血）：铜色徽标（Bronze / 暖橙色阶）；
   - 当题目尚未被攻破（`topTeams` 为空）时，优雅展示时钟图标与「暂无队伍解出 · 等待一血」占位提示；
5. **最底层状态指示条**：
   - 卡片正下方设置全宽 `h-1 w-full` 的状态横条：
     - 若战队已攻克（`isSolvedByMyTeam === true`）：呈现 Google Green 状态色（`#34a853`）；
     - 若未攻克（`isSolvedByMyTeam === false`）：严格采用指定的品牌蓝 `#1765CC`。

### 2.2 详情与交互弹窗抽取 (`CtfCompetitionChallengeModal.vue`)

新建组件 `src/views/competition/components/CtfCompetitionChallengeModal.vue`：
1. **完整受控封装**：接收 `show`（`v-model:show`）、`competitionId`、`challenge` 属性，并向外派发 `update:show` 与 `flag-solved` 事件；
2. **动态靶机完整生命周期内聚**：
   - 容器创建调度（`startCtfInstanceApi`）；
   - 独立靶标端口复制与倒计时租期跟踪（精确递减计时器与组件销毁自动清理）；
   - 租期续期 15 分钟（`extendCtfInstanceApi`）；
   - 靶机容器销毁回收（`stopCtfInstanceApi`）；
3. **Flag 提交评测与状态同步**：
   - 输入框防空校验、回车提交支持；
   - 调用 `submitCtfFlagApi`，命中正确时实时更新当前题目解出状态、增加解题数并向上广播 `flag-solved`。

### 2.3 列表视图重构与组件收敛 (`CtfCompetitionChallengeListView.vue`)

1. 精简 `CtfCompetitionChallengeListView.vue`：
   - 赛题网格仅通过 `<CtfCompetitionChallengeCard :challenge="ch" @click="openChallengeDetail(ch)" />` 进行遍历声明；
   - 弹窗通过 `<CtfCompetitionChallengeModal v-model:show="detailModalVisible" ... />` 声明；
   - 移除了容器生命周期及 Flag 提交等冗余内联状态，实现逻辑与展示的高内聚低耦合；
2. 规范组件导出：
   - 在 `src/views/competition/components/index.ts` 中统一导出 `CtfCompetitionChallengeCard` 与 `CtfCompetitionChallengeModal`。

### 2.4 Mock 数据与类型对齐

1. 在 `src/types/competition/index.ts` 的 `CtfCompetitionChallengeVO` 中补充类型 `topTeams?: string[]`；
2. 在 `CtfCompetitionView` 的 `MOCK_CHALLENGES` 中补充 Top 3 解出队伍样本数据，保证本地与联调模式下三层卡片的完整视觉呈现。

### 2.5 赛题卡片细节优化与交互精简

根据最新视觉交互要求，对 `CtfCompetitionChallengeCard.vue` 进行了二次精细化调整：
1. **删除上层「已解出」冗余徽标**：顶部右上角仅保留 `LogoDocker` 靶机状态指示，题目攻克状态由底部的贯穿状态横条（绿色）统一指示，保持头部极简；
2. **清除 Hover 动效与颜色渐变**：移除外层卡片的 `hover:shadow-md hover:-translate-y-0.5` 浮动位移与投影，移除题目标题在 Hover 时的 `group-hover:text-google-blue` 变色，保持卡片稳重内敛与视觉静止质感；
3. **重构底部 Top 3 解出战队展示**：
   - 彻底删除顶部 `解出战队 (Top 3) 3/3` 标题栏；
   - 采用 `@vicons/fa` 的 `Medal` 勋章图标，按照金（`text-amber-500`）、银（`text-slate-400`）、铜（`text-orange-500`）色彩体系分别渲染一、二、三血战队胶囊；
   - 强制战队列表在单行内横向排布（`flex-nowrap`、`w-full min-w-0`、`flex-1`），文字超出时自动省略（`truncate`）；
   - 接入 `@/components/common/VTooltip.vue`，在选手悬浮战队名称时即时提示完整战队名称。

### 2.6 标题行合并、解出数移除与纯 Medal 图标轻量化

根据进一步的排版紧凑与极简优化要求，对 `CtfCompetitionChallengeCard.vue` 进行了三次精简：
1. **纯粹 Medal 图标展示**：
   - 彻底移除了前三战队的外层胶囊背景与边框，仅保留有色勋章图标（金 `#f59e0b` / 银 `#94a3b8` / 铜 `#f97316`）；
   - 保留 `VTooltip` 悬浮完整战队名称提示，极大减轻了卡片底部的视觉负荷；
2. **容器内边距间距紧凑化**：
   - 删除了卡片主体 `p-5 flex flex-col flex-1 justify-between gap-3` 中的 `gap-3`，转由 `justify-between` 自适应流式拉伸三层高度；
3. **标题行整合与三段式网格对齐**：
   - 彻底移除了卡片上显式显示的解出数量（`solvesCount`）；
   - 将原先底部的题目分数提拉至标题行，整合为三段式栅格结构：`[左: 题目标题 | 中: 分数 pts | 右: Docker 标识]`；
   - 采用 `grid grid-cols-[1fr_auto_1fr] items-center` 布局，使分数值（`pts`）严格在卡片水平中央居中显示，左侧题目标题单行截断（`truncate`），右侧放置 `LogoDocker` 图标。

### 2.7 题目分类首行三段对齐与前三战队截断展示

根据最新的排版微调指令，完成以下优化：
1. **首行三段式网格整合**：
   - 将分数值与动态容器（Docker）标识移动至第一行（分类行）；
   - 采用 `grid grid-cols-[1fr_auto_1fr] items-center` 布局：
     - **左侧**：分类信息（图标 + 分类名）；
     - **中央**：分数（`currentPoints pts` 水平绝对居中）；
     - **右侧**：动态容器标识（`LogoDocker` 图标，靠右对齐）；
   - 题目标题独立恢复至中间层，拥有完整的卡片横向展示空间（`line-clamp-2`）；
2. **前三战队带名称截断单行展示**：
   - 紧随 Medal 勋章图标展示战队名称；
   - 对超出长度的战队名称通过 `formatTeamName` 使用 `".."` 进行精准截断控制，配合 `truncate` 确保即使 3 个长名称战队也能在单行内完整并排展示，彻底杜绝换行；
   - 鼠标悬浮时通过 `VTooltip` 完整显示原战队名称。

---

## 3. 安全隐患防范与架构边界

1. **零注释与代码纯洁性**：所有新增及调整的代码文件（`.vue`、`.ts`）严格遵循《纯粹代码与零注释铁律》，消除所有行内注释与解释性备注，统一由本设计文档溯源；
2. **样式无冲突**：Docker 与 Medal 图标与各状态条均采用精准 Tailwind 颜色与 Naive UI 图标，Preflight 安全隔离，杜绝 CSS 污染；
3. **资源泄漏防御**：弹窗中的靶机租期计时器在弹窗关闭（`watch: show`）与组件卸载（`onUnmounted`）阶段强制执行 `clearInterval` 销毁，避免内存泄漏与后台无意义消耗。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/types/competition/index.ts`
  - `src/views/competition/components/CtfCompetitionChallengeCard.vue`
  - `src/views/competition/components/CtfCompetitionChallengeModal.vue`
  - `src/views/competition/components/index.ts`
  - `src/views/competition/tabs/CtfCompetitionChallengeListView.vue`
  - `src/views/competition/CTFCompetitionView.vue`
- **验证结果**：
  - 执行 `vue-tsc --noEmit` 严格类型检查：通过（0 errors）；
  - 执行 `vite build` 生产构建：成功完成全量模块打包（`built in 14.77s`），无警告异常。

