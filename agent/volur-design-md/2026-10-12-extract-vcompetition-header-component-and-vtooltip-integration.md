# VCompetitionHeader 组件提取与 VTooltip 统一顶栏交互设计方案

## 1. 变更背景与目标
在 Volur Lite NEXT 前端工程中，`TheoryCompetitionView.vue`（理论知识考核考场）与 `CtfCompetitionView`（CTF 解题攻防竞技场）均采用统一的竞赛考场顶栏框架。然而此前该顶栏的核心 DOM 结构与大量逻辑（倒计时状态机、视口自适应标题截断、退出路由拦截、多语言及暗黑主题切换、用户菜单交互）均耦合平铺在 `CompetitionLayout.vue` 内部，导致容器组件职责混杂且达到 500 行代码。

同时，顶栏中原生的返回按钮、考题标题、倒计时徽章、战队身份、语言切换与主题切换按钮等均使用浏览器的原生 HTML `:title` 属性，存在样式风格与系统 Google Workspace 设计不统一、延迟高且无暗色模式适配等问题。

本次变更目标：
1. 从 `TheoryCompetitionView.vue` 与 `CtfCompetitionView` 所承载的竞赛顶栏中，正式提取出高内聚、易维护的 `VCompetitionHeader.vue` 全局竞赛顶栏组件；
2. 在 `VCompetitionHeader.vue` 中全面集成并赋能 `VTooltip.vue`，彻底消除原生 `:title` 属性；
3. 将 `CompetitionLayout.vue` 彻底精简并解耦，将顶栏渲染与交互全权委托给 `VCompetitionHeader.vue`，同时保持所有插槽（`header-left`、`header-center`、`header-right`、`header-team`）的 100% 透传；
4. 在 `TheoryCompetitionView.vue` 中对齐 `CtfCompetitionView`，为其顶部概览指标卡片（总题数、试卷总分、最终得分）包裹 `VTooltip`，统一整个竞赛模块的悬浮信息感知。

## 2. 核心架构与落地细节

### 2.1 组件解耦与架构设计
- **组件命名与归属**：创建 `src/components/competition/VCompetitionHeader.vue`，并在 `src/components/competition/index.ts` 中集中导出 `VCompetitionHeader` 及相关类型；
- **接口定义与类型安全**：定义 `VCompetitionHeaderProps`，完整接收 `competition`、`mode`、`title`、`startTime`、`endTime`、`exitText`、`teamName`、`candidateName`、`warningThresholdMinutes` 等竞赛状态参数；
- **事件委派**：通过调用签名定义 `exit` 和 `time-up` 强类型 emits；
- **布局容器委托**：`CompetitionLayout.vue` 仅保留 `min-h-screen` 页面骨架、`<main>` 路由视图渲染与 `AppFooter` 渲染职责，代码行数从 500 行精简至约 110 行。

### 2.2 VTooltip 在顶栏中的深度集成
在 `VCompetitionHeader.vue` 中全面替换 HTML 原生 `:title` 提示：
1. **退出/返回按钮**：包裹 `VTooltip`，悬浮提示 `exitText` 或 `返回竞赛详情`；
2. **比赛标题自适应**：在 Logo 与标题外层包裹 `VTooltip` 并指定 `placement="bottom"`，当比赛全称在窄屏截断时支持悬浮查看完整标题；
3. **倒计时状态徽章**：中间的倒计时胶囊标签包裹 `VTooltip`，实时提示 `${countdownLabel}: ${countdownText}`；
4. **战队与考生身份**：
   - 战队标签包裹 `VTooltip`，展示 `参赛战队: ${computedTeamName}`；
   - 个人考生标签包裹 `VTooltip`，展示 `参赛选手: ${candidateDisplayName}`；
5. **系统语言切换**：语言切换按钮包裹 `VTooltip`，提示当前可切换的目标语言（如 `切换至英文` / `切换至中文`）；
6. **主题切换按钮**：暗色/亮色切换按钮包裹 `VTooltip`，提示 `切换亮色模式` / `切换暗色模式`；
7. **登录引导按钮**：未登录态下的快捷登录按钮包裹 `VTooltip`，提示 `登录`。

### 2.3 TheoryCompetitionView 数据卡片体验对齐
在 `TheoryCompetitionView.vue` 中，对齐此前在 `CtfCompetitionView` 中的改动：
- **总题数卡片**：包裹 `VTooltip`，提示“试卷考核题目总量”；
- **试卷总分卡片**：包裹 `VTooltip`，提示“理论知识考核试卷满分分值”；
- **最终得分卡片**：包裹 `VTooltip`，提示“提交后自动评卷计算的实际最终得分”。

## 3. 规范与质量验证
- **纯粹代码与零注释铁律**：所有修改的 `.vue`、`.ts` 文件均不包含任何行内解释性注释或临时备注，保持代码高纯净度与自解释性；
- **文档外置原则**：所有设计考量、接口改动与重构细节均已记录在本文档中；
- **构建与类型验证**：运行 `npm run build`（Vite + vue-tsc），生产包打包成功（0 错误，0 警告）。
