# Volur Lite 全局通用实时 SSE 与系统公告/广告广播系统设计与施工归档

- **执行日期**：2026-08-25
- **施工模块**：`volur-notification` (核心增强), `volur-auth` (安全路由放行)
- **文档类型**：实时通信与全站事件广播架构施工档案

---

## 1. 变更背景与架构目标

在 Volur Lite 平台中，既有的站内信（`volur-notification`）采用传统的 RESTful 被动拉取模式（`/api/v1/notifications/*`），无法满足高时效性业务场景（如竞赛组队邀请、安全风险提示、系统紧急维护公告、全站运营活动/广告弹窗、赛事大屏一血广播等）的实时推送需求。

### 核心目标：
1. **全局统一长连接**：全站仅需维护单条 SSE (Server-Sent Events) 长连接，替代高开销、高延迟的 HTTP 轮询；
2. **多模式推送分发能力**：
   - **单播（Unicast）**：定向推送到指定在线用户（个人站内信、组队邀请卡片、密码修改告警）；
   - **组播（Multicast）**：定向推送到订阅特定频道/主题的客户端（如竞赛大屏 `competition:123`）；
   - **全站广播（Broadcast）**：推送到全站所有在线客户端（全站系统公告、紧急停机维护通知、运营活动与广告弹窗，**支持未登录游客与已登录用户**）；
3. **分布式多节点支持**：借助 Redis Pub/Sub 实现跨集群实例消息同步下发，杜绝节点间连接割裂；
4. **轻量心跳保活与异常自愈**：定时下发 `heartbeat` 保活帧，防止反向代理（Nginx）与浏览器自动断开，支持异常连接安全摘除。

---

## 2. 详细改动与新增清单

### 2.1 领域模型与枚举扩展 (`domain`)
- **`domain.enums.RealtimeEventType`**：定义统一事件类型：
  - `NOTIFICATION` (`"notification"`)：个人站内信；
  - `ANNOUNCEMENT` (`"announcement"`)：全站系统公告（居中弹窗/吸顶横幅）；
  - `PROMOTION` (`"promotion"`)：运营活动与广告弹窗；
  - `COMPETITION_EVENT` (`"competition_event"`)：竞赛动态事件（如解题/一血）；
  - `HEARTBEAT` (`"heartbeat"`)：保活心跳。
- **`domain.enums.BroadcastDisplayType`**：定义前端展示形式：
  - `MODAL`：阻断式居中弹窗；
  - `BANNER`：顶部吸顶横幅；
  - `TOAST`：轻量浮层气泡；
  - `DRAWER`：侧边栏抽屉。
- **`domain.dto.BroadcastMessageDTO`**：广播发布请求传输对象（含 `title`, `content`, `displayType`, `coverImageUrl`, `linkUrl`, `forcePopup`, `priority`, `expireTime`）；
- **`domain.vo.BroadcastMessageVO`**：广播推送视图对象（基于雪花算法生成唯一 `id`，便于前端基于 LocalStorage 进行已读防重复弹窗处理）；
- **`domain.bo.RealtimeMessageBO`**：Redis Pub/Sub 跨节点消息流转对象。

---

### 2.2 核心 SSE 引擎与连接管理器 (`sse`)
- **`sse.GlobalSseManager`**：
  - `userEmitterMap`：`ConcurrentHashMap<Long, List<SseEmitter>>` 维护已登录用户的多标签页/多终端连接；
  - `allActiveEmitters`：`CopyOnWriteArrayList<SseEmitter>` 维护全站全量活跃连接（含游客与已登录用户）；
  - `topicEmitterMap`：`ConcurrentHashMap<String, List<SseEmitter>>` 维护频道/主题订阅连接；
  - 提供 `connect(userId, topics)`、`sendToUser(userId, event, data)`、`broadcastAll(event, data)`、`broadcastToTopic(topic, event, data)` 等方法；
  - 定时调度 `@Scheduled(fixedRate = 25000)` 下发 `heartbeat` 保活。

---

### 2.3 分布式 Redis Pub/Sub 架构 (`redis`)
- **`redis.RealtimeRedisConfig`**：
  - 常量：`VREDIS_REALTIME_PUBSUB_CHANNEL = "vredis:realtime:pubsub:channel"`；
  - 配置 `RedisMessageListenerContainer` 监听全局实时广播频道；
- **`redis.RealtimeRedisSubscriber`**：
  - 监听 Redis 频道，收到消息后反序列化为 `RealtimeMessageBO` 并根据目标路由精确分发给当前节点的本地 `GlobalSseManager`。

---

### 2.4 服务层门面与业务联动 (`service`)
- **`service.RealtimePushService` & `service.impl.RealtimePushServiceImpl`**：
  - 提供 `pushNotification`、`publishAnnouncement`、`publishPromotion`、`broadcastToTopic`、`broadcastAll`；
  - 自动通过 `SnowflakeIdGenerator` 生成消息 ID 并发布至 Redis Channel；
- **业务无缝联动**：
  - 在 `NotificationServiceImpl.sendNotification()` 中注入 `RealtimePushService`，站内信持久化入库与 Redis 计数递增后，立即调用 `pushNotification` 进行毫秒级单播触达。

---

### 2.5 控制器层与安全放行 (`controller.v1` & `volur-auth`)
- **`controller.v1.RealtimeStreamController`**：
  - 暴露 `GET /api/v1/realtime/stream`（`text/event-stream`），支持 `@CurrentUserId(required = false)`，同时接纳游客与登录用户；
  - 暴露 `GET /api/v1/realtime/stats` 获取在线连接与在线用户统计；
- **`controller.v1.admin.AdminBroadcastController`**：
  - 暴露 `POST /api/v1/admin/broadcasts/announcement` 发布系统公告；
  - 暴露 `POST /api/v1/admin/broadcasts/promotion` 发布活动广告；
  - 受到 `@PreAuthorize("hasRole('ADMIN')")` 严格保护；
- **`volur-auth` `SecurityConfig` 路由更新**：
  - 在 `authorizeHttpRequests` 中将 `"/api/v1/realtime/stream"` 设为 `permitAll()`，允许公共连接建立。

---

## 3. 规范遵循与代码审计核验

- [x] **依赖注入规范**：所有类统一采用 `private final` 字段结合 Lombok `@RequiredArgsConstructor`，无 `@Autowired`，无手写构造函数；
- [x] **领域模型分层**：严格归置于 `domain.dto`, `domain.vo`, `domain.bo`, `domain.enums`；
- [x] **模型注解约束**：DTO 统一无 `@Builder`；VO 统一包含 `@Builder.Default` 保护初始值；
- [x] **控制层版本规范**：控制器均置于 `controller/v1` 及 `controller/v1/admin` 下，路由显式声明版本；
- [x] **Redis 规范**：常量以 `VREDIS_` 开头，Key/Channel 小写冒号分隔规范命名；
- [x] **构建验证**：Maven 整体 `mvn clean compile -DskipTests` 全平台 12 个模块全部构建成功（SUCCESS）。
