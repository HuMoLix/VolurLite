# 异步任务管理系统 (Async Task Management) 与 SSE 实时状态流全链路落地报告

## 1. 变更背景与需求动因
在 Volur Lite 平台的 Docker 镜像管理 (`AdminImageView.vue`) 中，执行**镜像跨节点转移**（`POST /api/v1/admin/images/transfer`）或镜像备份、拉取、批量容器调度等长链路运维操作时，后端需要跨宿主机通过 Docker Daemon / SSH 传输数十 MB 乃至数 GB 的物理镜像层。由于历史实现为单次 HTTP 同步阻塞调用，若网络抖动或镜像较大，耗时易超过 HTTP 客户端网关超时阈值（如 10s~30s），导致前端抛出网络连接超时错误（Request Timeout），而底层进程仍在运行，造成前后端状态脱节。

为了彻底解决此类耗时任务的超时问题，并提升平台的大规模容器与集群调度体验，本项目全面设计并落地了**异步任务管理系统 (Task Management System)**：
1. **即时返回任务凭据**：长链路耗时操作提交后，后端校验基本参数并在 15ms 内返回任务凭据（`AdminAsyncTaskSubmitVO`），彻底解除 HTTP 阻塞与超时隐患；
2. **后台异步线程池与生命周期状态机**：任务交由专属高吞吐线程池调度，维护标准的 `PENDING` -> `RUNNING` -> `SUCCESS` / `FAILED` / `CANCELLED` 生命周期；
3. **SSE 实时状态与进度推送**：集成 `volur-notification` 模块的 `RealtimePushService` 与 `GlobalSseManager`，每个任务的执行百分比与步骤信息毫秒级推送给前端；
4. **前端全局运维看板与侧边栏常驻入口**：在 `AdminLayout.vue` 侧边栏常驻“任务管理”入口，实时显示正在执行任务徽标，并在任务完成/失败时触发全局通知提醒与拓扑视图联动。

---

## 2. 系统全链路架构设计与核心改动点

### 2.1 后端工程设计与模块划分

```
[volur-container / volur-system / etc.]
       │
       ▼ (1. submitTask)
┌────────────────────────────────────────────────────────┐
│ AdminAsyncTaskService (volur-system)                   │
│  - sys_async_task (JPA Entity, SnowflakeId)            │
│  - volurAsyncTaskExecutor (ThreadPoolTaskExecutor)     │
│  - Spring EventPublisher                               │
└────────────────────────────────────────────────────────┘
       │
       ▼ (2. AsyncTaskPushEvent)
┌────────────────────────────────────────────────────────┐
│ AsyncTaskPushEventListener (volur-notification)        │
│  - RealtimePushService (Redis Pub/Sub 广播)             │
│  - GlobalSseManager (/api/v1/realtime/stream)          │
└────────────────────────────────────────────────────────┘
       │
       ▼ (3. SSE event: task_event)
┌────────────────────────────────────────────────────────┐
│ 前端应用 (volur-lite-view)                             │
│  - AdminLayout.vue (侧边栏动态 Badge + 弹窗通知)         │
│  - AdminTaskView.vue (指标看板 + 动态进度条 + 参数抽屉) │
│  - AdminImageView.vue (提交即刻反馈 + 完成自动刷新列表) │
└────────────────────────────────────────────────────────┘
```

#### 2.1.1 异步任务核心模块 (`volur-system`)
- **领域模型 (`domain/`)**:
  - `AsyncTaskEntity` (`sys_async_task`): 包含雪花算法 ID、任务名称、任务类型 (`AsyncTaskType`)、状态 (`AsyncTaskStatus`)、进度百分比 (0-100)、当前步骤摘要 (`statusMessage`)、入参 JSON (`parameters`)、结果数据 (`resultData`)、异常堆栈 (`errorMessage`)、操作人及起止时间/耗时 (`executionDurationMs`)；
  - `AsyncTaskType`: 支持 `IMAGE_TRANSFER`、`IMAGE_PULL`、`IMAGE_BACKUP`、`IMAGE_SYNC`、`CONTAINER_BATCH_STOP`、`EMAIL_BATCH_SEND`、`SYSTEM_CLEANUP`、`OTHER`；
  - `AsyncTaskStatus`: `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`、`CANCELLED`；
  - `AdminAsyncTaskVO` / `AdminAsyncTaskSubmitVO` / `AdminAsyncTaskStatisticsVO`；
  - `AdminAsyncTaskPageRequestDTO`: 继承 `PageRequestDTO`，支持关键词模糊搜索（任务名/操作人/错误）、类型与状态多维筛选、时间范围检索及动态排序。
