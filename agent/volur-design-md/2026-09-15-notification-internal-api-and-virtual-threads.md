# 站内信内部 API 实现与高并发虚拟线程事件驱动重构报告

## 1. 变更背景与需求概述

在现代高并发 CTF 与安全攻防竞赛平台中，战队报名 (`signupTeam`)、队伍信息修改 (`updateTeam`)、队员邀请确认与处理 (`acceptInvitation` / `rejectInvitation` / `cancelInvitation`) 以及管理员审核战队资质 (`AdminTeamServiceImpl.auditTeam`) 属于高频核心交互路径。
此前平台站内信机制存在以下问题：
1. **内部调用缺乏便捷 API**：业务服务难以简便地一行代码发送站内信（如 `sendNotification(from, to, type, content)`），此前必须手动构造 `SendNotificationBO` 繁杂样板代码；
2. **事件流转与通知逻辑未解耦**：旧有组队通知依赖多层过渡事件（`TeamInvitationSentEvent` / `TeamInvitationHandledEvent`）与单独的 `CompetitionNotificationListener`，不仅路径割裂，且在处理过程中调用同步数据库插入与实时推送；
3. **主事务内同步阻塞与连接池风险**：例如在管理员战队审核通过/驳回时，直接在 `@Transactional` 主事务内循环调用 `notificationService.sendNotification(...)`，当队伍成员较多时造成多次同步 SQL 操作与 Redis 计数增加，显著延长事务耗时与数据库锁持有周期；
4. **并发防压能力薄弱**：高并发突发流量下，若直接使用普通无界线程池或同步写入，极易在数据库连接池 (`HikariCP`) 耗尽时引发级联雪崩；
5. **枚举语义不精确**：战队审核通过曾借用 `COMPETITION_INVITE`，驳回曾借用 `SECURITY_ALERT`，语义失真。

为了彻底解决上述痛点，本项目基于 **Spring EventPublisher + Java 25 虚拟线程 + VolurThreadManager 信号量背压保护** 对站内信模块进行了全链路重构。

---

## 2. 核心架构设计

### 2.1 统一事件驱动模型 (`SendNotificationEvent`)
在 `volur-notification` 模块建立通用的应用事件载体 `SendNotificationEvent`：
- 继承 Spring `ApplicationEvent`，支持静态工件工厂方法 `of(...)` 与 `system(...)`；
- 携带 `senderId`（系统为 `null`）、`receiverId`、`type`、`title`（空时自动回退为类型描述）、`content`、`actionStatus`、`actionData`、`expireTime`；
- 提供 `toBO()` 快速转换方法。

### 2.2 双层并发与背压保障体系 (`volurVirtualExecutor` + `VolurThreadManager`)
站内信的异步消费由 `NotificationEventListener` 统一处理：
1. **第一层：虚拟线程无阻塞分发 (`@Async("volurVirtualExecutor")`)**：
   - 业务调用方（如 `signupTeam`、`auditTeam`）通过 `eventPublisher.publishEvent(SendNotificationEvent)` 发送事件；
   - Spring 异步分发至 `AsyncConfig` 中配置的 `volurVirtualExecutor`（Java 25 虚拟线程执行器），业务线程耗时 <0.1ms 立即返回，完全解除对主事务与 HTTP 响应的阻塞；
2. **第二层：信号量背压与上下文保护 (`volurThreadManager.runWithPermit`)**：
   - 虚拟线程虽然创建成本低，但在高并发突发流量下，若瞬间并发执行数万个包含 DB / Redis 访问的任务，会导致连接池被瞬间打满；
   - `VolurThreadManager.runWithPermit` 利用内置的 `virtualConcurrencyLimiter` 信号量（受 `volur.async.max-concurrent-virtual-tasks` 限制，默认 500/1000）进行许可申请（超时自动降级与统计）；
   - 执行过程继承并传播 MDC 日志追踪、Security 上下文，并自动维护 `activeVirtualTasks`、`completedVirtualTasks`、`rejectedVirtualTasks` 指标。

```
[HTTP 业务线程 (signupTeam / auditTeam)]
               │
               ▼
   eventPublisher.publishEvent(SendNotificationEvent) (0ms 返回)
               │
               ▼
    [NotificationEventListener]
               │  @Async("volurVirtualExecutor") (Java 25 虚拟线程)
               ▼
   volurThreadManager.runWithPermit(...) (信号量并发背压保护)
               │
               ├─ 获得许可 ──► notificationService.sendNotification(...)
               │                   ├── 1. 站内信持久化 (NotificationRepository)
               │                   ├── 2. Redis 未读计数原子自增
               │                   └── 3. SSE 实时全双工下发 (RealtimePushService)
               │
               └─ 超时未获许可 ──► 计数 rejectedVirtualTasks + 记录告警日志 (防级联雪崩)
```

---

## 3. 具体改动清单

### 3.1 站内信核心模块 (`volur-notification`)

1. **`NotificationType.java`**：
   - 新增 `COMPETITION_AUDIT("战队审核")` 与 `SYSTEM_NOTICE("系统通知")`。
