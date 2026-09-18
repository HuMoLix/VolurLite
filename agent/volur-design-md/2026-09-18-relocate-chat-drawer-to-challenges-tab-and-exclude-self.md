# 竞赛聊天抽屉作用域收敛至 ChallengesTab 及私信排除自身优化文档

**日期**：2026-09-18  
**类型**：前端架构优化 / 交互体验与安全增强  
**影响模块**：`src/volur-lite-view`（竞赛竞技场、聊天抽屉及 Pinia 聊天状态机）

---

## 1. 变更背景与需求说明

在竞赛竞技场系统原有的实现中：
1. **聊天抽屉全局挂载问题**：`VolurChatDrawer.vue` 及其右下角悬浮触发按钮挂载在主容器 [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/CtfCompetitionArenaView.vue) 根层级。这导致无论选手切换到“公告 (announcements)”、“排行榜 (scoreboard)”还是“我的战队 (team)”页面时，右下角悬浮按钮和聊天抽屉都持续浮现在页面视口中。按照业务交互要求，聊天组件应仅专属于赛题演练区，即仅挂载在 [`ChallengesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/tabs/ChallengesTab.vue) 内部。
2. **“发送给：”标签后目标名称加粗样式不协调**：[`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue) 底部工具栏中的沟通对象下拉按钮（`.target-display-name`）设置了 `font-weight: 600`，字重过粗，需要取消加粗样式（改为 `font-weight: 400`），保持界面统一与轻量。
3. **支持给自己发送私信的逻辑漏洞**：在沟通对象选择器中，由于用户 ID 比对存在潜在漏洞（如 `currentUserId` 未能及时匹配、或未用 ID 严格过滤），选手可能会在私信列表中看到自己并向自己发送私信，造成聊天数据混乱。需在 Store 状态派生、下拉候选过滤、目标切换和发信校验全链路用用户唯一 ID 彻底排除自身。

---

## 2. 具体改动点分析

### 2.1 聊天入口与抽屉从主视图剥离并收敛至 `ChallengesTab.vue`

1. **瘦身 [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/CtfCompetitionArenaView.vue)**：
   - 移除了模板中的 `.arena-chat-floating-launcher`（浮动按钮）与 `<VolurChatDrawer>` 组件标签；
   - 移除了未使用的依赖导入：`NBadge`、`ChatbubblesOutline`、`useCompetitionChatStore`、`VolurChatDrawer`；
   - 移除了 `chatStore` 实例、WebSocket 侦听器以及 `onUnmounted(() => chatStore.disconnectChat())`；
   - 移除了 `.arena-chat-floating-launcher` 相关 SCSS 样式。

2. **下沉至 [`ChallengesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/tabs/ChallengesTab.vue)**：
   - 在模板末尾加入 `.arena-chat-floating-launcher` 及 `<VolurChatDrawer>`；
   - 引入 `useCompetitionChatStore`、`VolurChatDrawer`、`NBadge`、`ChatbubblesOutline`；
   - 建立专属聊天生命周期管理：当当前竞赛实体 `currentCompetition.id` 准备就绪时自动建立聊天通道连接，组件销毁（即选手离开赛题 Tab 时）自动断开通道并回收资源；
   - 补充 `.arena-chat-floating-launcher` 固定浮动样式。

### 2.2 修改 `VolurChatDrawer.vue` 中“发送给：”目标名称样式

- 定位到抽屉底部工具栏沟通对象触发器：
  ```html
  <span class="target-display-name">{{ currentTargetDisplayName }}</span>
  ```
- 将 SCSS 中 `.target-display-name` 的 `font-weight: 600` 修改为 `font-weight: 400`；
- 同步在暗色模式 `.volur-chat-drawer.dark` 中显式指定 `font-weight: 400`，确保日间与黑夜模式下目标名称均不加粗。

### 2.3 全链路用 ID 与身份多源聚合排除自身，杜绝向自己发送私信与 @

#### 2.3.1 深度排查与根因剖析（为何此前仍能看到自己）
在进一步联调与排查中，发现此前仅依靠单一 ID 比对仍能偶尔看到自己的核心原因：
1. **WebSocket 传输中 FastJSON2 雪花算法（Snowflake ID）精度截断**：
   - 后端在 WebSocket 握手并推送 `READY` 报文时，Java `Long` 类型的 `userId`、`competitionId`、`teamId` 原先直接作为 JSON Number 输出；
   - 浏览器原生 `JSON.parse` 在解析超过 JavaScript `Number.MAX_SAFE_INTEGER`（$2^{53} - 1$）的长整型时发生低位截断（如后三位 `482` 被篡改为 `480`）；
   - 而通过 HTTP REST 接口获取的战队成员信息走的是 Spring Boot 全局 Jackson 配置（已配置 Long 转 String，保持高精度真值），导致两端 ID 末位不一致，字符串恒等判断失效。
2. **状态与加载时序差异**：
   - 选手直接刷新竞技场页面时，WebSocket 就绪事件与 Pinia `userStore.fetchCurrentUser()` 存在异步微小时间差，初始阶段可能为空；
   - 实体字段命名存在差异：登录认证返回实体为 `userId`，用户概要实体为 `id`。

#### 2.3.2 架构级彻底解决方案
1. **后端 WebSocket 传输协议加固**：
   - 在 [`CompetitionChatWebSocketHandler.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/handler/CompetitionChatWebSocketHandler.java) 中，明确将 `READY` 报文中的 `userId`、`competitionId`、`teamId` 强转为 `String` 字符串形式写入；
   - 在序列化方法中统一启用 `JSONWriter.Feature.WriteLongAsString` 特性，彻底切断所有后续长整型丢失精度的可能；
   - 在 [`CompetitionChatRedisSubscriber.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/websocket/redis/CompetitionChatRedisSubscriber.java) 中同样启用该安全特性。

2. **前端多源身份汇聚工具 ([`src/utils/auth.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/auth.ts))**：
   - 新增 `getDecodedTokenPayload()`：从当前 JWT Access Token 中直接解码提取 payload（包含 `sub`, `userId` 等不可篡改的第一手凭证）；
   - 新增 `getCurrentAuthCandidateIdentities(storeUser, readyData)`：多源提取候选身份集合（Set），包括：
     - JWT Payload 中的 `sub`、`userId`；
     - 本地 LocalStorage `volur_user_info` 中的 `id`、`userId`、`username`；
     - Pinia `userStore.user` 中的 `id`、`userId`、`username`；
     - WebSocket `chatStore.readyData` 中的 `userId`。
   - 新增 `isSelfMemberIdentity(target, storeUser, readyData)`：针对目标成员的 `userId`、`id` 与 `username`（忽略大小写）进行双向匹配，只要命中任意候选标识即可判定为本人。