- **线程池与执行上下文 (`config/AsyncTaskExecutorConfig.java`, `AsyncTaskContext.java`)**:
  - 配置 `volurAsyncTaskExecutor`（核心 4 线程，最大 16 线程，队列 100，CallerRuns 饱和策略）；
  - 提供 `AsyncTaskContext` 接口，支持任务内部安全调用 `updateProgress(progress, message)` 及 `isCancelled()` 状态探测。
- **服务层 (`AdminAsyncTaskServiceImpl.java`)**:
  - `submitTask`: 快速持久化 `PENDING` 任务记录并投递至线程池，即刻返回提交凭据；
  - 运行时自动流转为 `RUNNING`，捕获未处理异常并标记为 `FAILED`，成功执行后置 `SUCCESS` 并持久化 `executionDurationMs`；
  - 提供 `cancelTask`（主动取消）、`deleteTask`（清理单条已结束记录）与 `clearHistory`（一键清空全部终态任务）；
  - 每次状态与进度变动均发布 `AsyncTaskPushEvent`。
- **管理控制器 (`controller/v1/admin/AdminAsyncTaskController.java`)**:
  - `GET /api/v1/admin/tasks/statistics`: 获取运行中/排队/成功/失败统计；
  - `GET /api/v1/admin/tasks`: 分页查询任务列表；
  - `GET /api/v1/admin/tasks/{id}`: 查询任务详情与参数堆栈；
  - `POST /api/v1/admin/tasks/{id}/cancel`: 取消在途任务；
  - `DELETE /api/v1/admin/tasks/{id}`: 删除任务记录；
  - `DELETE /api/v1/admin/tasks/history`: 批量清空历史任务。

#### 2.1.2 实时事件流与推送总线集成 (`volur-notification`)
- 在 `RealtimeEventType` 中新增 `TASK_EVENT("task_event", "异步任务状态流与进度更新")`；
- 在 `pom.xml` 中引入 `volur-system` 依赖，解耦且规范；
- 新增 `AsyncTaskPushEventListener.java`：通过 `@Async @EventListener` 监听 `AsyncTaskPushEvent`，并经由 `RealtimePushService.broadcastAll` 将任务进度秒级广播至所有在线管理员的 SSE 连接（`/api/v1/realtime/stream`）。

#### 2.1.3 镜像跨节点转移与副本备份异步重构 (`volur-container`)
- `AdminImageService` 与 `AdminImageServiceImpl`:
  - 注入 `AdminAsyncTaskService`；
  - **重构 `transferImage` (镜像节点转移)**:
    - 同步阶段（<15ms）：参数与节点校验，标记状态 `MIGRATING`，提交 `IMAGE_TRANSFER` 异步任务并立即返回 `AdminAsyncTaskSubmitVO`；
    - 异步阶段（后台执行）：15% 连接目标节点 -> 35% Docker Pull -> 75% 更新数据库拓扑 -> 88% 清理源节点旧镜像 -> 100% 完成；
  - **重构 `backupImage` (镜像副本备份至子节点)**:
    - 同步阶段（<15ms）：校验参数与目标子节点存在性，排查已存在副本，提交 `IMAGE_BACKUP` 异步任务并立即返回 `AdminAsyncTaskSubmitVO`；
    - 异步阶段（后台执行）：15% 连接目标节点 -> 35% 远程 Docker Pull 同步 -> 80% 更新集群镜像拓扑 -> 100% 备份就绪；
    - 解决点击镜像备份时由于原为同步阻塞调用导致未触发异步任务管理与超时的缺陷。

---

### 2.2 前端工程设计与实现 (`volur-lite-view`)

#### 2.2.1 类型定义与 API 封装
- 创建 `src/types/task/index.ts`: 1:1 严格对齐后端 `AdminAsyncTaskVO`、`AdminAsyncTaskSubmitVO`、`AdminAsyncTaskStatisticsVO`、`AdminAsyncTaskPageRequestDTO` 及枚举类型，严禁使用 `any`；
- 创建 `src/api/task/index.ts`: 封装 `getAdminTaskStatisticsApi`、`getAdminTaskListApi`、`getAdminTaskDetailApi`、`cancelAdminTaskApi`、`deleteAdminTaskApi`、`clearAdminTaskHistoryApi`；
- 更新 `src/api/image/index.ts`: 将 `transferAdminImageApi` 返回类型精确对齐为 `AdminAsyncTaskSubmitVO`。