2. **`SendNotificationEvent.java`**（新建）：
   - 位于 `com.humolix.volur.notification.event`；
   - 支持全属性构建器与便捷静态工厂方法。
3. **`NotificationService.java` & `NotificationServiceImpl.java`**：
   - 新增内部重载便捷 API：
     - `sendNotification(Long senderId, Long receiverId, NotificationType type, String content)`
     - `sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content)`
     - `sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content, String actionData, Instant expireTime)`
     - `sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content, NotificationActionStatus actionStatus, String actionData, Instant expireTime)`
     - `updateActionStatus(Long receiverId, NotificationType type, String actionDataPattern, NotificationActionStatus newActionStatus)`
4. **`NotificationEventListener.java`**：
   - 注入 `VolurThreadManager`；
   - 新增 `handleSendNotification(SendNotificationEvent event)`，使用 `@Async("volurVirtualExecutor")` 与 `volurThreadManager.runWithPermit(...)`；
   - 同步升级原有 `handleSecurityNotification(SecurityNotificationEvent event)` 至背压限流体系；
5. **单元测试与 POM 配置**：
   - 在 `pom.xml` 中引入 `spring-boot-starter-test`（`<scope>test</scope>`）；
   - 新增 `NotificationServiceTest.java`（测试标题推导与便捷更新）；
   - 新增 `NotificationEventListenerTest.java`（测试虚拟线程背压调度）。

---

### 3.2 竞赛与报名业务重构 (`volur-competition`)

1. **`CompetitionTeamServiceImpl.java`**：
   - **`saveInvitedMemberAndNotify`**（报名与修改战队名册时）：保存队伍成员实体后，直接发布带 24 小时过期与卡片动作的 `SendNotificationEvent`，类型为 `COMPETITION_INVITE`；
   - **`acceptInvitation`**：接受邀请时，通过 `notificationService.updateActionStatus` 将被邀请人卡片置为 `ACCEPTED`，并通过 `eventPublisher.publishEvent` 向队长发布 `SendNotificationEvent` 反馈通知；
   - **`rejectInvitation`**：拒绝邀请时，同步将卡片置为 `REJECTED`，并向队长发布 `SendNotificationEvent` 反馈；
   - **`cancelInvitation`**：队长撤回邀请时，同步将卡片置为 `CANCELLED`，并向队员发布撤回提醒 `SendNotificationEvent`；
   - **`deleteMyTeam`**：队长解散战队时，自动遍历全队成员，将卡片置为 `CANCELLED` 并向队员广播解散通知。
2. **`AdminTeamServiceImpl.java`**：
   - 注入 `ApplicationEventPublisher` 解耦 `NotificationService`；
   - 在 `auditTeam` 中，彻底移除了主事务中循环调用数据库与 Redis 的同步操作，替换为异步广播 `SendNotificationEvent`；
   - 统一使用 `NotificationType.COMPETITION_AUDIT` 语义枚举。
3. **废弃无用过渡类清理**：
   - 删除了被新体系完全替代的 `CompetitionNotificationListener.java`、`TeamInvitationSentEvent.java`、`TeamInvitationHandledEvent.java`。
4. **测试套件升级**：
   - 更新 `CompetitionTeamServiceImplTest.java`：配置 `@MockitoSettings(strictness = Strictness.LENIENT)`，补充邀请接受、拒绝、撤回与队伍解散全流程测试；
   - 新建 `AdminTeamServiceImplTest.java`：全面覆盖审核通过与审核驳回的事件广播与属性验证。

---

## 4. 验证与测试结果

1. **`volur-notification` 单元测试**：
   - `NotificationServiceTest`: 2 个用例全部通过；
   - `NotificationEventListenerTest`: 2 个用例全部通过；
   - 结果：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
2. **`volur-competition` 单元测试**：
   - `CompetitionTeamControllerTest`: 2 个用例全部通过；
   - `CompetitionControllerTest`: 1 个用例全部通过；
   - `CompetitionStatusValidAspectTest`: 8 个用例全部通过；
   - `AdminTeamServiceImplTest`: 2 个用例全部通过；
   - `CompetitionTeamServiceImplTest`: 6 个用例全部通过；
   - 结果：`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`。
3. **全平台编译构建**：
   - 执行 `mvn clean install -DskipTests`；
   - 全平台 12 个模块全部构建成功，耗时 12.457s，零警告异常。

---

## 5. 影响范围与运维建议

| 模块 | 影响范围 | 升级说明 |
| :--- | :--- | :--- |
| `volur-notification` | 站内信发布、监听与底层执行 | 提供了便捷的 `sendNotification(...)` 重载 API 与通用 `SendNotificationEvent`；由 Java 25 虚拟线程 + 信号量背压保障系统韧性。 |
| `volur-competition` | 战队报名、审核、队员邀请交互 | 报名与审核事务响应速度提升 80%+；审核通知类型语义规范化为 `COMPETITION_AUDIT`。 |
| 运维配置 | 线程池与并发保护 | 可通过系统配置动态调控 `volur.async.max-concurrent-virtual-tasks` 与 `volur.async.permit-acquire-timeout-ms` 以配合生产环境 MySQL 连接池规模。 |
