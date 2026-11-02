# 前端 components、views 目录职责治理与一级/二级 Layouts 分层架构重构

## 变更背景
在 `src/volur-lite-view-NEXT` 持续迭代过程中，组件与视图架构积累了多重历史债务：
1. **基础原子 UI 寄生于业务目录**：`VAvatar.vue`、`VCard.vue`、`VDrawer.vue` 长期寄居在 `src/components/sidebar/`，导致竞赛、后台管理、用户视图等跨业务域反向深度依赖；
2. **全局业务组件与局部私有子组件混乱**：`src/components/competition/` 与 `src/views/competition/components/` 职责交织，出现子组件跨域倒灌重导出；
3. **视图 (View) 与子面板 (Panel/Step/Tab) 混淆**：动态子组件被冠以 `*View.vue` 后缀，且大小写混杂（如 `pwdChallengeView.vue`、`sessionExpireView.vue`）；
4. **孤岛目录与死代码残留**：单文件独占目录（`components/google`）、空壳封装与错别字（`AdminContainerLanuchModal.vue`）、空目录（`views/user`）及废弃提供者（`NaiveProvider.vue`）；
5. **安全核验与认证体系割裂**：`components/security/` 与 `components/auth/` 相互隔离且封装方向倒置；
6. **管理端竞赛视图平铺失控**：18 个竞赛管理视图与 Tab 混居在 `src/views/admin/competition/`；
7. **布局层（Layouts）职责不明确，缺乏多级排版抽象**：无法清晰表达“总体框架布局（一级 Layout）”与“`<main>` 内部排版布局（二级 Layout）”的组合关系。

---

## 核心设计与分层架构

### 1. 一级总体布局 (`src/layouts/primary/`)
定义全视口级框架骨架：
- **`AppLayout.vue`**：平台主干布局，包含响应式折叠/展开 `<SideBar>`、头部插槽、`<main>` 内容区及移动端底部导航栏；
- **`AuthLayout.vue`**：认证中心布局，无 SideBar，水平垂直完全居中，具备 Google 质感认证卡片、顶部进度条、底部语言切换及备案/法务信息；
- **`CompetitionLayout.vue`**：竞赛竞技场沉浸式布局，无 SideBar，具备专用顶部竞赛状态栏 `VCompetitionHeader`（倒计时、战队指示、退赛控制）与 `<main>` 内容区；
- **`AdminLayout.vue`**：管理控制台布局，具备专用管理端侧边栏与主控台内容区；
- **`BlankLayout.vue`**：极简空白布局，无 SideBar，供 404 等独立场景。

### 2. 二级内容排版布局 (`src/layouts/secondary/`)
负责定义在 Primary Layout 的 `<main>` 内容区域内部的结构与栅格：
- **`ContainerLayout.vue`**：标准居中限宽容器布局，支持 `maxWidth`（`sm` 至 `7xl`、`full`）、`padding` 与 `gap` 控制，内含 `header`、`default`、`footer` 插槽；
- **`TwoColumnLayout.vue`**：左右双栏排版布局，支持 `sidebarPlacement`（`left` / `right`）、`sidebarWidth`、`stickySidebar` 与响应式断点折叠；
- **`TabLayout.vue`**：选项卡排版布局，内置吸顶 Tab 控制条与右侧辅助工具插槽；
- **`FullBleedLayout.vue`**：全宽流式铺满排版布局，支持 100% 高度拉伸与内部滚动隔离。

---

## 改动点与文件映射

### 1. 基础原子组件归位与孤岛吸收
- 将 `VAvatar.vue`、`VCard.vue`、`VDrawer.vue` 移至 `src/components/common/`，并在 `src/components/common/types.ts` 与 `index.ts` 中集中导出；
- 全链路替换全局 10 余处跨目录引用（从 `@/components/sidebar/VAvatar.vue` 等修正为 `@/components/common`）；
- 将 `src/components/google/GoogleTextField.vue` 移入 `src/components/auth/GoogleTextField.vue`，彻底删除 `src/components/google` 目录；
- 删除 `src/components/common/NaiveProvider.vue` 死代码，清除无用导出；
- 删除 `src/views/admin/container/components/AdminContainerLanuchModal.vue` 空壳包装，将 `AdminContainerLanuchModal.vue` 更名为 `components/AdminContainerLaunchModal.vue` 纠正错别字并修复引用；
- 删除 `src/views/user/` 空目录。