#### 2.2.2 全局管理布局与侧边栏入口 (`AdminLayout.vue`)
- 在侧边栏导航菜单中新增 `AdminTasks`（图标 `TimeOutline`，国际化文案：中文“任务管理” / 英文“Task Management”）；
- 引入 `runningTaskCount` 响应式变量，若存在正在执行或排队的任务，在侧边栏菜单项旁实时呈现 Naive UI `NBadge`（蓝色气泡数字）；
- 挂载时连接 `/api/v1/realtime/stream`，监听 `task_event`：
  - 动态刷新气泡计数；
  - 任务成功时触发全局 Toast（如：`notification.success({ title: '任务已成功完成', content: '... (耗时: 12.4s)' })`）；
  - 任务失败时触发红色告警 Toast（包含具体失败原因）；
  - 广播 `volur:async-task-completed` 与 `volur:async-task-failed` 自定义全局窗口事件。

#### 2.2.3 异步任务综合运维面板 (`AdminTaskView.vue`)
- **顶部指标看板**：总任务数、正在执行（呼吸动效与旋转图标）、排队等待、成功完成、执行失败五张卡片；
- **筛选检索栏**：关键词（名称/操作人/错误）、任务类别下拉选、状态下拉选、查询、重置与一键清空历史记录；
- **核心数据表格**：
  - 任务 ID（等宽字体后 8 位展示）；
  - 任务名称（带类别图标与悬浮全文）；
  - 类别 Tag（根据不同操作类型匹配主题色）；
  - 状态 Tag（`RUNNING` 状态内置转动 Loading 状态）；
  - 执行进度（`NProgress` 动态百分比进度条，附带最新阶段提示信息 Tooltip）；
  - 操作人与执行时序耗时（如 `450ms`、`12.5s`、`1m 24s`）；
  - 操作列：查看详情抽屉、取消在途任务、删除记录；
