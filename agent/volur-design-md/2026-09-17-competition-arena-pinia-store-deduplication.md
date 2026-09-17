# 竞技场 Pinia Store 集中管理与在途 Promise 防抖去重方案落地审计

## 1. 变更背景与目标
在先前的分析中确认：当选手访问 `/arena/{slug}` 路由时，由于父级布局组件 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue) 与子级页面视图 [`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue) 在同一路由加载时并发挂载，且两者均在各自的 `onMounted` 钩子中独立发起：
1. `GET /api/v1/competitions/by-slug/{slug}`（获取竞赛基本信息）
2. `GET /api/v1/competitions/{id}/teams/me`（获取我的战队信息）

导致在网络面板中观察到两次一模一样的 HTTP 请求。用户确认采用**方案 A（基于 Pinia Store 集中状态管理与在途 Promise 去重复用）**进行重构，从根源上消除冗余的网络传输并统一状态源。

---

## 2. 核心改动点与实现细节

### 2.1 升级 Pinia 全局状态管理 [`src/store/modules/competition.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/competition.ts)
1. **状态扩展**：
   - 增加 `myTeam: ref<MyTeamInfoVO | null>(null)`；
   - 增加 `teamLoading: ref<boolean>(false)`。
2. **在途 Promise 缓存池 (In-flight Promise Pool)**：
   - 声明 `inFlightCompPromises = new Map<string, Promise<CompetitionDetailVO | null>>()`；
   - 声明 `inFlightTeamPromises = new Map<string, Promise<MyTeamInfoVO | null>>()`。
3. **去重与防抖复用逻辑 (`fetchCompetitionBySlug`)**：
   - **命中本地缓存**：若无需强制刷新（`force = false`），且 `currentCompetition.value` 匹配目标 slug 或 ID，直接同步返回已缓存数据，0 次网络调用；
   - **命中在途请求**：若已有相同 slug 的 HTTP 请求正在飞行中，直接返回该在途 `Promise` 进行 `await`，实现并发合并（Deduplication）；
   - **初次发起请求**：将生成的异步 `Promise` 存入 `inFlightCompPromises`，在 `finally` 阶段自动移出，保证异常安全与缓存池清洁。
4. **战队详情复用逻辑 (`fetchMyTeam`)**：
   - 同理针对 `competitionId` 维护当前战队信息与在途 `inFlightTeamPromises`；
   - 支持 `fetchMyTeam(competitionId, force = false)`，允许选手主动切换到“我的战队”Tab 时传入 `force = true` 强制穿透网络拉取最新队伍状态。
5. **状态重置 (`clearCompetitionState`)**：
   - 在重置竞赛状态时同步清空全部在途 Promise 缓存，杜绝跨路由残留。

### 2.2 重构布局组件 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue)
- 移除组件内部平铺的直接 API 请求（`getCompetitionBySlugApi`、`getMyTeamApi`）以及局部的 `competition` / `teamName` `ref`；
- 接入 `useCompetitionStore()`：
  - `competition = computed(() => competitionStore.currentCompetition)`；
  - `teamName = computed(() => competitionStore.myTeam?.teamName || '')`；
- 在 `initArena()` 中统一调用 `await competitionStore.fetchCompetitionBySlug(slug.value)` 与 `await competitionStore.fetchMyTeam(comp.id)`；
- 增加对 `slug` 的 `watch` 监听，在切换比赛路由时响应式更新。

### 2.3 重构视图组件 [`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue)
- 移除直接的 `getCompetitionBySlugApi`、`getMyTeamApi` 导入与局部的 `competition`、`myTeam` `ref`；
- 接入 `useCompetitionStore()`：
  - `competition = computed(() => competitionStore.currentCompetition)`；
  - `myTeam = computed(() => competitionStore.myTeam)`；
  - `teamLoading = computed(() => competitionStore.teamLoading)`；
- 在 `init()` 中统一调用 `await competitionStore.fetchCompetitionBySlug(slug.value)`；
- 在 `handleTabChange` 中，当用户显式点击“我的战队”Tab 时，调用 `fetchMyTeam(true)` 强制拉取最新状态；
- 增加对 `slug` 的 `watch` 监听。

---

## 3. 运行成效对比

| 场景 | 重构前表现 | 方案 A 重构后表现 |
| :--- | :--- | :--- |
| **首次进入 `/arena/{slug}`** | `CompetitionLayout` 与 `CompetitionArenaView` 各自发起 1 次 `by-slug` 和 1 次 `teams/me`（**共 4 次请求**） | 命中在途 Promise 合并，`by-slug` 与 `teams/me` **严格各发起 1 次（共 2 次请求，网络开销直降 50%）** |
| **在竞技场内部切换 Tabs** | 切换战队 Tab 时容易状态不一致或多次重复读取 | 共享 Pinia `myTeam` 单一真实数据源，顶部 `.team-pill` 与战队面板完全同步 |
| **战队数据主动刷新** | 仅刷新视图内部，顶部栏队伍胶囊无法感知变化 | 触发 Store 级更新，页面内容与顶部栏战队胶囊全响应式自动同步 |

---

## 4. 验证与审计结果
1. **TypeScript 类型校验**：`npx vue-tsc --noEmit` 0 错误通过；
2. **生产构建**：`npm run build` 成功完成打包（耗时 5.27 秒）；
3. **安全与兼容性**：在途 Promise 机制具有自动异常捕获与 `finally` 清理，不会发生内存泄漏或 Promise 卡死挂起。
