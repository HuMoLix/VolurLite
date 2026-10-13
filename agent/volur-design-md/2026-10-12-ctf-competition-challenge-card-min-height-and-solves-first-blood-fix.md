# CTF 竞技场赛题卡片 min-height 保障与一血/已解出战队全链路数据贯通重构方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）与后端竞赛服务（`volur-competition`）的 CTF 竞技场场景中：
1. **赛题卡片高度坍塌问题**：上一步在 `CtfCompetitionChallengeCard.vue` 中移除了未解出题目底部的「暂无队伍解出 · 等待一血」占位结构与分割线后，未解出赛题缺少了底部区域的高度支撑，导致其与已有队伍解出的卡片在栅格中产生高度不齐或卡片内容垂直布局不协调的问题，需为卡片设立科学的 `min-height`（`min-h-[140px]`）并使标题自适应居中；
2. **解题后一血与已解出战队未展示缺陷**：
   - 选手在比赛中完成题目 Flag 提交后，赛题卡片与详情模态框（`CtfCompetitionChallengeModal.vue`）的「已解出战队」Tab 均未能实时显示对应战队及一血勋章；
   - 经全链路架构审计，根本原因在于：
     1. 后端 `CtfCompetitionChallengeVO` 与 `CtfCompetitionChallengeDetailVO` 原先未声明并下发 `topTeams` / `solves` 字段，且 `listChallenges` / `getChallengeDetail` 未联查 `CtfCompetitionSolved` 记录；
     2. 前端模态框提交 Flag 成功后，未将当前战队即时同步进赛题的 `topTeams` 与 `solves`，亦未在提交成功后重新拉取该题目最新的详情数据；
     3. 模态框向外广播的 `flag-solved` 事件参数中的 `challengeId` 与列表匹配键不一致，且列表视图与主视图未实时同步战队解题榜与列表数据；
3. **模态框空状态精简**：
   - 在 `CtfCompetitionChallengeModal.vue` 的已解出队伍空状态中，移除时钟 SVG 图标（`TimeOutline`），并将「暂无队伍解出 · 等待一血」精炼为「暂无队伍解出」。

---

## 2. 核心改动点与技术实现

### 2.1 赛题卡片布局稳定性与 min-height 保障 (`CtfCompetitionChallengeCard.vue`)

1. **设置基准 min-height**：在卡片最外层容器增加 `min-h-[140px]`，精确弥补移除底部等待区域后的高度落差，确保在不同解题状态与栅格排版下卡片规格高度保持高度统一；
2. **自适应居中对齐**：题目标题容器调整为 `my-auto py-2`，当底部无解题队伍时不贴底显示，在上下空间中自适应垂直居中。

### 2.2 模态框空状态精简与多语言扩展 (`CtfCompetitionChallengeModal.vue` & `locales`)

1. **移除 SVG 图标**：彻底移除空状态下的 `TimeOutline` 圆形图标包装节点；
2. **文案精简与多语言对齐**：
   - 在 `src/locales/zh-CN/competition.json`、`zh-TW/competition.json`、`en-US/competition.json` 中统一新增 `no_teams_solved` 键值；
   - 模态框内绑定 `t('competition.no_teams_solved', '暂无队伍解出')`。

### 2.3 后端领域模型与业务层全链路数据贯通 (`volur-competition`)

1. **新建赛题解题视图模型 (`CtfChallengeSolveVO.java`)**：
   - 存放于 `com.humolix.volur.competition.domain.vo.ctf.CtfChallengeSolveVO`；
   - 包含 `teamId`、`teamName`、`avatarUrl`、`solvedAt`、`points`、`isFirstBlood` 等强类型字段。
2. **扩展赛题列表与详情 VO**：
   - `CtfCompetitionChallengeVO`：增加 `private List<String> topTeams`，承载前三血战队名称；
   - `CtfCompetitionChallengeDetailVO`：增加 `private List<String> topTeams` 与 `private List<CtfChallengeSolveVO> solves`。
3. **持久层查询能力增强 (`CtfCompetitionSolvedRepository.java`)**：
   - 新增 `@Query` 方法 `findByCompetitionChallengeIdWithDetails`，通过 `JOIN FETCH s.team t` 一次性关联加载解出队伍及基础信息，避免懒加载异常与 N+1 查询。
4. **服务层缓存底座装配 (`CtfCompetitionServiceImpl.java`)**：
   - `listChallenges`：加载底座时批量联查 `allSolves`，按 `solvedAt ASC` 提取每个题目前 3 名战队写入 `topTeams`；
   - `getChallengeDetail`：加载底座时联查赛题的 `solves`，映射为 `CtfChallengeSolveVO` 列表及 `topTeams` 列表；
   - `submitFlag`：解题成功后原先已内置缓存失效指令，数据可即时回源刷新。

### 2.4 前端实时响应与即时更新流 (`Modal`, `ListView`, `MainView`)

1. **模态框即时同步与回源拉取 (`CtfCompetitionChallengeModal.vue`)**：
   - 接收 `myTeam?: MyTeamInfoVO | null` 属性；
   - Flag 提交判定成功时，本地立即将 `props.myTeam.teamName` 写入 `props.challenge.topTeams` 与解出列表；
   - 异步调用 `getCtfChallengeDetailApi(props.competitionId, targetId)` 重新拉取服务端真实数据并通过 `Object.assign` 合并；
   - 广播 `flag-solved` 时统一采用比赛题目主键 `targetId`。
2. **列表视图实时更新与透传 (`CtfCompetitionChallengeListView.vue`)**：
   - 将 `myTeam` 透传至 `CtfCompetitionChallengeModal`；
   - `handleFlagSolvedModal` 精准比对 `c.id === targetId || c.challengeId === targetId`，并向 `found.topTeams` 追加当前队伍名称（首位即一血）；
   - `openChallengeDetail` 优先使用详情接口返回的真实 `topTeams`。
3. **主竞技场视图即时刷新 (`CTFCompetitionView.vue`)**：
   - `handleFlagSolved` 更新本地 `challenges` 数组，并异步重新调用 `listCtfChallengesApi` 获取最新赛题列表与榜单。

---

## 3. 安全隐患防范与规范审查

1. **纯粹代码与零注释铁律**：后端与前端代码均保持纯净紧凑，无冗余行内解释性注释；
2. **零 Mock 与快速失败**：全流程对接真实 Spring Boot API 与 JPA 实体，无任何隐式假数据；
3. **线程安全与缓存防护**：后端列表服务层仍严格通过 `.toBuilder()` 派生副本返回，杜绝并发篡改共享底座。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - 后端：
    - `com.humolix.volur.competition.domain.vo.ctf.CtfChallengeSolveVO.java` (新创)
    - `com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO.java`
    - `com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO.java`
    - `com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository.java`
    - `com.humolix.volur.competition.service.ctf.impl.CtfCompetitionServiceImpl.java`
  - 前端：
    - `src/views/competition/components/CtfCompetitionChallengeCard.vue`
    - `src/views/competition/components/CtfCompetitionChallengeModal.vue`
    - `src/views/competition/tabs/CtfCompetitionChallengeListView.vue`
    - `src/views/competition/CTFCompetitionView.vue`
    - `src/locales/zh-CN/competition.json`
    - `src/locales/zh-TW/competition.json`
    - `src/locales/en-US/competition.json`
- **验证结果**：
  - 后端 `mvn -pl volur-competition test`：45 个测试全部运行通过（BUILD SUCCESS，0 failures，0 errors）；
  - 前端 `vue-tsc --noEmit && vite build`：构建测试全部通过（0 errors，耗时 17.52s，生产产物打包完成）。
