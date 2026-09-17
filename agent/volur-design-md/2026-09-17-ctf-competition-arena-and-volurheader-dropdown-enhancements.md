# VolurHeader 用户菜单增强与 CTF 竞技场视图规范化改造

- **文档名称**：VolurHeader 用户菜单增强与 CTF 竞技场视图规范化改造
- **变更日期**：2026-09-17
- **涉及模块**：
  - 前端组件与视图：`volur-lite-view`
  - 后端竞赛模块：`volur-competition`
- **受影响核心文件**：
  - [`VolurHeader.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurHeader.vue)（添加用户下拉菜单图标与分割线）
  - [`zh_CN/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json) / [`en_US/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/auth.json)（补充缺失的 `log_out` 与 `logout_success` 国际化键）
  - [`zh_CN/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/competition.json) / [`en_US/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/competition.json)（优化竞技场 Tabs 国际化标签）
  - [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CtfCompetitionArenaView.vue)（由 `CompetitionArenaView.vue` 重命名并重构升级）
  - [`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue)（保留向后兼容转发壳）
  - [`routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts)（更新路由指向 `CtfCompetitionArenaView.vue`）
  - [`api/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/competition/index.ts)（添加前台比赛公告接口 `listCompetitionAnnouncementsApi`）
  - [`CompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java)（添加前台比赛公告接口 `GET /{id}/announcements`）
  - [`CompetitionService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionService.java) / [`CompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java)（实现竞赛公告查询服务逻辑）
  - [`CompetitionMapper.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/mapper/CompetitionMapper.java)（添加公告实体到 VO 的映射器）

---

## 1. 变更背景与目标

1. **统一全局导航栏下拉菜单规范**：
   - 之前 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue) 中用户下拉菜单带有分割线与离子图标（`PersonOutline`、`LogOutOutline`），而全局组件 [`VolurHeader.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurHeader.vue) 仅有朴素的文字选项。需要为其补齐分割线与图标（`PersonOutline`、`ArrowBackOutline`、`LogOutOutline`）。
2. **完善 i18n 国际化键名缺失**：
   - [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue) 使用了 `t('auth.log_out')` 和 `t('auth.logout_success')`，但在语言包 `auth.json` 中缺失，需要补全中英文语言包映射。
3. **CTF 竞技场视图解耦重命名与功能扩展**：
   - 将原通用的 `CompetitionArenaView.vue` 明确并收敛命名为 [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CtfCompetitionArenaView.vue)；
   - 优化四大选项卡命名与结构：
     - “题目演练” ➔ **“题目列表”** (`challenges`)
     - 新增 **“比赛公告”** (`announcements`) 选项卡，支持 Markdown 渲染与置顶展示
     - “实时天梯” ➔ **“排行榜”** (`scoreboard`)
     - “我的战队” ➔ **“队伍信息”** (`team`)
   - 样式精细化调整：
     - 选项卡卡片背景容器 (`.arena-nav-bar`) 宽度调整为不铺满全宽，采用 `width: fit-content` 紧凑包裹 tabs 内容及内外边距；
     - 调宽竞技场主视口两侧边距 (`padding: 0 32px`)，提升页面视觉留白与宽屏阅读体验。

---

## 2. 具体改造细节

### 2.1 VolurHeader 下拉菜单增强
- 在 [`VolurHeader.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurHeader.vue) 中引入 `PersonOutline`、`ArrowBackOutline`、`LogOutOutline` 图标；
- 在 `defaultDropdownOptions` 计算属性中添加 `type: 'divider'` 分割线与对应的 `icon: () => h(NIcon, ...)` 声明；
- Admin 模式与 App 模式均具备标准分割线与图标。

### 2.2 i18n 国际化键值补充与对齐
- 在 [`zh_CN/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json) 与 [`en_US/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/auth.json) 中补充：
  - `"log_out": "退出登录"` / `"log_out": "Log Out"`
  - `"logout_success": "退出登录成功"` / `"logout_success": "Logged out successfully"`
- 在 [`zh_CN/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/competition.json) 与 [`en_US/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/competition.json) 中对齐标签文案：
  - `tab_challenges`: 题目列表 / Challenges
  - `tab_announcements`: 比赛公告 / Announcements
  - `tab_scoreboard`: 排行榜 / Leaderboard
  - `tab_my_team`: 队伍信息 / Team Info

### 2.3 后端比赛公告公共接口支持
- 在 [`CompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java) 新增公开接口：
  - `GET /api/v1/competitions/{id}/announcements`
- 在 [`CompetitionService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionService.java) 与 [`CompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java) 中注入 `CompetitionAnnouncementRepository`，调用 `findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc` 查询；
- 通过 [`CompetitionMapper.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/mapper/CompetitionMapper.java) 转换为 `CompetitionAnnouncementVO` 列表输出；
- 前端在 [`src/api/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/competition/index.ts) 暴露强类型方法 `listCompetitionAnnouncementsApi`。

### 2.4 CtfCompetitionArenaView 重构与样式打磨
- 完整重构 [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CtfCompetitionArenaView.vue)：
  - **选项卡卡片宽度**：将 `.arena-nav-bar` 调整为 `width: fit-content; max-width: 100%; display: inline-flex;`，不再横向撑满整行；
  - **主容器边距**：设置 `.competition-arena-view { padding: 0 32px; max-width: 1400px; margin: 0 auto; }`，使左右两边具有更舒适的视觉留白；
  - **比赛公告 Tab**：支持展示列表、置顶标签、Markdown 解析、发布人及时间格式化，支持一键无感刷新；
  - 路由与历史兼容：在 [`routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts) 中指向 `CtfCompetitionArenaView.vue`，同时保留 `CompetitionArenaView.vue` 作为转发代理。

---

## 3. 验证情况

1. **前端工程构建与类型检查**：
   - 执行 `npm run build` (`vue-tsc --noEmit && vite build`)；
   - 验证结果：TypeScript 类型检查通过，Vite 打包构建成功 (`✓ built in 5.37s`)，0 错误。
2. **后端工程编译**：
   - 执行 `mvn test-compile -pl volur-competition`；
   - 验证结果：`BUILD SUCCESS`，135 个源文件编译全部通过，0 错误。
