# 竞技场顶部栏倒计时纯净化、标题自适应字号缩放及双重请求成因审计

## 1. 变更背景与目标
在前序对 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue) 顶部栏完成规范化提取与视觉对齐之后，存在以下两点体验改进与架构分析诉求：
1. **倒计时胶囊冗长文本清理**：顶部居中倒计时胶囊原包含 `比赛进行中 - 剩余时间:` 等长标签文本，占用大量横向物理空间，在非超大屏下极易挤压左右布局；
2. **竞赛标题禁止省略号并动态降级字号**：原标题样式存在 `max-width: 320px; overflow: hidden; text-overflow: ellipsis;`，超长比赛名称会被截断为 `...` 丢失关键信息。要求彻底禁用 `...`，并建立动态监测与自适应算法，当标题过长或屏幕空间受挤压时，动态调小字号避免压迫右侧工具与个人信息栏；
3. **深入剖析重复请求成因**：分析访问 `/arena/{slug}` 时，`/api/v1/competitions/by-slug/{slug}` 与 `/api/v1/competitions/{id}/teams/me` 分别被重复发起两次的原因与架构解决方案。

---

## 2. 施工改动与实现细节

### 2.1 倒计时纯净极简呈现
- **模板层**：从 `.countdown-pill` 中彻底删除了 `<span class="timer-label">{{ countdownLabel }}:</span>`；
- **交互与可访问性保留**：将计算出的状态描述通过 `:title="countdownLabel"` 挂载至胶囊容器上，在保持界面清爽（仅显示沙漏图标与单宽等宽字体倒计时数字 `00:00:00`）的同时，选手将鼠标悬停时仍可直观查看提示。

### 2.2 竞赛标题禁止截断与动态自适应字号算法
- **废除硬编码截断**：在 SCSS 样式中彻底移除了 `max-width: 320px;`、`overflow: hidden;` 与 `text-overflow: ellipsis;`，标题保留 `white-space: nowrap;` 并增加 `transition: font-size 0.15s ease;` 平滑过渡；
- **高精度轻量级测算引擎**：
  - 基于离屏 `CanvasRenderingContext2D` 的 `measureText` API，精确测量指定字号与字体族（`700 ${fontSize}px "Lato", system-ui, ...`）下的物理像素文本宽度；
  - 避免频繁读取导致浏览器重排（Reflow）的 DOM 抖动。
- **空间感知与字号比例自适应算法 (`adjustTitleFontSize`)**：
  - 读取顶部栏实际视口可用宽度（`headerWidth`）；
  - 扣除左侧除标题外的固定部件物理宽度（返回竞技大厅按钮、分割线、Volur 竞技场图标及 Flex 内部间距，合计约 `leftOtherWidth`）；
  - 扣除居中倒计时胶囊宽度（`centerWidth`）、右侧工具与战队/用户信息宽度（`rightWidth`）以及安全留白缓冲区；
  - 计算可供标题独占的最大无压迫宽度 `maxAvailableWidth`；
  - 若标准字号（16px）超出可用空间，则线性比例缩减：`scale = maxAvailableWidth / textWidthAtBase`，并在安全可读范围 `[11px, 16px]` 内自适应收敛；
- **响应式动态监听**：
  - 挂载 `ResizeObserver` 持续观测 `.arena-header` 尺寸变化；
  - 监听 `window.resize` 窗口变化；
  - `watch(competitionTitle)` 监听竞赛数据异步抵达时自动执行重算；
  - 在 `onUnmounted` 中严格注销所有 Observer 与事件监听，杜绝内存泄漏。

---

## 3. 双重网络请求深入根因剖析与解决方案

### 3.1 现象重现
访问 `/arena/test-comp-01` 路由时，网络面板中出现两次相同的接口调用：
1. `GET /api/v1/competitions/by-slug/test-comp-01`（2 次）
2. `GET /api/v1/competitions/358609931198267392/teams/me`（2 次）

### 3.2 根因定位
在 Vue Router 配置 [`src/volur-lite-view/src/router/routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts#L60-L71) 中：
- 父路由 `/arena/:slug` 挂载了布局组件 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue)；
- 子路由 `''` 挂载了视图组件 [`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue)。

两个组件在路由匹配时**同时被挂载**，且各自维护着独立的本地 `ref` 状态与生命周期钩子：
1. **父组件 `CompetitionLayout.vue`**：
   - 在其 `onMounted()` 中执行 `initArena()`；
   - 发起第 1 次 `getCompetitionBySlugApi(slug)`（获取竞赛详情以展示 Header 标题和倒计时）；
   - 响应成功后，携带返回的 `res.data.id` 发起第 1 次 `getMyTeamApi(res.data.id)`（获取战队名称以展示 Header 右侧队伍胶囊）。
2. **子组件 `CompetitionArenaView.vue`**：
   - 在其 `onMounted()` 中执行 `init()`；
   - 发起第 2 次 `getCompetitionBySlugApi(slug)`（获取竞赛类型及 ID 以初始化赛题与天梯榜）；
   - 响应成功后，调用 `fetchMyTeam()` 发起第 2 次 `getMyTeamApi(competition.value.id)`（获取战队详情以渲染“我的战队”Tab 面板）。

两者完全处于互相割裂的孤岛状态，既没有经过 Pinia 全局 Store 进行在途 Promise 复用/缓存，也没有通过 Vue 3 的 `provide / inject` 或父子 Props 传递数据，因而造成重复发起两次请求。

### 3.3 架构推荐解决方案

#### 方案一：基于 Pinia Store 集中管理与在途 Promise 防抖复用（推荐）
在现有的 `useCompetitionStore` (`src/store/modules/competition.ts`) 中维护 `currentCompetition`、`myTeam` 以及正在执行的 `fetchPromise`。当组件调用获取方法时，若存在同一 slug 在途请求，直接复用 Promise；若已有相同 slug 缓存数据则直接返回，彻底杜绝并行重复请求。

#### 方案二：父子组件 `provide / inject` 单一真实数据源
由父级 `CompetitionLayout.vue` 统一作为数据驱动源，负责加载竞赛和战队信息，并通过 Vue 3 的 `provide('arenaContext', { competition, myTeam, refreshTeam })` 注入；子级 `CompetitionArenaView.vue` 直接 `inject` 该数据源，省去独立的数据初始化逻辑。

---

## 4. 验证与审计结果
1. **构建与类型验证**：
   - `npx vue-tsc --noEmit`：0 错误通过；
   - `npm run build`：成功构建，耗时 5.48 秒；
2. **影响范围**：
   - 修改文件：[`src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue)。
