# 赛事大厅 CompetitionListView.vue 设计实现与通用路由重构

## 1. 变更背景与需求概述

针对 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`），根据认证模块（`src/views/auth`）所沉淀的 Google Material Design 3 (M3) 与 Google Workspace 现代设计规范，推进赛事模块核心页面与公共路由体系的建设与规范化重构：
1. **公共路由清理与 404 导航机制改造**：
   - 彻底删除 `src/volur-lite-view-NEXT/src/router/routes/common.ts` 中历史遗留的 `/v1/home`（`HomeView.vue`）路由配置；
   - 移除原有的 `/:pathMatch(.*)* -> /competitions` 粗暴重定向，改为统一导航至专门的 `/notFound` 页面；
   - 新增 Google Workspace 风格的 404 错误视图（`NotFoundView.vue`）。
2. **赛事大厅页面（CompetitionListView.vue）架构与视觉重构**：
   - 全面对齐 Google Material Design 3 色彩与表面阶梯体系（Google Blue `#1a73e8` / `#8ab4f8`、Google Gray 阶梯底色与卡片表面）；
   - 实现顶部 Google 药丸搜索框、头部竞技场 Banner、赛制动态筛选药丸标签（Filter Chips）、一行一个的卡片流（Single Item Per Row）、空状态重置与标准分页器；
   - 严格遵循视口左下角（`bottom: 20px; left: 20px; z-index: 1000;`）全局主题与多语言控制器铁律；
3. **真实对接后端 API 架构**：
   - 严格基于 `com.humolix.volur.competition.controller.v1.CompetitionController` 的 API 签名定义 TypeScript 实体与请求函数，杜绝任何假数据或隐式 Mock。
4. **规范与代码纯粹性铁律**：
   - 严格落实零注释铁律（纯粹代码，设计外置于本文档）；
   - 全量通过 `vue-tsc --noEmit && vite build` 强类型编译校验。

---

## 2. 详细改造内容与技术实现

### 2.1 路由层级重构 (`routes/common.ts` 与 `routes/competition.ts`)

- **清理旧路由**：
  移除 `V1Home`（`/v1/home`）及直接重定向至 `/competitions` 的通配路由。
- **新增 404 路由与未匹配捕获**：
  - 注册 `/notFound` 静态路由，元信息标记 `title: '404 - 页面未找到'`，对应组件异步加载 `@/views/error/NotFoundView.vue`；
  - 注册 `/:pathMatch(.*)*` 通配路由，所有未定义路径一律重定向至 `/notFound`。
- **赛事路由指向更新**：
  - 将 `competitionRoutes` 中的 `/competitions` 路径指向全新实现的 `@/views/competition/CompetitionListView.vue`。

### 2.2 404 错误视图设计 (`NotFoundView.vue`)

- **视觉风格**：
  - 采用 Google Workspace 阶梯表面背景（`bg-google-surface-light dark:bg-google-surface-dark`）；
  - 核心展示区域采用半透明大型等宽 `404` 标识，中心点缀 Google 四色语义光点（Blue、Red、Yellow、Green）；
  - 文字排版采用 Google Gray 字体阶梯（`text-google-text-primary` 与 `text-google-text-desc`）；
  - 操作区提供 Google Blue 药丸按钮“返回赛事大厅”及文本药丸按钮“返回上一页”；
  - 底部统一集成 `AppFooter`。

### 2.3 领域类型定义与 API 封装 (`types/competition` & `api/competition.ts`)

1. **类型对齐 (`src/types/competition/index.ts`)**：
   - `CompetitionType`：严格对齐后端枚举 `'CTF' | 'THEORY' | 'AWD' | 'AWD_PLUS' | 'ISW'`；
   - `CompetitionStatus`：严格对齐后端枚举 `'DRAFT' | 'PUBLISHED' | 'RUNNING' | 'ENDED' | 'ARCHIVED' | 'REGISTRATION' | 'FROZEN'`；
   - `CompetitionSummaryVO`：1:1 映射后端 `CompetitionSummaryVO` 字段（`id`, `title`, `slug`, `description`, `bannerUrl`, `type`, `status`, `regStartTime`, `regEndTime`, `startTime`, `endTime`, `createdAt`）；
   - `CompetitionDetailVO`、`CompetitionAboutVO`、`CompetitionAnnouncementVO`、`CompetitionQueryDTO`。
