# VolurEx 组件提取与竞赛布局继承重构文档

- **文档名称**：VolurEx 底部栏组件提取及竞技场通用布局抽象重构
- **变更日期**：2026-09-17
- **涉及模块**：`volur-lite-view` (前端自研组件库 `volurEx`)
- **受影响核心文件**：
  - [`VolurFooter.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurFooter.vue)（新提取通用底部栏组件）
  - [`components/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/index.ts)（导出 `VolurFooter` 及其类型）
  - [`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue)（切换使用 `VolurFooter`，移除冗余样式）
  - [`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue)（切换使用 `VolurFooter`，移除冗余样式）
  - [`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue)（切换使用 `VolurFooter`）
  - [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue)（新建竞赛通用骨架布局，集成通用 Header 与 Footer）
  - [`CtfCompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CtfCompetitionLayout.vue)（重构继承 `CompetitionLayout`）
  - [`TheoryCompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/TheoryCompetitionLayout.vue)（重构继承 `CompetitionLayout`）
  - [`competitions/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/index.ts)（导出通用 `CompetitionLayout`）
  - [`layouts/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/index.ts)（规范导出所有布局）

---

## 1. 变更背景与目标

1. **底部栏样式与组件收敛**：
   - 此前平台前台主布局 [`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue) 与管理后台布局 [`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue) 中的 Footer 均采用了带白色/深色背景色和固定边框的样式，与认证页面 [`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue) 简约轻量的透明背景、无边框风格不一致，且底部逻辑与备案标签分散冗余。
   - 需要参考 [`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue) 的无边框透明风格，提取高复用度、规范化的通用底部栏组件 [`VolurFooter.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurFooter.vue)，统一收敛在 `@/public/volurEx/components`。

2. **竞赛考场布局继承与抽象**：
   - 传统 CTF 竞技场布局 [`CtfCompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CtfCompetitionLayout.vue) 与理论考核竞技场布局 [`TheoryCompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/TheoryCompetitionLayout.vue) 存在高达 90% 以上的模板、样式与生命周期代码重叠（如自适应标题字号计算、倒计时轮询、头像下拉菜单、退赛确认等）。
   - 需要提取出通用的基础竞赛布局组件 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue)，Header 采用 `CtfCompetitionLayout` 的高规格实现（包含动态字号计算、倒计时与队伍/考生信息展示），Footer 采用新提取的 `VolurFooter` 组件，并使 `CtfCompetitionLayout` 和 `TheoryCompetitionLayout` 通过组件组合/继承模式复用此基础布局。

---

## 2. 详细改造点说明

### 2.1 提取通用底部栏组件 `VolurFooter.vue`
- **文件路径**：`src/volur-lite-view/src/public/volurEx/components/VolurFooter.vue`
- **设计规范与特性**：
  - **背景与边框**：默认透明背景 (`background-color: transparent;`)，默认不展示上边框 (`border: none;`)，契合现代轻量 UI 规范。
  - **主题与文字色彩**：严格遵循规范铁律，描述文本统一采用 `var(--v-text-desc, #444444)`，暗黑模式自适应为灰蓝文本；hover 状态高亮为 `var(--v-text-primary)`。
  - **自适应排版模式**：支持 `row`（默认横向包裹居中）与 `column`（纵向堆叠），支持自定义插槽 `#default`、`#text`、`#icp`、`#extra`。
  - **内置模式计算**：
    - `app`：`© ${year} ${platformName}. ${platformDesc}`
    - `admin`：`${platformName} Management Control System © ${year}`
    - `auth`：`© ${year} ${platformName}. All rights reserved.`
    - `competition`：`© ${year} ${platformName}. ${platformDesc || 'All rights reserved.'}`
    - `custom` / 传 `text`：优先渲染用户传入的版权文本。
  - **工信部备案展示**：联动 `systemStore.icpNumber`，支持显示备案图标 `/beian.png` 并外链跳转至工信部备案网。
  - **集中导出**：在 `components/index.ts` 集中导出 `VolurFooter` 与 `VolurFooterProps`。

### 2.2 改造既有布局的底部栏
- **`AppLayout.vue`**：
  - 移除 `<n-layout-footer bordered class="app-footer">` 及手写备案逻辑，使用 `<volur-footer mode="app" />`；
  - 清理未使用的 `NLayoutFooter` 与冗余样板代码；
  - 移除已废弃的 `.app-footer` 样式。
