# 竞赛详情标题状态胶囊移除、CTF 路由对齐与队伍页面四层架构重构报告

## 1. 变更背景与目标

本次任务针对 Volur-Lite CTF 竞技场与竞赛详情视图进行系统性重构与体验优化，具体包含以下四个核心诉求：
1. **竞赛详情页精简**：移除 `CompetitionDetailView.vue` 标题后的“竞赛状态”胶囊标签，使详情页头部标题区域更聚焦；
2. **CTF 竞技场选项卡文案统一**：将 `CtfCompetitionView.vue` 中的 “赛题挑战” 调整为 “赛题列表”，“官方公告” 调整为 “公告列表”，“计分排行榜” 调整为 “排行榜”，“我的战队” 调整为 “我的队伍”；
3. **CTF 竞技场路由联动深化**：
   - 赛题列表对应 `/competitions/<competitionSlug>/ctf/challenges`
   - 公告列表对应 `/competitions/<competitionSlug>/ctf/annoucements`（及别名 `announcements`）
   - 排行榜对应 `/competitions/<competitionSlug>/ctf/scoreboard`
   - 我的队伍对应 `/competitions/<competitionSlug>/ctf/team`
   - 查看其他指定队伍对应 `/competitions/<competitionSlug>/ctf/team/<teamId>`
4. **CTF 队伍视图四层 Flex 布局重构 (`CtfCompetitionTeamView.vue`)**：
   - 整体使用 Flex column 架构自顶向下清晰划分四个业务层级；
   - 第一层：**概况卡片**，采用 Flex 布局，第一行为队伍名称（居中，`font-size: 30px`），第二行为排名、总分、强项、解题数、一血数量；
   - 第二层：**参赛成绩卡片**，列表展示队员个人成绩，包含序号、选手名称、角色、强项、解题数、一血数量、得分；
   - 第三层：**能力雷达卡片**，采用 Flex 横向/响应式布局，左侧为队伍分值雷达图，右侧为队伍解题数雷达图；
   - 第四层：**答题记录卡片**，列表展示队伍所有解题记录，包含序号、答题时间（`YYYY-MM-DD hh:mm:ss`）、答题人、题目名称、答题方式、题目类型、得分、是否一血，并支持 Page 分页机制。

---

## 2. 详细改动点与实现方案

### 2.1 竞赛详情视图状态胶囊移除
- **文件**：`src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue`
- **改动说明**：
  - 移除了标题后绑定的 `effectiveStatus` 状态胶囊 `span` 元素，保留竞赛类型标签；
  - 移除了随之闲置的 `getStatusBadgeClass` 辅助函数，保持代码整洁并杜绝 TypeScript TS6133 警告。

### 2.2 CTF 竞技场路由与选项卡对齐
- **文件**：
  - `src/volur-lite-view-NEXT/src/router/routes/competition.ts`
  - `src/volur-lite-view-NEXT/src/views/competition/CtfCompetitionView.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionScoreboardView.vue`
- **改动说明**：
  - 在 `routes/competition.ts` 中新增了 CTF 竞技场各二级路由：`/competitions/:id/ctf/challenges`、`/competitions/:id/ctf/annoucements`、`/competitions/:id/ctf/scoreboard`、`/competitions/:id/ctf/team` 与 `/competitions/:id/ctf/team/:teamId`，以及对应的短链接 `/c/:id/ctf/...`；
  - 在 `CtfCompetitionView.vue` 中将选项卡由本地简单 ref 升级为基于 `useRoute()` 路径双向联动的可写计算属性 `activeTab`，并在切换选项卡时调用 `router.push()` 推进相应 URL；
  - 选项卡标签文案 100% 严格对齐：“赛题列表”、“公告列表”、“排行榜”、“我的队伍”；
  - 在 `CtfCompetitionScoreboardView.vue` 中将队伍名称设置为可点击交互，点击任意队伍触发 `@select-team` 事件跳转至 `/competitions/<slug>/ctf/team/<teamId>` 查看该队伍完整战绩；
  - 访问根路径 `/competitions/:slug/ctf` 时自动平滑重定向至默认子路由 `/challenges`。

### 2.3 后端 API 补充与解题记录数据源保障
- **实体与控制器改动**：
  - `com.humolix.volur.competition.domain.vo.ctf.CtfTeamSolveRecordVO`：定义解题记录专用视图对象，包含 `id`、`challengeId`、`challengeName`、`category`、`submissionMode`、`userId`、`username`、`points`、`isFirstBlood`、`solvedAt` 等完整元数据；
  - `com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository`：新增 `findByCompetitionIdAndTeamIdWithDetails` 联表预加载查询；
  - `com.humolix.volur.competition.controller.v1.ctf.CtfCompetitionController`：新增 `GET /api/v1/competitions/{competitionId}/ctf/teams/{teamId}/solves` 接口；
  - `com.humolix.volur.competition.controller.v1.CompetitionTeamController`：新增 `GET /api/v1/competitions/{competitionId}/teams/{teamId}` 接口，支持查看他人队伍基本信息与成员列表。

### 2.4 队伍视图四层 Flex 布局重构 (`CtfCompetitionTeamView.vue`)
- **文件**：`src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionTeamView.vue`
- **结构与层次**：
  1. **层级 1 - 概况卡片**：
     - Flex column 布局；
     - 第一行：队伍头像与居中队伍名称，字体大小严格设定为 `30px`；
     - 第二行：5 项核心统计卡片网格，展示排名、总分、强项（自动统计队伍得分最高题型）、解题数、一血数量。
  2. **层级 2 - 参赛成绩卡片**：
     - 表格形式列出战队所有成员个人成绩；
     - 表头包含：序号、选手名称（附头像）、角色（队长/队员/领队标签）、强项、解题数、一血数量、个人得分。
  3. **层级 3 - 能力雷达卡片**：
     - Flex 响应式并列布局；
     - 基于纯原生 SVG 构建高性能多边形雷达图，遵循 Google Material Design 蓝绿主色调；
     - 左侧为 **队伍分值雷达图**（展示 Web、Pwn、Reverse、Crypto、Misc 等题型的实际总分及雷达多边形面积）；
     - 右侧为 **队伍解题数雷达图**（展示各题型解题数量多边形与端点数值）。
  4. **层级 4 - 答题记录卡片**：
     - 表格形式展示队伍答题流水记录；
     - 包含：序号、答题时间（严格格式化为 `YYYY-MM-DD hh:mm:ss`）、答题人、题目名称、答题方式（动态容器 Flag / 静态 Flag）、题目类型、得分、是否一血（高亮胶囊标签）；
     - 接入 Naive UI `NPagination` 支持页码切换与单页大小选择（Page 分页）。

---

## 3. 安全与合规性验证

1. **规范遵守情况**：
   - 严格遵循 `GEMINI.md` 零注释铁律，所有源文件均无冗余注释与行内备注；
   - 严格对齐 Google Workspace / Material Design 调色板与暗色模式阶梯；
   - 遵循 Thin Controller、Lombok 不可变依赖注入与强类型 DTO/VO 规范；
2. **构建与类型检查**：
   - 前端通过 `npx vue-tsc --noEmit` 严格类型检验，零错误；
   - 前端通过 `npm run build` Vite 生产打包验证成功；
   - 后端通过 `mvn compile -DskipTests` 编译验证成功。