3. **Pinia 状态机与组件层全量应用**：
   - 在 [`competitionChat.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/competitionChat.ts) 的 `teammateList` 计算属性中直接调用 `!isSelfMemberIdentity(m, userStore.user, readyData.value)`；
   - 在 [`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue) 中：
     - `targetDropdownOptions` 下拉菜单强力排除自己；
     - `mentionCandidateOptions` 输入 `@` 候选名单强力排除自己；
     - `handleTargetDropdownSelect`、`handleSend`、`isSelfMessage`、`isMentionedMe` 及侦听器自动重置为 `'TEAM'` 均严格基于该多源身份判定，杜绝任何漏网可能。

### 2.4 界面轻量化与角色标识精简

1. **取消聊天框中的“队长”及普通队员标识**：
   - 选手在战队内协同交流时，通常不需要每条消息侧边均醒目张贴“队长”或“队员”标签，过多标签造成视觉干扰；
   - 在 [`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue) 中优化 `formatRoleName` 与 `getRolePillClass`：
     - 当角色为 `CAPTAIN`、`MEMBER` 或 `LEADER` 时统一返回空字符串 `''`；
     - 消息头部角色药丸添加 `v-if="formatRoleName(msg.senderRole)"` 条件渲染；
     - 仅在收到 `REFEREE`、`ADMIN`、`SUPER_ADMIN` 等官方裁判组消息时，保留并展示专有的高辨识度“裁判组”药丸。

2. **标题精简：由“竞赛协同交流”修改为“聊天”**：
   - 将 [`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue) 的默认标题属性（`props.title`）修改为 `'聊天'`；
   - 同步将 [`ChallengesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/tabs/ChallengesTab.vue) 右下角浮动启动按钮的 `title` 和 `aria-label` 统一变更为“打开聊天”。

3. **删除顶栏右侧“裁判”（发送给谁）指示药丸**：
   - 在抽屉顶栏右侧（`header-right`），此前配置了 `.active-target-pill`，用于指示当前激活的沟通对象（如 `裁判`、战队名等）；
   - 该药丸与抽屉底部发信栏中明确标注的“发送给: [沟通对象下拉选择]”形成视觉冗余，且易让选手产生误解；
   - 彻底移除 `header-right` 中的 `.active-target-pill` 及其计算属性 `targetPillClass` 与日夜模式 SCSS 样式；
   - 顶栏右侧仅保留精简的关闭（`X`）按钮，沟通对象的切换与指示统一收敛至底部输入工具栏。

---

## 3. 验证结果

1. **类型安全性分析**：
   - 执行 `npm run build`，通过 `vue-tsc --noEmit` 校验，无任何类型缺失或未引用警告；
2. **前端构建验证**：
   - Vite 生产环境打包顺利通过（`✓ built in 6.40s`），构建产物正常生成；
3. **后端编译验证**：
   - 执行 `mvn compile -pl volur-competition -DskipTests`，编译顺利通过（`BUILD SUCCESS`）。


