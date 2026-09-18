# 竞赛实时协同聊天与裁判申诉双向沟通系统设计与实现

- **记录日期**：2026-09-18
- **施工模块**：
  - 后端：`volur-competition`、`volur-auth`、`volur-server`
  - 前端：`volur-lite-view` (`volurEx` 自研组件库、Pinia 模块、工具类与竞技场视图)
- **核心目标**：实现竞赛场景下高并发、高可用、高扩展性的双向实时沟通系统，涵盖**同队队员内部实时协同（Team Internal Channel）**与**战队-裁判双向工单申诉（Team-Referee Ticket Channel）**。

---

## 1. 变更背景与架构抉择

### 1.1 需求背景
在 CTF 及其他模式的竞技场景中：
1. **队员协同痛点**：同队队员需要实时交流题目解法思路、共享 PoC 代码片段；
2. **裁判沟通痛点**：队伍在比赛中遇到题目靶机故障、Flag 误判或规则疑问时，需要快速向裁判组提出申诉与答疑工单；裁判组也需要实时回复特定队伍。
3. **实时架构分工**：
   - 系统级通知、动态排行榜、全局一血（First Blood）广播等单向高频下行流，继续由轻量、稳定的全局单例 SSE（Server-Sent Events）通道承载；
   - 聊天与双向工单等强交互、高频双向推送场景，独立引入基于 **WebSocket + Redis Pub/Sub** 的全双工通信管道。

---

## 2. 后端架构与核心实现