- **`AdminLayout.vue`**：
  - 移除 `<n-layout-footer bordered class="admin-footer">`，使用 `<volur-footer mode="admin" />`；
  - 清理未使用的 `NLayoutFooter` 与冗余状态；
  - 移除已废弃的 `.admin-footer` 样式。
- **`AuthLayout.vue`**：
  - 由 `@/components/common/Footer.vue` 升级迁移为统一的 `<volur-footer mode="auth" />`。

### 2.3 抽象创建 `CompetitionLayout.vue`
- **文件路径**：`src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue`
- **核心职能**：
  - **Header 顶栏**：全面采用原 `CtfCompetitionLayout` 规范：
    - 退出竞技场按钮（支持自定义文案 `exitText` 与自定义跳转路径 `exitRoute`）；
    - 平台 Logo 与赛事标题（内置 Canvas 文本测算与 `ResizeObserver` 动态平滑缩放字号 11px~16px，超长触发 ellipsis 截断）；
    - 居中倒计时（支持自定义各阶段提示词 `countdownLabels` 与时间紧迫红色预警状态 `warningThresholdMinutes`）；
    - 队伍胶囊信息（支持若无队伍时展示个人考生胶囊 `showCandidatePill`）；
    - 多语言切换、深浅主题切换、用户个人中心与退出下拉面板。
  - **Main 内容区**：默认提供带平滑路由过渡动效 (`arena-page-fade`) 的 `<router-view>`，同时支持具名与默认插槽覆盖自定义视图。
  - **Footer 底栏**：集成通用的 `<volur-footer mode="competition" />`，支持通过 `showFooter` 控制显隐及插槽重写。
  - **响应式断点**：完美覆盖宽屏、平板 (<=768px) 及小屏手机 (<=520px) 布局适配。

### 2.4 重构 `CtfCompetitionLayout` 与 `TheoryCompetitionLayout`
- **`CtfCompetitionLayout.vue`**：
  - 简化为继承 `CompetitionLayout`，传入 CTF 专有属性：`mode="ctf"`、`default-title="网络安全竞技场"`、`user-role-fallback="Player"`、`warning-threshold-minutes="30"`、`main-class="ctf-arena-main"`；
  - 转发所有可能的外部插槽（`header-left`、`header-center`、`header-right`、`header-team`、`footer` 及主插槽）。
- **`TheoryCompetitionLayout.vue`**：
  - 简化为继承 `CompetitionLayout`，传入理论考核专有属性：`mode="theory"`、`default-title="理论知识考核竞技场"`、`user-role-fallback="Candidate"`、`exit-text="退出考场"`、`warning-threshold-minutes="15"`、`show-candidate-pill="true"`、`main-class="theory-arena-main"`、考核阶段特定倒计时文案；
  - 转发外部插槽至底层布局。
- **模块导出统一**：
  - 在 `layouts/competitions/index.ts` 导出 `CompetitionLayout`、`CompetitionLayoutProps`、`CountdownLabelsConfig`、`CtfCompetitionLayout`、`TheoryCompetitionLayout`。
  - 在 `layouts/index.ts` 导出全部布局，确保与全站路由及模块引用的平滑兼容。

---

## 3. 验证与构建情况

- 执行工程全量类型校验与打包构建：
  ```bash
  npm run build (vue-tsc --noEmit && vite build)
  ```
- **验证结果**：
  - TypeScript 类型校验无任何报错；
  - Vite 打包构建成功 (`✓ built in 5.33s`)；
  - 现有路由 `/arena/:slug` 与 `CtfCompetitionLayout`、`TheoryCompetitionLayout` 引用 100% 保持向前兼容。

---

## 4. 补充微调：竞技场导航栏退出按钮纯图标化优化

- **变更背景**：竞技场顶栏左侧原本带有“退出竞技场”字样，占据较多横向空间，且在视觉上不够精炼。
- **改动点**：
  - 修改 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue)，删除按钮内部的“退出竞技场”文本；
  - 调整退出按钮为圆形 Quaternary 按钮（`quaternary circle size="small"`），内嵌 18px 的 `ArrowBackOutline` 箭头图标；
  - 将原 `exitText` 或 `t('competition.exit_arena', '返回竞赛详情')` 保留为按钮的 `:title` 悬浮 Tooltip 提示，兼顾无障碍访问与空间精简；
  - 动态字号计算机制 `adjustTitleFontSize` 自动根据缩小后的按钮宽度为长标题释放更多可用水平宽度。