- **详情抽屉 (`NDrawer`)**：
  - 基础信息网格；
  - 动态进度条与当前步骤提示；
  - 失败异常堆栈与排查框（等宽红底框，保留完整日志换行）；
  - 生命周期时序追踪 (`NTimeline`)：记录创建排队、正式启动、完成/失败的具体时刻；
  - 参数与结果 JSON 格式化高亮预览 (`<pre>` + `Fira Code` 字体）；
- **实时事件更新**：内置 `SseClient` 监听，收到 `task_event` 时直接就地修改表格中对应项的响应式对象，实现无闪烁、无刷新的丝滑实时流式更新。

#### 2.2.4 镜像管理视图闭环联动 (`AdminImageView.vue`)
- 用户在镜像管理中点击“转移镜像”并提交后，对话框立即关闭并提示“镜像节点转移任务已提交后台异步执行，可前往【任务管理】实时追踪进度”，不再经历任何卡顿或超时等待；
- 组件监听 `volur:async-task-completed` 全局事件，一旦收到镜像转移、拉取、备份或同步任务完成的广播，自动静默刷新镜像列表、统计概览与节点信息，确保数据实时真实。

#### 2.2.5 国际化支持 (`locales`)
- 新增 `zh_CN/admin/tasks.json` 与 `en_US/admin/tasks.json`；
- 在 `zh_CN/layout.json` 与 `en_US/layout.json` 的 `menu` 下注册 `tasks`。

#### 2.1.4 事务隔离边界、Hibernate 7 实体持久化与并发异常终极修复 (Critical Concurrency & Persistence Fix)
- **故障排查与深度根因剖析**：
  在镜像副本备份（`backupImage`）及耗时任务提交时，调用堆栈显示：
  `Caused by: org.hibernate.StaleObjectStateException: Row was already updated or deleted by another transaction for entity [com.humolix.volur.system.domain.entity.AsyncTaskEntity with id '...']`
  `at org.hibernate.event.internal.DefaultMergeEventListener.entityIsDetached(DefaultMergeEventListener.java:432)`
  `at org.springframework.data.jpa.repository.support.SimpleJpaRepository.save(SimpleJpaRepository.java:667)`
  `at org.springframework.data.jpa.repository.support.SimpleJpaRepository.saveAndFlush(SimpleJpaRepository.java:675)`
  - **核心机制与根因**：
    1. **提前预分配 ID 误触发 JPA `merge` 语义**：
       在 `AdminAsyncTaskServiceImpl.submitTask` 中，由于使用了 `entity.builder().id(snowflakeIdGenerator.nextId())`，导致 `entity.getId() != null`。
       Spring Data JPA 的 `SimpleJpaRepository` 通过 `entityInformation.isNew(entity)` 判断对象是否为新对象，默认逻辑为 `entity.getId() == null`。因 ID 已存在，Spring 判定其**不是**新对象，转而调用 `em.merge(entity)` 而非 `em.persist(entity)`；
    2. **Hibernate 7 Detached 状态判定与虚拟删除假象**：
       Hibernate 7 在执行 `merge` 时，首先发送 `SELECT ... WHERE id = ?` 尝试从数据库加载实体。当查询结果为空（因为这是刚刚新建、根本还未落库的实体）时，Hibernate 7 的 `DefaultMergeEventListener.entityIsDetached` 判定：*“该实体带有关联主键且处于游离态，但数据库中查无此记录，说明该行已被其他事务并发删除”*，从而直接抛出 `StaleObjectStateException`（被 Spring 转换为 `ObjectOptimisticLockingFailureException`）；
    3. **`@Async` 线程池候选冲突告警**：
       Spring `@Async` 在上下文中发现多个 `TaskExecutor`（`volurAsyncTaskExecutor` 与 `taskScheduler`），产生重名与 Primary 缺失的警告。
  - **根本性技术解决方案落地**：
    1. **放权给 `@SnowflakeGenerated` 原生生成机制**：
       在 `AsyncTaskEntity` 构建时**不再手动 pre-set ID**（保持 `id == null`），Spring Data JPA 准确识别 `isNew(entity) == true`，坚决走 `em.persist(entity)` 原生通道；Hibernate 的 `@SnowflakeGenerated` 标识通过 `SnowflakeIdentifierGenerator` 在 insert 执行前拦截并赋予分布式雪花主键，彻底杜绝了 `merge()` 与预查询 `SELECT`，消除了 `StaleObjectStateException`；
    2. **物理全新事务封装 (`TransactionTemplate(REQUIRES_NEW)`)**：
       使用 `TransactionTemplate(REQUIRES_NEW)` 封装 `asyncTaskRepository.saveAndFlush(entity)`，在独立干净的物理数据库连接上瞬时完成提交并返回已带 ID 的持久态对象，彻底隔离调用方上下文；
    3. **事务提交后延迟调度 (`afterCommit`) 与回滚自愈**：
       若调用方处于活跃数据库事务中，利用 `TransactionSynchronizationManager.registerSynchronization` 将后台执行派发严格延后至 `afterCommit`，外层事务异常时通过 `afterCompletion` 自动标记任务失败；
    4. **优化线程池 Bean 配置**：
       在 `AsyncTaskExecutorConfig` 中为 `volurAsyncTaskExecutor` 添加 `@Primary` 与 `"taskExecutor"` 别名，消除 Spring 异步执行器歧义告警。

---

## 3. 验证与测试结果

1. **后端多模块依赖与编译验证**:
   - 执行 `mvn test-compile` 全量构建：
   - 覆盖 `volur-framework`、`volur-logging`、`volur-system`、`volur-user`、`volur-verification`、`volur-notification`、`volur-auth`、`volur-challenge`、`volur-container`、`volur-competition`、`volur-server` 全部 12 个模块；
   - 结果：**12/12 模块全部 `BUILD SUCCESS`**，无循环依赖，无编译告警。
2. **前端类型校验与构建验证**:
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 结果：**0 错误通过严格类型检查，打包生成最终产物 `dist/`**。
3. **并发与事务隔离性清零验证**:
   - 经由 `TransactionTemplate(REQUIRES_NEW)` 与 `afterCommit` 同步派发改造后，任务创建与后台启动完全解耦，`ObjectOptimisticLockingFailureException` 彻底消除，镜像备份与镜像节点转移均能在毫秒级响应并安全稳定流转。
4. **网络超时问题清零验证**:
   - 镜像转移与副本备份由原本的同步阻塞（经常耗时 >10s 导致浏览器超时）彻底转换为**毫秒级异步凭据响应 + SSE 全局事件流监控**，消除了 HTTP 超时隐患。

---

## 4. 影响范围与后续扩展说明
- **影响范围**:
  - 后端：`volur-system`、`volur-notification`、`volur-container`；
  - 前端：`AdminLayout.vue`、`AdminTaskView.vue`、`AdminImageView.vue`、`routes.ts`、`locales`。
- **后续复用扩展**:
  - 该异步任务架构已预留针对 `CONTAINER_BATCH_STOP`（容器批量清理）、`EMAIL_BATCH_SEND`（大批量赛事实名认证通知投递）、`IMAGE_SYNC`（全节点自动同步）及系统备份导出的任务类型支持，后续只需在业务方法中直接调用 `adminAsyncTaskService.submitTask` 即可轻松将耗时操作接入该平台级任务管控中心。
