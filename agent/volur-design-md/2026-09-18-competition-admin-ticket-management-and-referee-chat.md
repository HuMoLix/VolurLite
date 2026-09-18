# 竞赛管理端工单管理搭建与比赛前台裁判拉取剥离设计文档

**日期**：2026-09-18  
**类型**：前后端全栈功能开发 / 业务架构重构  
**影响模块**：
- 前端：`src/volur-lite-view`（`TicketTab.vue`、`AdminCompetitionEditView.vue`、`competitionChat.ts`、`VolurChatDrawer.vue`、中英文国际化）
- 后端：`src/volur-lite-platform/volur-competition`（`CompetitionChatServiceImpl`、`CompetitionChatSessionManager`、`CompetitionChatRedisSubscriber`）

---

## 1. 变更背景与需求说明

1. **前台比赛页面剥离管理员专属队伍工单拉取**：
   - 此前当管理员或裁判账号访问竞赛竞技场（如 `/arena/test-comp-01`）时，WebSocket 连接就绪帧自动触发了 `fetchRefereeTeams()` 请求 `/api/v1/competitions/{id}/chat/referee/teams`；
   - 竞技场前端抽屉是面向选手的赛场环境，不应在此拉取管理端所有队伍列表；
   - 现需彻底移除前台比赛页面中对管理员拉取裁判队伍工单列表的逻辑，保持前台通信轻量纯粹。

2. **管理后台新增工单管理标签页 (`TicketTab.vue`)**：
   - 在竞赛管理详情页 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 的标签页集合中新增「工单管理」标签页（`TicketTab.vue`）；
   - 提供双栏工作台：
     - 左栏：参赛队伍列表，支持按队伍名称/队长实时搜索，支持按“全部队伍”与“有工单”快速筛选，展示未读与最新工单摘要；
     - 右栏：主交互控制台，支持裁判与队伍进行全方位多维交流。

3. **支持裁判与队伍及参赛成员的三维双向交流**：
   - **裁判答疑申诉工单 (`REFEREE`)**：承接队伍向裁判组发起的答疑与争议工单，裁判与队伍进行专属对话；
   - **队伍公屏广播 (`TEAM`)**：裁判可在后台直接向指定队伍频道发送文字（如官方提醒、作弊警告、赛况通知），该队伍全体队员均可见；
   - **队伍不能在队伍频道中 @ 裁判**：队伍公屏中严格禁止队员 `@` 裁判（在头像点击、输入框候选及后端发信接口全方位剔除与拦截裁判）；
   - **队员点对点私信 (`PRIVATE`)**：裁判可选择队伍内具体成员（如队长或某选手）进行 1 对 1 点对点交流。

---

## 2. 具体改动点分析

### 2.1 前台比赛页面移除管理员拉取队伍工单

1. **Pinia 状态机 ([`competitionChat.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/competitionChat.ts))**：
   - 移除 WebSocket `connectedReady` 帧处理中的 `if (data.isReferee) fetchRefereeTeams()` 自动拉取；
   - 移除 `handleIncomingMessage` 中针对缺失队伍重复触发 `fetchRefereeTeams()` 的逻辑。
2. **竞技场抽屉 ([`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue))**：
   - 下拉选项精简：前台抽屉仅保留固定的 `key: 'REFEREE', label: '裁判'` 选项，不再在前台向管理员展开全场队伍列表；
   - 防 @ 裁判：点击裁判头像或名称时拦截 `@` 注入（`if (isRefereeMessage(msg)) return`），光标 hover 样式同步禁用。

### 2.2 后端通信权限与多频道发信加固

1. **服务层改造 ([`CompetitionChatServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionChatServiceImpl.java))**：
   - **队伍公屏 (`TEAM`)**：放行管理员与裁判（`isReferee = true`），根据传入的 `dto.getTeamId()` 向指定队伍频道发送文字，`senderRole` 标记为 `REFEREE`；
   - **队员私信 (`PRIVATE`)**：放行管理员与裁判向指定 `receiverId` 的参赛成员直接发送私信；
   - **队伍频道禁止 @ 裁判拦截**：在队伍公屏发信逻辑中，对入参 `mentionUserIds` 进行严格过滤，排除任何角色为 `ADMIN` 或 `SUPER_ADMIN` 的用户 ID，从数据库与业务协议层彻底杜绝队伍在公屏 @ 裁判；
   - **历史查询 (`getHistoryMessages`)**：放行裁判通过 `query.getTeamId()` 分页查阅任意队伍的公屏历史记录与工单历史记录。
2. **长连接消息广播调度**：
   - 在 [`CompetitionChatSessionManager.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/session/CompetitionChatSessionManager.java) 中将 `sendToUser` 方法权限提升为 `public`；
   - 在 [`CompetitionChatRedisSubscriber.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/redis/CompetitionChatRedisSubscriber.java) 中，当收到 `TEAM` 频道广播时，除了推送给全队成员外，若发送者为裁判或非队内连接，自动向发送者本人长连接推送消息回显。

### 2.3 管理后台工单管理标签页 (`TicketTab.vue`)

1. **组件实现 ([`src/views/admin/competition/tabs/TicketTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TicketTab.vue))**：
   - **左侧队伍列表**：
     - 整合 `listAdminTeamsApi`（参赛队伍全量数据）与 `getRefereeTeamsApi`（裁判申诉工单概览）；
     - 支持关键字模糊检索队伍名称与队长名；
     - 支持“全部队伍”与“有工单”快速分栏切换；
     - 队伍卡片直观展示队伍名、工单标识、最新申诉内容摘要及时间戳。
   - **右侧主工作台**：
     - **顶部控制条**：展示队伍名称、ID、成员数量；提供「裁判答疑工单」、「队伍公屏广播」、「队员点对点私信」三大频道切换按键；
     - **私信成员选择条**：在私信模式下提供下拉选择器，可指定与队内任意成员（队长或队员）对话；
     - **消息流主展示区**：正序排列历史记录，高辨识度区分裁判消息（黑色/高对比气泡）与参赛选手消息（白色/浅灰气泡），显示官方「裁判组」与「参赛队员」徽章及精确时间；
     - **底部输入控制台**：动态提示当前发送目标与可见范围，支持 Enter 发送与快捷换行，支持发送状态反馈。
   - **实时通信链路**：
     - 挂载独立的 `ChatWebSocketClient` 实例，实时监听比赛消息推送并自动滚动刷新；
     - 发送消息调用 `sendChatMessageApi`，支持多频道与目标灵活投递。

2. **视图挂载与国际化**：
   - 在 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 中引入并注册 `ticket-tab` 标签面板；
   - 在 `zh_CN/admin/competitions.json` 添加 `"tab_tickets": "工单管理"`；
   - 在 `en_US/admin/competitions.json` 添加 `"tab_tickets": "Tickets & Chat"`。

---

## 3. 验证结果

1. **前端类型校验与生产构建**：
   - 执行 `npm run build`，严格通过 `vue-tsc --noEmit` 校验，无任何类型缺失或未引用警告，Vite 打包构建通过（耗时 6.33s）。
2. **后端模块编译**：
   - 执行 `mvn compile -pl volur-competition -DskipTests`，编译通过（`BUILD SUCCESS`）。
