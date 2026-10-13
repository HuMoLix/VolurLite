# CTF 竞技场真实 API 全面接入与 Zero-Mock 架构重构

## 1. 变更背景与动因
在 Volur Lite NEXT 前端工程中，`src/views/competition/CTFCompetitionView.vue` 及下属赛题列表、题目弹窗最初包含用于 UI 原型联调的本地 Mock 数据（如 `MOCK_COMPETITION`、`MOCK_CHALLENGES`、`MOCK_ANNOUNCEMENTS`、`MOCK_SCOREBOARD`、`MOCK_MY_TEAM` 及 `MOCK_CHALLENGE_DETAILS`）。同时，弹窗内部存在本地硬编码的 Flag 判定（如判定前缀 `volur{`、`flag{`、`123456` 直接通过）与伪造动态靶机容器（端口 30000+）逻辑。

根据项目根规范 `GEMINI.md` 与 `view-NEXT-rules.md`：
- **Zero-Mock & Fail-Fast 铁律**：严禁隐式 Mock 与伪造执行成功，必须全面对接 Spring Boot 后端真实 Controller 接口；
- **纯粹代码与零注释原则**：代码必须凭借清晰的结构与精准命名保持高度自解释，所有架构决策统一外置记录于设计文档中。

---

## 2. 核心改动明细

### 2.1 主视图重构 (`CtfCompetitionView`)
1. **删除全部硬编码 Mock 数据**：
   - 彻底删除 `MOCK_COMPETITION`、`MOCK_CHALLENGES`、`MOCK_ANNOUNCEMENTS`、`MOCK_SCOREBOARD`、`MOCK_MY_TEAM`；
   - 彻底删除 `applyMockData()` 及其在请求异常时的降级回退逻辑。
2. **接入真实 API 聚合请求 (`fetchCtfData`)**：
   - 优先依据路由参数（ID 或 Slug）请求 `getCompetitionByIdApi` / `getCompetitionBySlugApi` 获取真实竞赛详情；
   - 若竞赛不存在或未找到，提示错误并置空数据，杜绝伪造竞赛实体；
   - 采用 `Promise.allSettled` 并行发起子资源加载：
     - `listCtfChallengesApi(compData.id)`：加载 CTF 赛题列表；
     - `listAnnouncementsApi(compData.id)`：加载官方赛事公告；
     - `getCtfScoreboardApi(compData.id)`：加载 CTF 解题赛实时天梯排行榜；
     - `getMyTeamApi(compData.id)`：若用户处于登录态则加载当前战队信息，未登录时置空。
3. **排行榜实时刷新机制 (`handleFlagSolved`)**：
   - 当选手成功提交 Flag 并夺旗后，本地即时更新该赛题解出标记与队伍得分；
   - 异步触发 `refreshScoreboard` 从后端拉取最新排行榜物化数据。

### 2.2 赛题列表视图重构 (`CtfCompetitionChallengeListView.vue`)
1. **移除 `MOCK_CHALLENGE_DETAILS` 字典**：
   - 彻底移除题目 1001-1010 的所有本地 Mock 描述与附件数据。
2. **真实赛题详情下发与快速失败**：
   - 点击题目卡片时调用 `getCtfChallengeDetailApi(props.competition.id, targetId)`（优先使用竞赛赛题主键 `ch.id`，兼容 `ch.challengeId`）；
   - 请求成功时弹出真实赛题模态窗，请求失败时使用 Naive UI `message.error` 进行拦截提示，杜绝伪造兜底。

### 2.3 赛题模态弹窗与动态靶机重构 (`CtfCompetitionChallengeModal.vue`)
1. **真实 Flag 校验流**：
   - 移除 `trimmed.startsWith('volur{')`、`'123456'` 等所有前端伪造分支；
   - 严格通过 `submitCtfFlagApi` 发送真实 Payload，依据后端 `ApiResponseEntity<Boolean>` 的 `data` 判定是否夺旗，并展示真实响应消息。
2. **真实容器生命周期对接**：
   - 移除伪造端口生成（`30000 + Math.random() * 5000`）与假装启动的异步延时；
   - 启动：通过 `startCtfInstanceApi(competitionId, targetId)` 调度真实独立容器，绑定分配的 `accessUrl` 或 `host:port`；
   - 续期：通过 `extendCtfInstanceApi(competitionId, targetId)` 增加租期；
   - 销毁：通过 `stopCtfInstanceApi(competitionId, targetId)` 释放并清理定时器；
   - 倒计时：自适应兼容后端返回的 `remainingTimeSeconds` 与 `remainingSeconds`。

### 2.4 我的战队状态空态强化 (`CtfCompetitionTeamView.vue`)
- 当登录用户尚未加入或组建任何审核通过的战队（`myTeam === null`）时，呈现规范的浅灰居中空状态提示卡片，消除原有的破损占位符。

### 2.5 实体类型与接口定义扩充 (`types/competition` & `api/competition`)
- `CtfScoreboardVO`：严格映射后端 `CtfScoreboardVO`，包含 `competitionId`、`isFrozen` 与 `rankings: CtfScoreboardItemVO[]`；
- `CtfChallengeInstanceVO`：扩展 `challengeId`、`accessUrl`、`expiresAt`、`remainingTimeSeconds`；
- `getCtfScoreboardApi`：函数签名强类型修正为 `Promise<ApiResponse<CtfScoreboardVO>>`。

---

## 3. 影响范围与验证结果
- **变更文件**：
  - `src/views/competition/CTFCompetitionView.vue`
  - `src/views/competition/tabs/CtfCompetitionChallengeListView.vue`
  - `src/views/competition/tabs/CtfCompetitionTeamView.vue`
  - `src/views/competition/components/CtfCompetitionChallengeModal.vue`
  - `src/types/competition/index.ts`
  - `src/api/competition.ts`
- **构建测试**：
  - 运行 `vue-tsc --noEmit && vite build`，全量 TypeScript 校验 100% 通过，构建耗时 8.34s，生产打包完全正常，无任何编译报错或未捕获类型缺陷。
