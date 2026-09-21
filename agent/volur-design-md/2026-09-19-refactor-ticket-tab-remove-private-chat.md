# 裁判端工单管理 (TicketTab) 移除点对点队员私信功能与逻辑重构

## 1. 变更背景与原因

在 Volur CTF 竞赛管理体系中，裁判（Referee / Admin）与参赛队伍的交互职责主要包含两大维度：
1. **裁判答疑与申诉工单 (`ChatChannelType.REFEREE`)**：队伍队员向官方裁判组发起技术疑问或判罚申诉，裁判针对工单进行回复。此工单通道属于“战队与裁判组”之间的双向官方通联渠道，该队伍内部所有成员均可阅读裁判的回复，且任何成员均可发言追问，消息卡片上均能清晰标识发送者的身份与角色。
2. **队伍公屏官方广播 (`ChatChannelType.TEAM`)**：裁判可直接向目标队伍内部公屏广播发布官方警告或提醒通报，全队队员可见。

此前在 [`TicketTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TicketTab.vue) 中，除了上述两个核心频道外，还额外挂载了 `PRIVATE`（队员点对点私信）频道，并配备了队员下拉选择器（`NSelect`）、成员变更监听以及私信历史/发送逻辑。

**存在的问题**：
- **概念混淆与冗余**：裁判与队员的官方答疑和沟通本就完全收敛在 `REFEREE` 工单中；在裁判管理端再单独引入 1v1 私信通道，导致功能认知重叠（裁判产生“到底是在工单里回还是私聊回”的疑惑）；
- **UI 冗余复杂**：为了支持私信，引入了额外的顶栏选择条、下拉筛选、`selectedMemberId` 状态同步、私信空状态分支判断等；
- **架构收敛**：因此，决定从 `TicketTab.vue` 中彻底移除 `PRIVATE` 点对点私信频道，使裁判端全面聚焦于 **`REFEREE`（裁判答疑工单）** 与 **`TEAM`（队伍公屏广播）**，同时将相关 API 调用、WebSocket 实时推送以及类型定义进行严格对齐与清理。

---

## 2. 变更前原逻辑深度分析

### 2.1 原视图与状态流转
- 原 `currentChannel` 支持 `'REFEREE' | 'TEAM' | 'PRIVATE'`。
- 当裁判切换到 `PRIVATE` 频道时：
  1. 渲染 `<div v-if="currentChannel === 'PRIVATE'" class="private-member-select-bar">`，展示成员下拉框 `n-select`。
  2. 下拉框绑定 `selectedMemberId`，选项为 `memberSelectOptions`（来自 `teamMembers`）。
  3. 切换队员时触发 `handleMemberChange`，重新调用 `fetchHistory`。
- 在 `canSend` 校验中，判断如果 `currentChannel === 'PRIVATE'` 且未选定队员，则禁用发送按钮。
- 在 `fetchHistory` 中，携带 `targetUserId: currentChannel === 'PRIVATE' ? selectedMemberId : undefined`。
- 在 `handleSend` 中，调用 `sendChatMessageApi` 并携带 `receiverId: selectedMemberId`。
- 在 WebSocket 的 `handleIncomingSocketMessage` 中，对 `PRIVATE` 频道进行 `senderId` 与 `receiverId` 的双向过滤。

---

## 3. 具体改动点

### 3.1 前端视图重构 (`TicketTab.vue`)
1. **频道切换 Pill 按钮**：从 `active-channel-pills` 中彻底移除 `PRIVATE` 按钮（保留 `REFEREE` 裁判答疑工单与 `TEAM` 队伍公屏广播）。
2. **私信选人条**：彻底移除 `.private-member-select-bar` 及其对应的 `n-select` 组件，并清理 SCSS 中对应的样式规则。
3. **状态与变量精简**：
   - 彻底删除 `selectedMemberId`、`memberSelectOptions`、`handleMemberChange`。
   - 保留 `teamMembers` 及 `getAdminTeamDetailApi`，仅用于在顶栏展示队伍总成员数（`成员: X 人`）。
   - 将 `currentChannel` 的类型收敛为 `'REFEREE' | 'TEAM'`。
4. **计算属性与文本分支精简**：
   - `canSend`：移除 `PRIVATE` 和 `selectedMemberId` 校验，仅需文本非空、非发送中、选定队伍即可发送。
   - `currentTargetNoticeText`、`inputPlaceholderText`、`channelEmptyDesc`：全面移除 `PRIVATE` 分支，仅保留清晰的工单与公屏广播提示。
5. **API 与网络调用精简**：
   - `fetchHistory`：移除冗余的 `targetUserId` 传参，纯净传递 `{ channelType: currentChannel.value, teamId: selectedTeam.value.id, size: 100 }`。
   - `handleSend`：移除冗余的 `receiverId` 传参，纯净传递 `{ channelType: currentChannel.value, teamId: selectedTeam.value.id, content: text, msgType: 'TEXT' }`。
6. **WebSocket 实时消息处理精简**：
   - `handleIncomingSocketMessage`：移除针对 `PRIVATE` 的多重判定分支，属于当前激活队伍与频道的推送消息直接追加展示。
7. **组件导入清理**：
   - 移除 `naive-ui` 中未使用的 `NSelect`、`SelectOption`。
   - 移除 `@vicons/ionicons5` 中未使用的 `PersonOutline`。
   - 移除类型导入中未使用的 `ChatChannelType`。

---

## 4. 前后端 API 与行为契约对齐

| 频道类型 | 前端参数 (`TicketTab.vue`) | 后端对应处理 (`CompetitionChatServiceImpl.java`) | 行为说明 |
| :--- | :--- | :--- | :--- |
| **`REFEREE`** (裁判答疑工单) | `channelType: 'REFEREE'`, `teamId: <selectedTeamId>` | 校验 `isReferee == true`，锁定对应队伍工单记录，拉取/发送工单沟通消息，WebSocket 广播至战队全员与裁判组 | 战队提问、裁判答疑的标准官方渠道 |
| **`TEAM`** (队伍公屏广播) | `channelType: 'TEAM'`, `teamId: <selectedTeamId>` | 校验 `isReferee == true`，将消息存入目标队伍公屏，WebSocket 广播至该队伍公屏并回显裁判长连接 | 裁判发布官方队伍通报或告警广播 |

---

## 5. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view/src/views/admin/competition/tabs/TicketTab.vue`
- **编译与类型验证**：
  - 前端工程：执行 `npm run build`，Vue 3 + TypeScript 编译顺利通过，生成零错误产物。
  - 后端工程：执行 `mvn compile -pl volur-competition -DskipTests`，编译通过（160 source files compiled, BUILD SUCCESS）。
- **向后兼容性**：
  - 选手端（`VolurChatDrawer.vue`）内的战队内部队员点对点私信保持完全正常，不受任何影响。
  - 裁判端界面更精炼直接，彻底消除了冗余交互与误用歧义。