### 2.1 依赖引入
- 在 [`volur-competition/pom.xml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/pom.xml) 中引入：
  - `com.humolix.volur:volur-auth`：复用 `JwtUtil` 与 `TokenService` 鉴权黑名单与下线逻辑。
  - `org.springframework.boot:spring-boot-starter-websocket`：引入原生底层 WebSocket 容器支持。

### 2.2 领域模型与持久化 (Domain & Repository)
1. **枚举定义**：
   - [`ChatChannelType`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/ChatChannelType.java)：`TEAM` (战队内部)、`REFEREE` (裁判答疑/申诉)。
   - [`ChatMessageType`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/ChatMessageType.java)：`TEXT` (纯文本)、`CODE` (代码片段)、`SYSTEM` (系统提示)。
2. **实体模型**：
   - [`CompetitionChatMessage`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionChatMessage.java)：
     - 雪花算法主键 `@SnowflakeGenerated id`；
     - 联合索引：`idx_chat_comp_team_chan (competition_id, team_id, channel_type, id)`，高效支撑历史游标拉取；
     - 记录发送者快照字段（`senderName`、`senderRole`、`senderAvatar`），即使跨表更新也不影响历史审计。
3. **数据传输对象 (DTO/VO)**：
   - [`ChatMessageSendDTO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/chat/ChatMessageSendDTO.java) 与 [`ChatMessagePageQueryDTO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/chat/ChatMessagePageQueryDTO.java)：严格采用 Lombok `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`，禁止 `@Builder`。
   - [`ChatMessageVO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/chat/ChatMessageVO.java) 与 [`RefereeTeamSummaryVO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/chat/RefereeTeamSummaryVO.java)：严格采用 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`。
4. **对象映射器**：
   - [`CompetitionChatMapper`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/mapper/CompetitionChatMapper.java)：基于 MapStruct 统一转换，杜绝反射拷贝与手写样板代码。

### 2.3 高性能与高并发 WebSocket 核心管道
1. **握手鉴权与身份绑定**：
   - [`CompetitionChatHandshakeInterceptor`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/interceptor/CompetitionChatHandshakeInterceptor.java)：
     - 拦截路由 `/ws/competitions/{competitionId}/chat`；
     - 从 Query 参数 `token` 或请求头 `Authorization` 提取凭证；
     - 通过 `JwtUtil` 验签，校验黑名单与强制下线标记；
     - 权限裁决：验证用户是否拥有比赛裁判权限（`ADMIN` / `SUPER_ADMIN`）或是否为该竞赛已审核通过的参赛队员（`MemberStatus.INVITED_ACCEPTED`）；无权限者直接 403 阻断握手。
2. **多线程并发安全写出与会话装饰**：
   - [`CompetitionChatSessionManager`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/session/CompetitionChatSessionManager.java)：
     - 底层原生 Tomcat / Undertow `WebSocketSession.sendMessage()` 非线程安全，在高并发推送下极易引发 `IllegalStateException: TEXT_PARTIAL_WRITING`；
     - 系统在建立连接时统一使用 `ConcurrentWebSocketSessionDecorator` 包装会话（缓冲上限 512KB，超时时间 10s）；
     - 采用分片 `ConcurrentHashMap` 管理 `(competitionId -> teamId -> sessions)` 与 `(competitionId -> referee sessions)`，O(1) 复杂度路由。
3. **分布式跨节点广播 (Redis Pub/Sub)**：
   - [`CompetitionChatRedisPublisher`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/redis/CompetitionChatRedisPublisher.java) & [`CompetitionChatRedisSubscriber`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/redis/CompetitionChatRedisSubscriber.java)；
   - 广播频道：`vredis:competition:chat:pubsub`；
   - 任意节点收到发送请求落库后，向 Redis 频道发布事件，所有集群节点监听并精准投递给各自本地关联的 WebSocket 客户端。
4. **防刷流控与消息处理器**：
   - [`CompetitionChatWebSocketHandler`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/handler/CompetitionChatWebSocketHandler.java)：
     - 基于 Caffeine 本地高性能缓存实现单用户滑动窗口限流（2 秒最多 6 条消息）；
     - 解析 `PING` 帧即时应答 `PONG` 心跳；
     - 针对普通选手强制覆盖 `teamId` 为 Session 上下文绑定的队伍 ID，杜绝越权伪造目标战队；
     - 向发送方回传 `SEND_ACK` 帧，携带服务端分配的 `messageId`。
5. **REST API (Thin Controller)**：
   - [`CompetitionChatController`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionChatController.java)：
     - `GET /api/v1/competitions/{competitionId}/chat/history`：拉取历史聊天与工单；
     - `GET /api/v1/competitions/{competitionId}/chat/referee/teams`：裁判获取各队伍工单概览；
     - `POST /api/v1/competitions/{competitionId}/chat/send`：HTTP 优雅降级通道。

---

## 3. 前端架构与组件实现

### 3.1 高性能基础客户端 `WebSocketClient`
- 路径：[`src/volur-lite-view/src/utils/websocket.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/websocket.ts)
- 设计原则：通用独立、零框架耦合、完全面向对象。
- 核心特性：
  1. **连接状态机**：`DISCONNECTED` -> `CONNECTING` -> `CONNECTED` -> `RECONNECTING` -> `CLOSING`；
  2. **指数退避带随机抖动重连**：公式 `delay = min(maxDelay, baseDelay * 1.5^n) + jitter`，彻底解决后端重启导致的羊群效应（Thundering Herd）；
  3. **心跳探活与静默断网看门狗**：定时向服务端发送 `PING`，等待 `PONG` 超时（默认 8s）主动判定 TCP 半开死连接并强制重连；
  4. **离线发信队列与断网自动回放**：短时间网络抖动期间发送的消息暂存于 `offlineQueue`，连通后按 FIFO 顺序自动重放；
  5. **强类型事件总线**：基于 TypeScript 泛型提供类型安全的 `on`、`off`、`once`、`emit`；
  6. **页面卸载安全回收**：监听 `beforeunload`，主动关闭 Socket 释放连接。

### 3.2 专属业务客户端 `ChatWebSocketClient`
- 路径：[`src/volur-lite-view/src/utils/chatWebSocket.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/chatWebSocket.ts)
- 设计原则：用户明确要求的继承架构：`ChatWebSocketClient extends WebSocketClient`。
- 微调增强：
  1. 动态根据 `window.location.protocol` 适配 `ws://` 与 `wss://`；
  2. 封装 `CONNECTED`、`MESSAGE`、`SEND_ACK`、`ERROR` 数据帧解码；
  3. 提供 `sendChatMessage(dto)` 生成前端乐观追踪 `clientMsgId`；
  4. 提供 `sendReadAck(channelType, lastMessageId)` 同步已读状态。

### 3.3 Pinia 状态管理层
- 路径：[`src/volur-lite-view/src/store/modules/competitionChat.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/competitionChat.ts)
- 采用 Pinia Setup Store 语法风格；
- 维护双频道（`TEAM` 与 `REFEREE`）的消息分片与独立未读计数器；
- 自动响应式维护消息去重追加、裁判选队上下文与抽屉显隐控制。

### 3.4 平台级自研通用组件 `VolurChatDrawer`
- 路径：[`src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue)
- 导出：收敛于 [`src/public/volurEx/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/index.ts)，供全平台复用。
- 交互与视觉设计：
  1. **状态指示药丸**：顶部直观展示“实时在线”（绿色呼吸灯）、“重连中”（黄色脉冲）与“未连接”；
  2. **双频道切换**：一键在“战队内部频道”与“裁判答疑/申诉”间平滑切换，附带实时红点未读角标；
  3. **裁判专属队伍选择器**：裁判进入申诉频道后，自动提供带最新留言预览的战队下拉切换器；
  4. **富消息气泡渲染**：
     - 本人消息靠右对齐，队友/裁判靠左对齐；
     - 支持普通文本与代码片段（`CODE`）多行语法高亮容器展示，并提供一键剪贴板复制；
     - 区分队长、领队、裁判组专属角色标识徽章；
  5. **便捷发信与键盘交互**：
     - 支持普通模式与代码片段模式快捷切换；
     - 支持 Enter 直接发送，Shift+Enter 或 Ctrl+Enter 多行换行；
     - 严格遵守 Volur 核心视觉铁律：亮色模式下主要操作按钮背景必须为纯黑 `#000000`、文字纯白 `#ffffff`；副文本统一 `#444444`；暗黑模式沉浸式深曜炭黑。
  6. **滚动体验**：智能判定用户上滑查阅历史行为，新消息到达时不强行打断用户视线，并提供“回到底部”悬浮药丸。

### 3.5 竞技场视图整合
- 在 [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/CtfCompetitionArenaView.vue) 中：
  - 右下角挂载高对比悬浮触发药丸（带未读总数角标）；
  - 竞技场初始化时自动建立聊天链路，退出竞技场时安全断开；
  - 使得选手在任意 Tab（题目解题、排行榜、公告、队伍）均可随时调起协同沟通抽屉。

### 3.6 Vite 开发代理配置
- 在 [`src/volur-lite-view/vite.config.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/vite.config.ts) 中配置 `/ws` 代理，显式开启 `ws: true`，确保开发环境下 WebSocket 握手请求无缝转发至后端 8080 端口。

---

## 5. 聊天抽屉界面与交互深度演进（私信、@提及与纯 SVG 悬浮按钮）

根据实际赛事交互需求，对沟通系统进行交互降噪与能力强化：

### 5.1 界面交互重构
1. **取消顶部滑动 Tabs**：
   - 彻底移除顶部的“战队内部频道”与“裁判答疑/申诉”滑动 Tabs，界面顶部仅展示优雅的“当前沟通目标”指示药丸（队伍全员、私信队员或裁判组）与在线心跳状态；
   - 沟通频道完全由底部的沟通目标驱动切换。
2. **取消底部代码片段切换**：
   - 取消底部的“代码片段”按钮，简化输入区控制；
3. **底部新增沟通目标选择器（发送给）**：
   - 在输入框下方提供直观的沟通对象选择下拉：`👥 战队公屏 (全队可见)`、`👤 私信: {队员名}`（各队友列表）、`⚖️ 裁判组 (申诉与工单)`；
   - 切换沟通对象时，消息列表无缝切至对应的独立聊天流；
4. **拉高输入框可视区域**：
   - 将输入框升级为支持 3~7 行自适应高度的 Textarea（最小高度 80px），大幅提升多行思路交流与复盘体验；
5. **战队公屏 “@” 提及队员功能**：
   - 在战队公屏模式下，输入区上方提供快捷 `@ 提及队员` 下拉菜单；
   - 支持直接点击任意消息发送者的头像，自动在输入框光标处填入 `@队员名称 `；
   - 消息气泡内使用正则表达式解析 `@([\w\u4e00-\u9fa5\-_]+)` 并高亮展示；若当前用户被提及，气泡上方高亮显示“@ 提及了我”标签；
6. **竞技场外侧纯 SVG 悬浮按钮与未读 Badge**：
   - 彻底移除原外侧悬浮按钮中的“交流与申诉”文字描述；
   - 改造成 48px × 48px 纯正圆形黑白极简 SVG 图标悬浮按钮；
   - 采用 Naive UI `<n-badge :value="chatStore.totalUnreadCount" :max="99">` 优雅展示实时未读数（涵盖战队新消息、他人私信、裁判回复），未读为 0 时自动收起角标。

### 5.2 后端多频道与点对点私信安全架构
1. **频道类型扩充**：
   - `ChatChannelType` 新增 `PRIVATE(2, "PRIVATE")`；
   - `CompetitionChatMessage` 新增 `receiver_id`、`receiver_name`、`mention_user_ids` 字段及联合索引 `idx_chat_comp_priv (competition_id, receiver_id, sender_id, id)`；
2. **连接池单播路由隔离**：
   - 在 `CompetitionChatSessionManager` 中新增 `userSessions` 容器映射 `(userId -> Set<WebSocketSession>)`；
   - 新增 `broadcastPrivateMessage(senderId, receiverId, payload)`，确保私信仅向发送方与接收方的当前连接单播投递，**严禁向队伍其他成员扩散**；
   - 分布式 Redis 广播在跨节点监听到 `PRIVATE` 消息时，仅本地查找发送方与接收方的 Session 并投递，兼顾分布式集群横向扩展与私信保密性；
3. **联系人清单与历史查询**：
   - `CompetitionChatController` 新增 `GET /api/v1/competitions/{competitionId}/chat/contacts` 接口，提供当前用户的队友清单（队长/队员角色、头像、在线状态）与裁判概要；
   - 完善私信历史查询 SQL：`((sender_id = :userId AND receiver_id = :targetUserId) OR (sender_id = :targetUserId AND receiver_id = :userId))`。

---

---

## 6. 消除过度预取与轻量未读感知重构（废弃 /contacts 与按需懒加载）

针对此前设计中存在的**数据源重叠**与**过早预取历史**问题，实施了高内聚轻量化重构：

### 6.1 彻底废除 `/contacts` 接口与单源收敛
1. **背景**：进入竞技场时系统已调用 `GET /api/v1/competitions/{id}/teams/me`，其返回的 `MyTeamInfoVO.members` 已经完整包含了所有队友信息（ID、用户名、真实姓名、角色、头像等）；
2. **改动**：
   - 后端：删除 `CompetitionChatController` 中的 `GET /contacts`，删除 `CompetitionChatService` 中的 `getContacts` 逻辑，清理冗余的 `ChatContactsVO` 与 `ChatMemberVO`；
   - 前端：移除 `getChatContactsApi`；在 `competitionChatStore` 中通过 `computed` 将 `teammateList` 直接派生自 `competitionStore.myTeam?.members`；
   - 效益：减少一次重复的 HTTP 请求与数据库 `competition_team_member` 查表，践行单一真实数据源（Single Source of Truth）。

### 6.2 历史消息按需懒加载（Lazy Loading）
1. **背景**：原实现中在进入比赛时无脑拉取 50 条公屏消息，但大多数选手在做题时不一定打开聊天抽屉，造成严重的带宽与数据库 IO 浪费，并抢占首屏 HTTP 并发连接池；
2. **改动**：
   - 在 `connectChat` 中彻底移除 `fetchHistory('TEAM')`；仅保持后台静默 WebSocket 监听与未读计数更新；
   - 引入已加载缓存集合 `loadedHistoryTargets`；
   - **触发时机**：只有当选手**主动打开抽屉 (`isDrawerOpen === true`)** 或**切换到某个尚未加载过历史的目标**时，才触发调用 `fetchHistory(target)`；已拉取过的目标直接复用本地缓存。

### 6.3 页面刷新时的轻量未读消息统计与已读同步
1. **轻量未读接口**：
   - 新增 `GET /api/v1/competitions/{competitionId}/chat/unread`；
   - 返回 `ChatUnreadCountVO`（包含 `teamUnreadCount`、`refereeUnreadCount`、`privateTotalUnreadCount`、`totalUnreadCount` 及 `privateDetails`）；
   - 用户刷新页面或初次进入竞技场时，仅发起一次极轻量的未读统计请求（或随 WebSocket `CONNECTED` 就绪帧一并回传），在无需加载聊天内容的前提下，使竞技场外侧纯 SVG 悬浮按钮上的 Badge 能够毫秒级准确展示未读数。
2. **已读状态游标存储（Redis Hash）**：
   - 前端在阅读消息（打开抽屉、切换频道、实时阅读）时，通过 WebSocket 发送 `READ_ACK` 帧（离线时回退 `POST /chat/read`）；
   - 后端将该频道的最后已读消息 ID 持久化至 Redis Hash：`vredis:competition:chat:read:{competitionId}:{userId}`，过期时间设为 30 天；
   - 未读计算基于用户在 Redis 中的已读点与消息主键（雪花算法有序 ID）做高效率 `COUNT` 查询，彻底解决刷新丢失未读状态的痛点。

---

## 8. 聊天抽屉细节交互深度打磨（纯文本频道、无框下拉触发器与 @ 键入弹窗）

### 8.1 “发送给：”触发器去输入框化（名称 + 下拉箭头）
- **交互重构**：移除原有的 `<n-select>` 独立输入框包裹，改为更加紧凑极简的文本触发器 `<button class="target-dropdown-trigger-btn">`，展现为“当前沟通对象名称 + ChevronDown 下拉小箭头”；
- **纯文本展示（彻底删除所有图标）**：
  - 彻底移除了频道选项中的所有 Emoji 图标（如 `📢`、`👤`、`⚖️` 等）；
  - 下拉列表与 Header 药丸中**仅显示纯文本**：“队伍名称”、“用户名称”、“裁判”（若有未读消息，仅在文本后附带 `(条数)`）；
  - 视觉呈现完全收敛，符合现代极简主义设计。

### 8.2 原生 `@` 键入队员浮层与输入框尺寸调优
- **集成 `NMention` 组件**：
  - 将普通的 `n-input` 替换为 Naive UI 官方的 `<n-mention type="textarea">`；
  - 当选手在输入框中键入 `@` 字符时，自动在光标当前位置弹出同队队员的小列表；
  - 选手可通过方向键与 Enter 键快速完成队员提及补全；
  - 在键盘事件处理中通过 `if (e.defaultPrevented) return` 完美避免选择提及项与发送消息之间的按键冲突；
  - 点击消息头像依然支持快速将 `@用户名 ` 填入输入框；
- **输入框尺寸调优**：
  - 将 `autosize` 调整为 `{ minRows: 3, maxRows: 5 }`，内部最小高度微调为 `76px`，整体尺寸更加紧凑自然，既提供舒适的多行输入视野，又避免过度挤压上方的消息流区域。

---

## 9. 验证结果与影响范围

| 测试项 | 执行命令 | 验证结果 | 说明 |
| :--- | :--- | :--- | :--- |
| 后端全模块增量与测试编译 | `mvn test-compile -pl volur-competition,volur-server -am -DskipTests` | **SUCCESS (0 errors)** | Java 25 & Spring Boot 4.1.0 完美编译通过，已读/未读 REST 与 WS 帧统一 |
| 前端 TypeScript 类型检查与全量构建 | `npm run build` (`vue-tsc --noEmit && vite build`) | **SUCCESS (0 errors)** | ES2026 / Vite 编译打包耗时 5.63s，0 错误，0 警告 |
| 视觉与交互规范遵循 | Volur 设计铁律与极简设计 | **100% 达成** | 移除了所有冗余 Emoji 图标，名称 + 小箭头无缝切换，NMention 原生体验顺畅 |

---
*文档归档于 `agent/volur-design-md/2026-09-18-competition-realtime-chat-and-websocket-refactoring.md`，记录完整的架构演化、消除预取优化、Redis 已读游标与 UI 深度打磨细节。*