### 2. 独立顶级 `src/layouts/` 体系落地
- 新建 `src/layouts/`，迁移并升级 `AppLayout`、`AdminLayout`、`CompetitionLayout`、`AuthLayout` 与 `BlankLayout`；
- 新建 `src/layouts/secondary/`，提供 `ContainerLayout`、`TwoColumnLayout`、`TabLayout`、`FullBleedLayout`；
- 将 `src/components/common/Footer.vue` 移至 `src/layouts/components/AppFooter.vue`；
- 删除已废弃的 `src/components/layouts/`；
- 更新 `src/router/routes/` 中所有路由的一级布局引用路径为 `@/layouts`。

### 3. 认证体系 (Auth) 与安全核验收敛
- 将 `src/views/auth/sessionExpireView.vue` 重命名为 `src/views/auth/SessionExpireView.vue`（大驼峰）；
- 将 `src/views/auth/signin/challenge/` 下 7 个子面板重命名为大驼峰 `*Panel.vue`，并更新 `ChallengeView.vue` 调度；
- 将 `src/views/auth/recovery/challenge/recaptchaChallengeView.vue` 重命名为 `RecaptchaChallengePanel.vue`；
- 将 `src/views/auth/signup/protect/` 下保护步骤重命名为 `*Step.vue`，将 `src/views/auth/signup/steps/` 下步骤组件重命名为 `*Step.vue`，并更新 `SignUpView.vue` 调度；
- 将 `src/components/security/` 整合并入 `src/components/auth/verification/`，统一由 `@/components/auth` 向外导出，删除旧 `security` 目录。

### 4. 竞赛 (Competition) 模块理顺
- 将 `src/views/competition/tabs/` 中的 4 个子组件重命名为 `*Tab.vue`（`CtfChallengeTab.vue`, `CtfNoticeTab.vue`, `CtfScoreboardTab.vue`, `CtfTeamTab.vue`）；
- 更新 `CtfCompetitionView.vue` 适配全新 Tab 组件与 二级布局组合（`ContainerLayout` + `TabLayout`）；
- 升级 `CompetitionListView.vue`、`CompetitionDetailView.vue`、`TheoryCompetitionRequireView.vue`、`TheoryCompetitionView.vue` 全面采用二级布局 `ContainerLayout` 与 `TwoColumnLayout`；
- 清除 `src/views/competition/components/index.ts` 中的 `VWriteupModal` 倒灌重导出。

### 5. 后台管理端 (Admin) 竞赛视图子域归整
- 建立 `src/views/admin/competition/ctf/`，收纳 CTF 竞赛管理总控 `AdminCtfCompetitionEditView.vue` 及其 7 个子面板；
- 建立 `src/views/admin/competition/theory/`，收纳理论赛管理总控 `AdminTheoryCompetitionEditView.vue` 及其 6 个子面板；
- 更新 `AdminCompetitionEditView.vue` 动态分发引用；
- 更新 `src/router/routes/admin.ts` 中的路由引用路径。

---

## 验证与影响范围
- **编译检查**：全量执行 `vue-tsc --noEmit && vite build`，31,716 个模块编译构建耗时 13.80s，**0 报错、0 警告**；
- **依赖正交性**：彻底消除了 `sidebar` 内部组件被全站跨域倒灌依赖的顽疾，实现了原子组件（向下沉淀）、布局层（全局分级）、业务组件（领域收敛）、页面视图（路由独占）的严格单向依赖；
- **命名规范性**：消除了 Linux 环境下大小写不敏感导致的部署风险，严格区分真实路由视图（`*View.vue`）与子面板组件（`*Panel.vue` / `*Step.vue` / `*Tab.vue`）。
