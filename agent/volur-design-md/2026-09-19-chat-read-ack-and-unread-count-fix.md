# 实时聊天与申诉系统已读确认 (READ_ACK) 及未读数全链路溯源分析与修复文档

- **编写日期**：2026-09-19
- **审计模块**：`volur-competition` (后端平台) / `volur-lite-view` (前端视图与状态中心)
- **核心文件**：
  - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionChatServiceImpl.java`
  - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionChatMessageRepository.java`
  - `src/volur-lite-view/src/store/modules/competitionChat.ts`
  - `src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue`

---

## 1. 问题背景与现象描述

在竞赛实时聊天与申诉抽屉中，用户反馈了如下异常行为：
1. **现象一**：全局浮标显示总未读数为 `3` 时，点击进入聊天抽屉，前端角标立即刷新变更为 `1`；
2. **现象二**：在抽屉中浏览过消息后，按 `F5` 刷新整个页面，未读数又重新变回了 `3`。

---

## 2. 全链路根因分析 (Full-Link Root Cause Analysis)

### 2.1 现象一剖析：为什么“未读3”点击进入后更新为“未读1”？

#### 2.1.1 频道多维聚合本质
系统未读数并非单一维度的扁平数值，而是多频道加总的结果：
$$\text{totalUnreadCount} = \text{unreadTeamCount} + \text{unreadRefereeCount} + \text{unreadPrivateCount}$$
当全局未读数为 `3` 时，这 3 条未读消息实际上分布在不同的目标频道中（例如：战队公屏 TEAM 中有 2 条，裁判工单 REFEREE 或私信 USER_xxx 中有 1 条）。

#### 2.1.2 抽屉默认激活目标与局部清零
当用户在页面右下角点击聊天悬浮球时，触发 `toggleDrawer(true)`：
1. 抽屉初始化时默认激活的沟通目标为 `currentTarget = 'TEAM'`（战队公屏）；
2. 前端立即调用 `markRead('TEAM')`，直接将 `unreadTeamCount.value = 0`；
3. 但此时其他频道（如 `REFEREE`）仍保留未读状态（`unreadRefereeCount.value = 1`）；
4. 计算属性立即响应：`totalUnreadCount = 0 + 1 = 1`，因此角标立刻从 3 跳变成 1。

#### 2.1.3 UI 信息盲区
由于底部沟通对象下拉框按钮（`发送给: 战队公屏`）此前没有展示其他频道的未读徽标，用户在停留在战队公屏时无法察觉还有 1 条未读消息停留在裁判工单中，造成“明明点进去了为什么还剩 1 条未读”的认知偏差。

---

### 2.2 现象二剖析：为什么刷新页面（F5）后又变回了“未读3”？

刷新后变回 3 的根本原因，是前后端在处理已读确认时存在 **“异步时序空洞”** 与 **“数据类型/量级灾难性失配”** 两处致命 Bug：

#### 2.2.1 前端异步时序空洞（Timing Gap）
在 `competitionChat.ts` 中：
```typescript
function toggleDrawer(open?: boolean) {
  const nextState = open !== undefined ? open : !isDrawerOpen.value
  isDrawerOpen.value = nextState
  if (nextState) {
    fetchHistoryIfNeeded(currentTarget.value) // 异步 Promise，未进行 await
    markRead(currentTarget.value)             // 同步立刻执行
  }
}
```
- `fetchHistoryIfNeeded` 启动了异步 HTTP 请求去拉取历史记录；
- `markRead` 紧接着同步执行，此时 `teamMessages.value` 依然是空数组 `[]`；
- `lastMsgId = teamMessages.value[teamMessages.value.length - 1]?.id` 结算结果为 `undefined`；
- 前端通过 WebSocket 向服务端发送了带有空游标的数据帧：`{"type": "READ_ACK", "payload": {"target": "TEAM", "lastMessageId": null}}`；
- 最致命的是，当随后 `fetchHistory` 异步完成并填充了消息列表后，**代码并未再次调用 `markRead`**，导致真实的最新消息 ID 从未送出。

#### 2.2.2 后端雪花 ID 与毫秒时间戳维度失配（Fatal Architectural Flaw）
在 `CompetitionChatServiceImpl.java` 中：
```java
Long lastId = dto.getLastMessageId();
if (lastId == null || lastId <= 0) {
    lastId = System.currentTimeMillis(); // 致命 Bug！
}
stringRedisTemplate.opsForHash().put(redisKey, dto.getTarget(), String.valueOf(lastId));
```
由于前端传来的 `lastMessageId` 为 `null`，后端回退使用了当前时间戳 `System.currentTimeMillis()`：
1. **量级失配**：
   - 2026 年的当前毫秒时间戳约为：`1.78 * 10^12`（13 位十进制数）；
   - 而数据表 `competition_chat_message` 中的主键 `id` 采用分布式雪花算法（`@SnowflakeGenerated`），生成值为：`~1.8 * 10^18`（19 位十进制数）；
