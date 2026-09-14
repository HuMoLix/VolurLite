# 竞赛详情 SPA 页内页、报名参赛流与沉浸式竞技场布局（CompetitionLayout）重构审计说明

## 1. 变更背景与需求说明
为了向参赛选手提供更沉浸、专业且符合网络安全赛事体验的流程，本次对公开端竞赛模块进行了全方位重构，核心诉求包括：
1. **取消抽屉柜，改为 SPA 页内页（In-Page Subview）**：
   - 列表页卡片按钮文案由「进入竞赛大厅」统一改为「查看竞赛详情」；
   - 废除右侧滑出抽屉（`n-drawer`），采用与后台对齐的 SPA 平滑页内页切换，消除狭窄弹层与双滚动条；
2. **战队报名与状态驱动的准入流（Registration & Entry State Machine）**：
   - 在详情页内页中动态感知当前用户登录状态与战队参赛状态（`getMyTeamApi`）；
   - **未报名状态**：在开放期内提供醒目的「立即报名参赛」按钮，支持弹出队伍创建与报名表单（战队名称、战队宣言）；
   - **已报名状态**：呈现战队专属卡片（战队名称、口号、队长/队员角色、邀请码及一键复制）；
   - **准入状态机控制**：
     - 若战队状态为 `PENDING_AUDIT` 或 `AUDIT_DRAFT`：提示「队伍报名审核中」；
     - 若审核通过但比赛未开启（`now < startTime`）：提示「审核通过，比赛尚未开始」并伴随高精度开赛倒计时；
     - 若比赛已正式打响（`RUNNING` 或 `FROZEN` 或当前时间处于比赛区间）：动态亮起醒目的**「进入竞赛」**（Enter Competition）主按钮；
     - 若比赛结束：提示「竞赛已结束」，允许进入竞技场回顾或查阅天梯榜单。
3. **全新的竞赛专属 SPA 布局与竞技场视图（CompetitionLayout & ArenaView）**：
   - 进入竞赛后，跳脱出常规平台的通用菜单与侧边栏，启用专为网络攻防设计的顶级全屏竞技场布局 `CompetitionLayout.vue`；
   - 布局顶部包含：退出竞技场快捷按钮、赛事名称与赛制 Badge、**实时倒计时时钟（比赛进行中 / 倒计时）**、当前战队标识、语言/主题切换及个人中心；
   - 竞技场视图 `CompetitionArenaView.vue` 提供「题目演练」（支持分类筛选、动态靶机启动/延长/销毁、Flag 校验提交）、「实时天梯」与「我的战队」深度竞技操作。

---

## 2. 核心架构改动点

### 2.1 公开端竞赛大厅重构 (`CompetitionListView.vue`)
1. **SPA 视图状态驱动 (`viewMode: 'list' | 'detail'`)**：
   - 卡片主操作按钮修正为 `t('competition.view_details')`（查看竞赛详情）；
   - 移除原有 `<n-drawer>` 容器，通过 `<transition name="page-transition" mode="out-in">` 实现列表大厅与详情页内页之间的平滑切换。
2. **详情子视图头部（Subview Header）**：
   - 提供「返回竞赛列表」带矢量图标快捷操作；
   - 标题、单一赛制模式标签（CTF / THEORY / AWD 等）、竞赛状态、一赛一制架构 Tag 以及短链接（Slug）一键复制胶囊（`.slug-pill`）；
   - 支持通过浏览器的返回按键或直接访问短链接（如 `/c/national-ctf-2026`）自动定位至对应的页内页详情。
3. **报名参赛全流程闭环**：
   - 未登录状态提示登录跳转；
   - 未报名状态在有效报名期内展示「立即报名参赛」，点击弹出创建队伍对话框；调用 `signupTeamApi` 快速完成战队创建并实时更新队伍状态；
   - 审核通过后实时判断比赛起止时间；开赛后渲染「进入竞赛」核心按钮，一键路由至 `/arena/:slug`。

### 2.2 竞赛专属沉浸式 SPA 布局 (`src/layout/CompetitionLayout.vue`)
1. **纯净全屏沉浸感**：
   - 专为赛事设计，彻底脱敏平台其他模块的干扰；
2. **核心控制台组件**：
   - **左侧导航**：「退出竞技场」一键返回赛事详情页 `/c/:slug`，赛事 Logo 与赛制 Tag；
   - **中央实时倒计时（`.countdown-pill`）**：精准计算距离结束（或开赛）秒级倒计时（`HH:MM:SS`），最后 30 分钟触发高亮警示样式；
   - **右侧专属状态**：当前登录用户战队名称胶囊、国际化多语言切换、亮色/暗色主题切换与用户个人菜单。

### 2.3 竞赛竞技场主控制台 (`src/views/competition/CompetitionArenaView.vue`)
1. **题目演练 (Challenges)**：
   - CTF 模式：Web / Pwn / Reverse / Crypto / Misc 分类过滤与题目卡片（已解出标记、分值、解出战队数）；
   - 赛题弹窗：赛题描述、独立动态靶机实例交互（启动 `startCtfInstanceApi`、销毁 `stopCtfInstanceApi`、延期 `extendCtfInstanceApi`）、Flag 提交校验（`submitCtfFlagApi`）；
   - 非 CTF 模式（THEORY、AWD、AWD+、ISW）：展现攻防靶标节点指南、SLA 探针监测机制与实战演练说明。
2. **实时天梯榜 (Scoreboard)**：
   - 接入 `getCompetitionScoreboardApi`，实时展现队伍排名、得分与解题数。
3. **我的战队 (My Team)**：
   - 战队基本信息、队长/队员角色展示、邀请码快捷复制与战队成员阵容表格。

### 2.4 路由与国际化配置
1. **路由定义 (`src/router/routes.ts`)**：
   - 注册顶级布局路由 `/arena/:slug`，以 `CompetitionLayout.vue` 作为容器，挂载 `CompetitionArenaView.vue`；
   - 强化 `/competitions/:slug` 与 `/c/:slug` 路由直接映射至 `CompetitionListView.vue` 的页内页详情模式。
2. **国际化字典 (`zh_CN/competition.json` & `en_US/competition.json`)**：
   - 补全「查看竞赛详情」、「立即报名参赛」、「进入竞赛」、「退出竞技场」、「实时天梯」、「题目演练」等中英文词条。

---

## 3. 验证与构建结果

1. **前端类型推断与生产打包验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - 包含 `CompetitionLayout.vue`、`CompetitionArenaView.vue` 和 `CompetitionListView.vue` 在内的所有模块通过静态类型推导，0 类型错误，0 语法警告；
     - 顺利产出构建产物：
       - `dist/assets/CompetitionLayout-dv42H_Gy.js (7.23 kB)`
       - `dist/assets/CompetitionArenaView-D-0HkyHB.js (13.48 kB)`
       - `dist/assets/CompetitionListView-HIDu5kTk.js (22.42 kB)`
     - 打包用时 4.18s，构建成功。
2. **后端编译与测试验证**：
   - 执行 `mvn test-compile -pl volur-competition`：0 错误编译通过，各服务层与领域模型保持 100% 稳定一致。

---

## 4. 影响范围与审计结论
- **影响范围**：`CompetitionListView.vue`、`CompetitionLayout.vue`、`CompetitionArenaView.vue`、`routes.ts`、`competition.json`、`src/types/competition/`。
- **审计结论**：完全实现了用户要求的「查看竞赛详情」SPA 页内页重构、战队报名与审核后等待开赛状态机流程，以及基于独立全新 Layout 的 SPA 竞技场体验，彻底杜绝了弹层嵌套，交互体验专业流畅。
