# 前端全站组件国际化 (i18n) 建设与中英文语言包落地审计

## 变更背景
为了满足 Volur Lite 全球化及双语（中文 / 英文）赛事实战演练需求，前端系统需要对所有视图（Views）、组件（Components）、布局框架（Layouts）以及路由导航元信息（Route Meta）进行完整的国际化治理，消除硬编码文本，支持动态语言切换响应式刷新。

## 改动点详述

### 1. 国际化资源配置扩充 (`src/locales/`)
根据模块领域对中英文语言包进行了细致拆分与完善：
- **`common.json`**：公共操作与控件文本（搜索、状态、操作、模态框交互、主题/语言切换 Tooltip 提示等）；
- **`layout.json`**：前台与管理端侧边栏导航菜单、面包屑、用户下拉菜单、底部版权及平台名称；
- **`home.json`**：平台落地页 Hero 标语、操作入口及各对抗赛型（CTF、AWD/AWD+、ISW 靶场、动态积分榜）特性卡片描述；
- **`competition.json`**：竞赛演练列表、赛事状态（未开始/进行中/已结束）、赛程时间、战队/选手规模格式化及进入大厅提示；
- **`challenge.json`**：题库训练大厅、搜索过滤、题型分类标签（Web/Pwn/Reverse/Crypto/Misc）、解题统计及查看详情；
- **`admin.json`**：管理控制台概览仪表盘（场次、题库、用户、容器指标统计）及用户、题库（CTF/理论/ISW/AWD/AWD+）、竞赛、公告、容器、日志、系统设置各子模块标题与描述；
- **`error.json`**：404 资源未找到页面提示及返回首页入口。

### 2. 布局组件响应式国际化 (`src/layout/`)
- **`AppLayout.vue`**：
  - 侧边栏菜单通过 `computed<MenuOption[]>` 封装，响应式监听语言变更；
  - 底部管理员面板悬浮提示与文字通过 `t('layout.admin_panel')` 国际化；
  - 顶部面包屑支持 `route.meta.titleKey` 动态翻译；
  - 顶部语言/暗色主题切换按钮的 Tooltip 动态国际化；
  - 顶部用户下拉菜单及底部版权声明完成 i18n 覆盖。
- **`AdminLayout.vue`**：
  - 多级侧边栏菜单、底部返回前台按钮通过 `computed` 和 `t()` 动态翻译；
  - 面包屑 `parentRouteTitle` 与 `currentRouteTitle` 响应式展示当前语言；
  - 管理端顶部及底部版权信息全量国际化。
- **`AuthLayout.vue`**：
  - 左下角语言切换与主题切换按钮 Tooltip 完成国际化。

### 3. 页面视图组件国际化覆盖 (`src/views/`)
- **`HomeView.vue`**：Slogan、行动按钮与 4 大赛型卡片使用 `t('home.*')`；
- **`CompetitionListView.vue`**：赛事大厅标题、状态判定函数与格式化规模文本使用 `t('competition.*')`；
- **`ChallengeListView.vue`**：题库搜索框、分类 Tabs、分值/解出人数与卡片操作使用 `t('challenge.*')`；
- **`NotFoundView.vue`**：404 结果页标题与返回首页按钮使用 `t('error.*')`；
- **`ResetView.vue`**：清理遗留中文控制台报错，规范日志输出；
- **管理端全量视图**（`AdminDashboardView.vue`、`AdminUserView.vue`、`AdminChallengeView.vue`、`AdminCtfChallengeView.vue`、`AdminTheoryChallengeView.vue`、`AdminIswChallengeView.vue`、`AdminAwdChallengeView.vue`、`AdminAwdPlusChallengeView.vue`、`AdminCompetitionView.vue`、`AdminBroadcastView.vue`、`AdminContainerView.vue`、`AdminLogView.vue`、`AdminSystemView.vue`）：
  - 标题、副标题与占位符描述全部使用 `t('admin.*')`。

### 4. 路由系统与类型增强 (`src/router/`, `src/types/router.d.ts`)
- 在 `RouteMeta` 中扩展 `titleKey`、`parentTitleKey` 和 `parentTitle` 类型声明；
- 在 `routes.ts` 中为前台和后台全部路由挂载对应的 `titleKey`；
- 在 `router/index.ts` 导航守卫中利用 `i18n.global.t()` 实现浏览器 Tab 页签动态标题国际化。

## 安全与影响范围分析
1. **类型安全与零破坏性**：所有 Vue 组件与 TS 文件经过 `vue-tsc` 严格类型校验和 Vite 生产环境构建，无编译错误；
2. **状态与持久化**：用户语言选择持久化在 `localStorage`（键名 `locale`），Naive UI 组件库与 Day.js 时间本地化配置联动保持一致；
3. **用户体验**：切换中英文时界面无需重新加载页面，所有导航、面包屑、表格/列表标签与卡片即时平滑刷新。
