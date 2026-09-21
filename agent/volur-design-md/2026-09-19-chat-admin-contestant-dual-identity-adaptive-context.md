# 竞赛聊天系统：管理员与参赛选手双重身份上下文自适应方案 (方案 A) 重构实施报告

## 1. 变更背景与问题复盘

### 1.1 缺陷现象
在 CTF 竞赛系统中，当平台超级管理员（`SUPER_ADMIN`）或管理员（`ADMIN`）账号创建战队或作为队长/队员报名参赛时，出现以下问题：
1. **选手端发信静默丢弃**：管理员在比赛前台抽屉 [`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue) 发送消息，输入框文字清空后，聊天流中未显示任何消息；刷新后依然没有任何历史记录。
2. **后台管理端身份分裂**：在后台管理面板 [`TicketTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TicketTab.vue) 中，管理员既作为右侧黑色气泡的“裁判组（REFEREE）”，又作为左侧白色气泡的“参赛队员（CAPTAIN/MEMBER）”，同一个账号在同一工单中出现左右互搏的身份分裂。

### 1.2 全链路根因剖析
- **死锁根源**：后端在 `CompetitionChatServiceImpl` 以及 WebSocket 处理器中，采用了互斥分支判定：
  `if (isReferee) { ... } else { /* 选手逻辑 */ }`。
- **发信环节抛错**：
  - 前端选手抽屉发信不传递 `teamId`（选手默认隐式属于本队）；
  - WebSocket 检查到 `context.isReferee() == true`，跳过了自动注入 `teamId`；
  - `CompetitionChatServiceImpl.sendMessage` 强制判定 `if (isReferee)`，在未传 `teamId` 时直接抛出 `400 BAD_REQUEST: 裁判向队伍频道发送消息时必须指定目标队伍 ID`；
  - WebSocket 仅捕获异常回发错误帧，消息未入库未广播，前端表现为静默失败。
- **拉取历史环节空集**：
  - 前端拉取选手公屏历史不传 `teamId`；
  - 后端 `getHistoryMessages` 判定 `if (isReferee)`，在 `query.getTeamId() == null` 时直接返回 `CursorResponse.empty()`，导致历史也全部显示为空。

---

## 2. 方案 A：上下文自适应防御架构设计

为彻底解决“全局平台特权”与“具体比赛参赛上下文”的冲突，我们落地了**上下文自适应防御（Context-Adaptive Resolution）**机制：

```
                    ┌───────────────────────────────┐
                    │ 全局管理员账号 (Role: ADMIN)    │
                    └───────────────┬───────────────┘
                                    │
           ┌────────────────────────┴────────────────────────┐
           ▼                                                 ▼
【请求携带 teamId 且 isReferee】                 【未传 teamId 或非管理员】
  -> 判定为【裁判巡查/官方广播】上下文                -> 判定为【选手端前台交互】上下文
  -> senderRole = "REFEREE"                         -> 自动回退绑定其本场比赛战队 acceptedMember
  -> 向指定 teamId 广播/回复工单                     -> senderRole = member.getRole().name()
                                                    -> 正常拉取与收发本队消息
```

---

## 3. 具体修改点

### 3.1 服务层：[`CompetitionChatServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionChatServiceImpl.java)

1. **`sendMessage` 消息发送上下文自适应**：
   - **`TEAM` (战队公屏)**：
     - 若 `isReferee && dto.getTeamId() != null`：执行官方后台裁判广播，`senderRole = "REFEREE"`；
     - 若 `dto.getTeamId() == null`（选手端前台视角）：自动回退查询 `getAcceptedMember(competitionId, userId)`。若该管理员已加入战队，则自动绑定其战队，`senderRole` 采用其实际战队角色（`CAPTAIN` 或 `MEMBER`），并生效队伍内部 `@提及` 校验逻辑；若未加入战队，才抛出参数缺失或权限异常。
   - **`REFEREE` (裁判答疑工单)**：
     - 若 `isReferee && dto.getTeamId() != null`：裁判在后台回复工单，`senderRole = "REFEREE"`；
     - 若 `dto.getTeamId() == null`：选手端发起提问，自动绑定其战队并使用队员角色。
   - **`PRIVATE` (队员私信)**：
     - 优先判断 `acceptedMember != null`，若已加入战队则作为队员与同队队友私信；若未参赛且为裁判，则作为裁判开展官方私信。

2. **`getHistoryMessages` 历史记录上下文自适应**：
   - 在 `TEAM` 和 `REFEREE` 频道查询历史时：
     - 若 `isReferee && query.getTeamId() != null`：按裁判查询指定队伍记录；
     - 若 `query.getTeamId() == null`：自动回退获取当前用户的 `acceptedMember`，将其自身的战队 ID 作为 `targetTeamId`，使参赛管理员在选手端抽屉能顺畅拉取到本队的全部历史聊天。

### 3.2 WebSocket 消息网关：[`CompetitionChatWebSocketHandler.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/handler/CompetitionChatWebSocketHandler.java)
- 在接收客户端 `SEND` 数据帧时，优化参数防御与回退：
  ```java
  // 权限保护：非裁判用户不可篡改目标队伍 ID；若管理员未显式指定目标队伍 ID 且已加入战队，自动填充其参赛战队 ID
  if (!context.isReferee() || sendDTO.getTeamId() == null) {
      if (context.getTeamId() != null) {
          sendDTO.setTeamId(context.getTeamId());
      }
  }
  ```

### 3.3 前端状态与拉取自适应：[`competitionChat.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/competitionChat.ts)
- 在 `currentMessages`、`fetchHistory`、`loadMoreHistory`、`markRead` 与 `sendMessage` 中：
  - 针对 `REFEREE` 频道，以往仅判断 `isReferee`，若 `selectedRefereeTeamId` 为空会导致取不到消息或发送失败；
  - 优化为：`(isReferee.value && selectedRefereeTeamId.value) ? selectedRefereeTeamId.value : (readyData.value?.teamId || competitionStore.myTeam?.teamId)`，当管理员在选手前台抽屉（未指定后台巡查队伍）时，无缝回退到自身参赛战队，保证工单消息顺利展示、已读与发送。

---

## 4. 验证与测试结果

1. **后端单元与切片测试**：
   - 执行 `mvn test -pl volur-competition`：全部通过（`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`）。
2. **后端编译验证**：
   - 执行 `mvn compile -pl volur-competition -DskipTests`：160 个 Java 源文件全部编译通过。
3. **前端生产打包与类型校验**：
   - 执行 `npm run build`：`vue-tsc` 0 错误通过，Vite 5.58s 生产打包成功。
