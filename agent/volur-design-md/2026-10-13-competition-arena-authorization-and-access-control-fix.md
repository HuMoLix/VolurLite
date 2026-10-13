# 竞赛工作台与赛题数据全链路越权访问防护方案

## 1. 变更背景与隐患溯源
在 Volur 平台先前的实现中，任意用户（即使未登录或未报名任何战队）通过直接在浏览器输入竞赛工作台路由（如 `/competitions/:id/ctf`、`/competitions/:id/theory`、`/c/:id/ctf` 等）或直接调用后端 REST API，均可无门禁地浏览整场比赛的工作台界面及核心赛题数据。

经前后端全链路溯源分析，发现存在如下深层安全漏洞与设计缺陷：
1. **布局组件职责无鉴权**：
   `CompetitionLayout.vue` 纯粹为 UI 容器组件，未包含任何权限验证。
2. **前端路由与视图拦截缺失**：
   - 路由守卫仅配置了 `meta: { requiredAuth: true }`，仅校验是否具备用户 Token，未对该竞赛的报名与过审参赛状态进行门禁校验；
   - `CtfCompetitionView.vue` 与 `TheoryCompetitionView.vue` 在数据加载时未对未参赛用户实施物理阻断，通过 `Promise.allSettled` 静默吞掉无队伍状态，导致界面完全对外敞开；
   - `TheoryCompetitionView.vue` 甚至内建了 `applyMockData()` 模拟数据兜底逻辑，违背了 Fast-Fail 与 Zero-Mock 铁律。
3. **后端只对 Mutation 动作鉴权，Query 读取接口全量开放**：
   - `CtfCompetitionController.java` 中的 `/challenges` 与 `/challenges/{challengeId}` 均声明为 `@CurrentUserId(required = false) Long userId`；
   - `TheoryCompetitionController.java` 中的 `/paper` 声明为 `@CurrentUserId(required = false) Long userId`；
   - `AwdCompetitionController.java`、`AwdPlusCompetitionController.java`、`IswCompetitionController.java` 亦未在当前轮次/靶机/场景读取接口中校验参赛身份；
   - 底层 Service 只要查出题目且属于 `RUNNING` 状态，对普通游客与未参赛用户直接返回赛题详情、附件地址甚至理论考卷全部题目与选项。

---

## 2. 改造方案与改动点详情

### 2.1 后端控制器与服务层（volur-competition）
1. **强制登录约束**：
   - 将 `CtfCompetitionController` 的 `listChallenges` 与 `getChallengeDetail` 中的用户注入改为 `@CurrentUserId Long userId`（强制要求已登录）；
   - 将 `TheoryCompetitionController` 的 `getPaper` 改为 `@CurrentUserId Long userId`；
   - 将 `AwdCompetitionController` 的 `getCurrentRound`、`AwdPlusCompetitionController` 的 `listTargets`、`IswCompetitionController` 的 `listScenarios` 改为 `@CurrentUserId Long userId`。
2. **参赛资格与管理权限校验**：
   - 在各模式 Service（`CtfCompetitionServiceImpl`、`TheoryCompetitionServiceImpl`、`AwdCompetitionServiceImpl`、`AwdPlusCompetitionServiceImpl`、`IswCompetitionServiceImpl`）中新增 `validateUserParticipationOrAdmin(competitionId, userId)` 统一门禁检查：
     - 若当前用户为已审核通过战队队员（`getMyApprovedTeam != null`），直接放行；
     - 若非参赛队伍成员，进一步检查是否为管理员或超级管理员（`UserRole.ADMIN` / `UserRole.SUPER_ADMIN`）；
     - 若既非参赛队伍成员，又非管理员，立即抛出 `BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍")` 快速失败。
3. **单元测试补齐**：
   - 在 `CtfCompetitionCacheTest` 中补齐非参赛用户访问 403 阻断验证，以及管理员未参赛情况下的免鉴权查阅验证。

### 2.2 前端视图与路由门禁（volur-lite-view-NEXT）
1. **`CtfCompetitionView.vue` 访问控制增强**：
   - 进入 `fetchCtfData` 时，首先检查 `userStore.isLoggedIn`，未登录立即重定向至 `/v1/signin/identifier`；
   - 加载完竞赛基本信息后，首先拉取 `getMyTeamApi`，校验当前登录用户是否具备 `userStore.isAdmin` 或战队状态为 `AUDIT_PASSED`；
   - 若不具备参赛资格，弹出错误 Toast 并强制 `router.replace` 回退至竞赛详情页 `/competitions/${id}`；
   - 当赛题接口返回 403 越权拒绝时，同样执行拦截与返回。
2. **`TheoryCompetitionView.vue` 门禁与 Zero-Mock 治理**：
   - 彻底移除 `applyMockData()` 及假数据结构，遵循 Fail-Fast 规范；
   - 进入考场前强制核验队伍审核通过状态，非参赛人员拦截重定向回详情页；
   - 若试卷获取失败直接提示错误并回退，禁止伪造试卷。
3. **多语言词条同步**：
   - 在 `zh-CN/competition.json`、`en-US/competition.json`、`zh-TW/competition.json` 中统一新增 `not_participating` 国际化文案。

---

## 3. 影响范围与验证情况
- **后端测试**：`CtfCompetitionCacheTest` 全量测试用例通过（8/8 passed）。
- **前端类型检查**：`vue-tsc --noEmit` 0 错误全量通过。
- **业务兼容性**：
  - 管理员与裁判仍然拥有对赛题、试卷、靶机环境的全局监考与巡检权限；
  - 已过审参赛选手体验流畅，完全不受影响；
  - 未报名或审核未通过的用户被彻底阻断在考场与竞技场之外，保障了竞赛公平与赛题保密性。
