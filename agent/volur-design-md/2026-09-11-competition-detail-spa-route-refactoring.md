# 竞赛详情由页内页升级为独立 SPA 路由页面（CompetitionDetailView）重构审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「查看竞赛详情不要使用页内页了，也改成SPA。」

在之前的设计中，竞赛详情是作为 `CompetitionListView.vue` 内部的一个条件渲染子视图（`viewMode === 'detail'`）。
为了提供更加标准、解耦且具备独立 URL 地址状态的单页应用（SPA）体验，本次任务将竞赛详情彻底抽离为独立的 SPA 页面视图组件 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)，并与路由、大厅列表页、竞技场布局实现标准解耦。

---

## 2. 核心架构改动点

### 2.1 模块解耦与独立 SPA 详情视图 (`CompetitionDetailView.vue`)
1. **纯净独立的 SPA 页面**：
   - 移除在 `CompetitionListView.vue` 中侵入式混编的详情状态；
   - 新建 [`src/views/competition/CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)；
   - 页面直接由 Vue Router 路由注入 `slug` 参数，独立负责加载赛事详情（`getCompetitionBySlugApi`）、监听登录用户战队状态（`getMyTeamApi`）与开赛倒计时。
2. **完整的报名参赛状态机与进入竞赛控制**：
   - **未登录**：引导至登录页面并保留回跳参数；
   - **未报名**：在有效报名期内展示「立即报名参赛」按钮，弹窗填写战队名称与宣言，调用 `signupTeamApi`；
   - **已报名**：呈现战队卡片与专属邀请码（支持一键复制）；
   - **审核与入场判定**：
     - `PENDING_AUDIT` / `AUDIT_DRAFT`：提示「队伍报名审核中」；
     - `AUDIT_REJECTED`：提示「队伍审核未通过」；
     - `AUDIT_PASSED`：
       - 比赛未开始：展示开赛高精度倒计时；
       - 比赛进行中（`RUNNING` 或处于比赛窗口内）：动态点亮核心 **「进入竞赛」**（`enter_arena`）大按钮，一键路由跳转至全屏沉浸式竞技场 `/arena/:slug`；
       - 比赛已结束：展示「进入竞赛（查看天梯）」。

### 2.2 竞赛列表视图瘦身 (`CompetitionListView.vue`)
1. **聚焦列表大厅职责**：
   - 彻底移除了 `viewMode` 状态机分支；
   - 专注于呈现竞赛卡片、多赛制模式筛选 Tabs（CTF / THEORY / AWD / AWD+ / ISW / 全部赛制）、关键字/短链接联合检索（后端已支持 Title + Slug 联合匹配）及分页器。
2. **标准 SPA 路由跳转**：
   - 点击卡片或操作栏「查看竞赛详情」按钮，执行 `router.push('/c/' + comp.slug)`；
   - 点击短链接胶囊（`.slug-pill`）或卡片「复制短链接」按钮，一键将完整直达链接 `${originUrl}/c/${comp.slug}` 写入剪贴板。

### 2.3 路由中心重组 (`src/router/routes.ts`)
1. **公开端路由解耦**：
   - `/competitions` -> `CompetitionListView.vue`（竞赛列表大厅）；
   - `/competitions/:slug` -> `CompetitionDetailView.vue`（独立竞赛详情页）；
   - `/c/:slug` -> `CompetitionDetailView.vue`（独立短链接直达详情页）；
2. **全屏沉浸式竞技场路由**：
   - `/arena/:slug` -> `CompetitionLayout.vue`（专有赛事顶栏与倒计时容器） + `CompetitionArenaView.vue`（题目演练、靶机控制、Flag 提交、实时天梯）。

---

## 3. 验证与构建结果

1. **前端工程编译打包**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态类型推断 100% 通过（0 错误，0 告警）；
     - 各 SPA 视图模块打包产物尺寸：
       - `CompetitionListView`: 8.80 kB (gzip: 3.61 kB)
       - `CompetitionDetailView`: 15.06 kB (gzip: 5.61 kB)
       - `CompetitionLayout`: 7.23 kB (gzip: 2.80 kB)
       - `CompetitionArenaView`: 13.48 kB (gzip: 5.50 kB)
     - 打包用时 4.05s，构建通过。
2. **后端编译验证**：
   - 执行 `mvn test-compile -pl volur-competition`：0 错误编译通过。

---

## 4. 影响范围与审计结论
- **影响范围**：`CompetitionListView.vue`、`CompetitionDetailView.vue`、`CompetitionArenaView.vue`、`CompetitionLayout.vue`、`routes.ts`。
- **审计结论**：全面摒弃了单组件内嵌页内页的设计，构建了规范的「大厅列表 -> 独立详情页面 -> 专属竞技场」三层标准 SPA 路由拓扑，完全符合 Volur Lite 前端规范与现代 Web 开发体验。