2. **API 封装 (`src/api/competition.ts`)**：
   - `getCompetitionListApi(params?: CompetitionQueryDTO)`：调用 `GET /competitions`，对接 `CompetitionController.listCompetitions`，入参支持 `page`、`size`、`keyword`；
   - `getCompetitionBySlugApi(slug: string)`：调用 `GET /competitions/by-slug/{slug}`；
   - `getCompetitionByIdApi(id: number | string)`：调用 `GET /competitions/{id}`；
   - `getCompetitionAboutApi(id: number | string)`：调用 `GET /competitions/{id}/about`；
   - `listAnnouncementsApi(id: number | string)`：调用 `GET /competitions/{id}/announcements`。
3. **图片处理工具 (`src/utils/imageUrl.ts`)**：
   - 封装 `formatBannerUrl`，兼容绝对 HTTP(S) 地址、Base64/Blob 与基于 `VITE_API_BASE_URL` 的服务端静态资源相对路径拼接。

### 2.4 多语言系统扩充 (`i18n` 与 `locales`)

- 新增 `competition` 命名空间：
  - `src/locales/zh-CN/competition.json`
  - `src/locales/en-US/competition.json`
  - `src/locales/zh-TW/competition.json`
- 扩充 `common.json`：
  - 补充 `not_found_title`、`not_found_desc`、`back_to_competitions`、`back_prev`。
- 更新 `src/i18n/index.ts`：
  - 静态预载入 `zhCnCompetition`；
  - 动态路由命名空间检测：为 `/competitions` 和 `/c/` 路径自动追加 `competition` 资源加载。

### 2.5 赛事大厅页面交互与视觉设计 (`CompetitionListView.vue`)

1. **顶部应用栏与 Google 搜索框**：
   - 固定于视口顶部的毛玻璃吸顶 Header；
   - 居中放置 Google 药丸搜索框（`rounded-full`、左置放大镜图标、右置快速清空按钮、回车触发查询）；
   - 右侧根据登录凭证（读取 `logged_in` Cookie）自适应展示“登录”或“个人中心”状态。
2. **顶部线性进度条**：
   - 当 `loading === true` 时，在页面最顶部展示 Google 经典双段交替平移的不确定线性进度动画（Indeterminate Linear Progress Bar），与 `AuthLayout` 动效保持同构。
3. **Hero 竞技场区域**：
   - 呈现高层次的赛事大厅标题与副标题，右侧装饰 Google 品牌四色点阵。
4. **动态赛制筛选药丸标签 (Material 3 Filter Chips)**：
   - 动态汇聚当前列表中呈现的赛制并计算各赛制下的赛事数量；
   - 激活标签采用高对比药丸高亮，内置计数微章；
   - 支持平滑滚动并自适应重置。
5. **赛事卡片流 (Single Item Per Row)**：
   - 卡片尺寸统一，外层采用柔和边框与微阴影，悬停时触发微位移与 Google Blue 浅光晕边框；
   - **封面区**：展示竞赛 Banner 图片（支持延迟加载与悬停微缩放），缺失时优雅降级为 Material 3 梯度背景配合奖杯矢量图与赛制徽记；
   - **核心信息区**：
     - 状态徽章：进行中（`RUNNING`，伴随绿色脉冲呼吸灯动效）、报名中/已发布（Google Blue）、封榜（Google Yellow）、已结束/已归档（Google Gray）；
     - 赛制类型胶囊标签；
     - 标题与简介（支持最多两行优雅截断 `line-clamp-2`）；
     - 比赛时间、报名时间与短链接快捷复制（点击后即时反馈成功状态）。
   - **操作区**：
     - Google Blue 药丸按钮“查看详情”，携带前进箭头。
6. **空状态与重置**：
   - 搜索或筛选无结果时，展示 Material 风格空状态面板与“重置筛选”按钮。
7. **分页与全局控制器**：
   - 数据总量超过单页条数时渲染 Naive UI 分页器；
   - 页面左下角严格固定亮暗主题切换按钮与多语言下拉切换菜单。

---

## 3. 影响范围与验证结果

- **影响模块**：
  - `src/volur-lite-view-NEXT/src/router/routes/common.ts`
  - `src/volur-lite-view-NEXT/src/router/routes/competition.ts`
  - `src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue`
  - `src/volur-lite-view-NEXT/src/views/error/NotFoundView.vue`
  - `src/volur-lite-view-NEXT/src/types/competition/index.ts`
  - `src/volur-lite-view-NEXT/src/api/competition.ts`
  - `src/volur-lite-view-NEXT/src/utils/imageUrl.ts`
  - `src/volur-lite-view-NEXT/src/i18n/index.ts`
  - `src/volur-lite-view-NEXT/src/locales/*`
- **构建验证**：
  - 执行 `vue-tsc --noEmit && vite build`，编译完全通过，无任何类型缺失或构建警告。
