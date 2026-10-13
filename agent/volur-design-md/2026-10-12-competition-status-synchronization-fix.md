# 竞赛状态动态推导与跨层缓存同步修复方案

## 1. 变更背景与问题溯源

在竞赛详情视图（`CompetitionDetailView.vue`）中，当赛事到达开始时间时，页面中部的操作卡片由于绑定了基于动态时钟推导的计算属性 `isRunning`，能够实时翻转为“竞赛火热进行中”并展示“进入竞技场”按钮；然而顶部标题旁侧的状态胶囊与右侧赛程概况卡片中的“当前状态”却持续停留在“报名中”。与此同时，在赛事大厅列表视图（`CompetitionListView.vue`）中，该竞赛已被正常标记为“进行中”。

经全链路追踪，该缺陷由前后端两层原因叠加产生：
1. **前端状态源非单一与响应式割裂**：
   - 标题胶囊与右侧赛程概况直接绑定了后端返回的静态字段 `competition.status`，未与每秒递增的 `nowTimestamp` 时钟联动推导；
   - 页面内的状态判断逻辑（`hasStarted`、`isRunning`、`isUpcoming`、`isRegistrationOpen`）各自根据不同条件进行多重防御性检查，缺乏统一的状态单一真相源（Single Source of Truth）。
2. **后端缓存失效机制与 TTL 配置缺陷**：
   - 列表接口（`/api/v1/competitions/slice`）未走缓存，每次直接从数据库检索，并在映射 VO 时调用 `comp.refreshStatus()`，依据当前时间戳将实体状态更新为 `RUNNING` 并保存至 MySQL；
   - 列表接口在更新数据库记录时未主动驱逐 `detail:id` 与 `detail:slug` 的缓存；
   - 全局配置未给 `CacheNames.COMPETITION_STATUS` 指定专用 TTL，导致其继承了全局默认的 Redis L2 10 分钟长缓存与 Caffeine L1 30 秒缓存。若在开赛前曾访问过详情接口，该接口返回的 `status: REGISTRATION` 脏数据将持续滞留在缓存中长达 10 分钟，阻断了数据库最新状态的透出。

---

## 2. 改动详情

### 2.1 前端工程 (`src/volur-lite-view-NEXT`)

#### 1. `src/views/competition/CompetitionDetailView.vue`
- **引入 `effectiveStatus` 计算属性**：
  在 `<script setup>` 中建立响应式状态推导计算属性 `effectiveStatus`，与 `nowTimestamp` 紧密绑定，对齐服务端 `Competition.resolveStatusByTime()` 状态机流转规则（按时间自然流转 `DRAFT` -> `PUBLISHED` / `REGISTRATION` -> `RUNNING` -> `ENDED`，并保留 `FROZEN` 与 `ARCHIVED`）；
- **收敛所有状态衍生计算属性**：
  将 `hasStarted`、`isUpcoming`、`isRunning`、`isRegistrationOpen` 以及 `getRegistrationStatusTip()` 全面重构为基于 `effectiveStatus` 的纯函数映射，彻底杜绝多源分散判断；
- **重构模板绑定**：
  将顶部标题旁的状态胶囊与右侧赛程概况的“当前状态”统一绑定至 `effectiveStatus`。开赛时刻到达时，胶囊无需刷新页面即可由“报名中”自动实时变更为“进行中”（附带高亮呼吸绿点）。

#### 2. `src/components/competition/VCompetitionCard.vue`
- **引入 `effectiveStatus` 计算属性**：
  根据卡片传入的比赛开始/结束时间、报名开始/结束时间及当前时间戳动态推导卡片展示状态；
- **同步操作按钮行为**：
  将状态胶囊与卡片右侧快捷操作按钮（`actionButtonText`）重构为基于 `effectiveStatus` 驱动，防止卡片长时间停留于客户端时产生状态滞后。

#### 3. `src/components/common/types.ts`
- **类型修复**：
  将 `VTooltipProps` 中的 `width` 类型修正为 `number | 'trigger'`，严格对齐 Naive UI `NTooltip` 底层受控类型约束，消除 TypeScript 编译错误。

### 2.2 后端工程 (`src/volur-lite-platform`)

#### 1. `volur-server/src/main/resources/application.yml`
- **配置强一致超短缓存规格**：
  在 `volur.cache.caches` 节点中显式注册 `competition_status`：
  - `l1-enabled: false`（禁用进程内 Caffeine 缓存，防止节点间数据脏读）；
  - `l2-ttl: 5s`（设置 Redis 二级缓存极短生存期为 5 秒，严格契约化实现强一致性要求）。

#### 2. `volur-competition/.../CompetitionServiceImpl.java`
- **刷新状态时联动缓存驱逐**：
  在 `listCompetitions` 与 `listCompetitionSlice` 中，当 `comp.refreshStatus()` 判定状态自然迁移并持久化数据库后，同步调用 `evictCompetitionDetailCache(comp)` 主动驱逐 `detail:id:{id}`、`detail:slug:{slug}` 及 `about:{id}`；
- **详情反向刷新时联动驱逐**：
  在 `getCompetitionById` 与 `getCompetitionBySlug` 内部的回源逻辑中，若发生状态刷新，同样清理关联标识缓存；
- **代码整洁度**：
  遵循零注释原则，移除行内冗余备注。

---

## 3. 影响范围与测试验证

### 3.1 影响范围
- **前端视图**：竞赛详情页（`CompetitionDetailView`）、竞赛卡片组件（`VCompetitionCard`）；
- **后端服务**：竞赛分页与切片查询服务、竞赛详情与 Slug 详情服务、Redis 缓存命名空间 `vcache:competition_status:*`。

### 3.2 验证结果
1. **前端类型校验与构建**：执行 `vue-tsc --noEmit && vite build`，编译打包耗时 7.94s，0 错误；
2. **后端编译与单元测试**：
   - `mvn clean test-compile -pl volur-competition,volur-server` 构建成功；
   - `mvn test -pl volur-competition` 运行全部 44 个单元测试，100% 通过（0 失败，0 错误）。