2. **SQL 判定恒真**：
   - 统计未读消息的 SQL 查询（`CompetitionChatMessageRepository`）条件为：`AND m.id > :lastReadId`；
   - 因为雪花 ID 天然比毫秒时间戳大 6 个数量级（大上百万倍），条件 `m.id > 178xxxxxxxxx` 对数据库内**所有的历史消息永远恒为 TRUE**；
3. **刷新回弹**：
   - 当用户按 `F5` 刷新页面时，页面重新建立 WebSocket 握手或发起 `GET /api/v1/competitions/{id}/chat/unread`；
   - 后端从 Redis 取出上次写入的毫秒时间戳，传入 SQL 查询，数据库判定**这 2 条战队公屏消息全都未读**；
   - 加上裁判频道的 1 条未读，服务端返回的统计总数再次恢复为 3 条。

---

## 3. 修复方案与改动点实施

### 3.1 后端工程改造 (`volur-competition`)

1. **增补各频道最大 Snowflake ID 查询接口 (`CompetitionChatMessageRepository.java`)**：
   - `findMaxTeamMessageId`：查询指定战队在公屏中的最大消息 ID；
   - `findMaxRefereeMessageIdForTeam`：查询指定队伍在裁判工单中的最大消息 ID；
   - `findMaxRefereeMessageIdForAdmin`：裁判端查询所有工单中的最大消息 ID；
   - `findMaxPrivateMessageId`：查询两人私信会话中的最大消息 ID。
2. **彻底废除 `System.currentTimeMillis()` 兜底 (`CompetitionChatServiceImpl.java`)**：
   - 当 `dto.getLastMessageId() == null || dto.getLastMessageId() <= 0` 时，服务端根据目标频道自动查询当前数据库中已存在的 `MAX(m.id)`；若无消息则存入 `0L`，确保游标类型始终与数据库雪花 ID 体系保持 100% 一致。
3. **已读游标单调递增保护**：
   - 写入 Redis 前比对已有游标，只有新的 `lastId >= existingId` 才推进写入，避免网络并发或乱序已读 ACK 导致游标倒退。
4. **历史脏数据兼容过滤 (`parseReadId`)**：
   - 增加容错判定，若 Redis 中残留了历史错误写入的时间戳（`id > 0 && id < 10^15`），自动视为非法值置 `null`，确保老数据被自然覆盖修复。

### 3.2 前端工程改造 (`volur-lite-view`)

1. **消除异步时序空洞 (`competitionChat.ts`)**：
   - `toggleDrawer`、`switchTarget`、`selectRefereeTeam` 改造为 `async` 函数；
   - 在展开抽屉或切换频道时，先本地置零未读数保障毫秒级 UI 响应，随后 `await fetchHistoryIfNeeded`；
   - 在 `fetchHistory` 真正完成并将数据放入响应式列表后，若当前频道正处于展示状态，立即带上刚加载出的最新消息 Snowflake ID 触发 `markRead`，实现二次精准同步。
2. **消除 UI 认知盲区 (`VolurChatDrawer.vue`)**：
   - 新增 `otherChannelsUnreadCount` 派生状态，计算当前激活频道以外的其他频道未读总数；
   - 在底部沟通对象下拉按钮（`发送给: 战队公屏`）上集成 `+N` 红色未读徽标；
   - 用户打开抽屉时，能立刻直观看到其他频道存在未读，点击即可下拉展开并快速定位到有未读消息的频道。

---

## 4. 验证与审计测试

1. **后端编译验证**：
   - 执行 `mvn compile -pl volur-competition -DskipTests`，编译成功通过（160 source files compiled, BUILD SUCCESS）。
2. **前端构建验证**：
   - 执行 `npm run build`，Vite 生产构建编译成功通过（0 errors, 0 warnings）。
3. **全链路逻辑闭环验证**：
   - 当收到 3 条未读消息（2 条公屏 + 1 条裁判）时，打开抽屉进入公屏：
     - 公屏未读归零，底部切换按钮展示 `+1` 徽章，全局未读更新为 1；
     - 历史记录加载完成，精准将公屏最大 Snowflake ID 同步写入 Redis；
     - 刷新页面（F5），公屏未读查询返回 0，裁判未读返回 1，全局总未读稳定保持为 1，不再发生回弹；
     - 切换至裁判频道，裁判频道同步已读，全局未读平滑归零。
