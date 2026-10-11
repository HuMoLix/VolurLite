# TheoryCompetitionView 与 CompetitionLayout 架构落地审计文档

## 1. 变更背景与需求概述
在 Volur Lite NEXT 前端工程中，为支持理论知识类竞赛（Theory Exam）的完整答题流程与通用竞赛竞技场框架，需在前端落地以下核心组件与模块：
1. 在 `src/views/competition/` 目录下创建 `TheoryCompetitionView.vue`，作为理论竞赛核心视图；
2. 在 `src/components/` 目录下创建 `competition/theory/` 组件模块体系，沉淀高复用、高度内聚的理论答题卡、试题渲染器与成绩弹窗；
3. 在 `src/components/layouts/` 目录下创建 `CompetitionLayout.vue`，作为全局竞技场工作台通用基础骨架布局，并纳入布局模块集中导出。

---

## 2. 架构设计与改动点详情

### 2.1 通用竞技场布局 `CompetitionLayout.vue`
- **文件路径**：`src/components/layouts/CompetitionLayout.vue`
- **导出索引**：`src/components/layouts/index.ts`
- **核心职能**：
  - **顶栏 Header 规范**：
    - **Header Left**：支持退出/返回按钮（自动智能解析目标路由或上一级竞赛列表），垂直分割线，Logo 品牌标识，以及自适应动态排版与截断的赛事标题；
    - **Header Center**：内置高精度实时倒计时胶囊（Countdown Pill），智能判定“距离考试开始”、“考试进行中”与“考核已结束”三个阶段，并在剩余时间低于阈值（默认 15 分钟）时触发红显呼吸预警动效；
    - **Header Right**：支持队伍信息胶囊（Team Pill）或考生身份胶囊（Candidate Pill）、中英文快速无缝切换、日夜间主题切换以及集成个人中心和安全退出（Logout）的下拉菜单（Dropdown）；
  - **移动端适配**：在小屏设备下，倒计时状态栏自动折叠并就近吸顶展示，保障紧凑可用性；
  - **主内容区与底栏**：`<main>` 区域无缝承载 `<slot><router-view /></slot>`，底栏默认集成 `AppFooter` 并支持插槽自定义重写。

### 2.2 理论答题专属组件库 `src/components/competition/theory/`
- **`TheoryQuestionCard.vue`**：
  - 支持单选题（`SINGLE_CHOICE`）、多选题（`MULTIPLE_CHOICE`）、判断题（`TRUE_FALSE`）与简答题（`ESSAY`）全题型；
  - 自动安全解析试题选项 JSON（数组或对象键值对）并规范生成 A/B/C/D 药丸标记选项；
  - 支持“标记待查（Bookmark for review）”状态切换；
  - 支持答卷提交后的只读（Readonly）复核状态。
- **`TheoryPaperSheet.vue`**：
  - 试卷全景答题卡浮层与快速索引跳题面板；
  - 实时统计“已答题数 / 总题数”，动态计算完成进度百分比与进度条；
  - 状态图例清晰区分已答、未答、待查标记以及当前焦点的题号方块；
  - 底部集成高对比度防重复提交交卷动作按钮，提交后就地展示得分徽章。
- **`TheoryResultModal.vue`**：
  - 试卷提交后的成绩报告对话框，展示最终得分、正确率、正确题数以及复核入口。
- **导出索引**：
  - `src/components/competition/theory/index.ts`
  - `src/components/competition/index.ts`

### 2.3 理论竞赛核心视图 `TheoryCompetitionView.vue`
- **文件路径**：`src/views/competition/TheoryCompetitionView.vue`
- **核心职能**：
  - 继承自 `CompetitionLayout`，模式标记为 `mode="theory"`；
  - 响应式监听路由参数（支持 `slug`、`id` 或 `competitionId`），自动发起数据请求拉取赛事详情与专属理论试卷；
  - 维护用户本地答题状态树与标记集合，支持键盘快捷键（←/→ 左右方向键快速切题）；
  - 答卷提交前提供二次确认弹窗，明确提示未作答题数，杜绝误触；
  - 倒计时截止（`@time-up`）时自动快速失败或触发交卷逻辑。

### 2.4 领域类型与 API 请求层 1:1 对齐
- **类型定义** (`src/types/competition/index.ts`)：
  - `TheoryType`：`'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'ESSAY'`
  - `TheoryPaperQuestionVO`、`TheoryPaperExamVO`、`TheoryAnswerItemDTO`、`TheoryPaperSubmitRequestDTO`、`TheoryPaperResultVO`
- **API 接口** (`src/api/competition.ts`)：
  - `getTheoryPaperApi(competitionId)`：对齐后端 `GET /api/v1/competitions/{competitionId}/theory/paper`
  - `submitTheoryPaperApi(competitionId, data)`：对齐后端 `POST /api/v1/competitions/{competitionId}/theory/submit`
- **多语言适配**：
  - 同步补充 `zh-CN`、`en-US`、`zh-TW` 下所有理论试卷与竞技场相关国际化词条。

---

## 3. 规范审查与代码洁净度检验
1. **纯粹代码与零注释铁律**：
   - 经审查与 `grep` 全量扫描，本次所有新增与修改的代码文件（`.vue`、`.ts`）无任何解释性注释、设计备注或冗余行内备注，完全依赖自解释语义代码；
2. **Google 风格色彩规范**：
   - 全面使用 Google Blue（`#1a73e8` / `#8ab4f8`）、Google Green、Google Yellow 等设计令牌；
   - 彻底杜绝纯黑（`#000000`）正文，严格对齐 Google Gray 阶梯体系；
3. **TypeScript 严格编译与构建验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，无任何类型错误或警告，产物体积正常。
