# 竞赛动态状态生命周期演进与状态校验 AOP 统一架构审计报告

- **报告日期**：2026-09-15
- **任务代号**：COMPETITION-STATUS-AOP-REGISTRATION-LIFECYCLE
- **执行模块**：[`volur-competition`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition)
- **状态**：已完成 (Completed)

---

## 1. 变更背景与架构痛点

在原有设计中，竞赛战队报名、信息修改与解散等流程存在以下结构性痛点：
1. **私有校验逻辑重复冗余**：
   [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 内部声明了私有方法 `validateRegistrationPeriod(Competition comp)`，并在 `signupTeam`、`updateTeam`、`deleteMyTeam`、`acceptInvitation` 4 处方法体首行手工调用，属于典型的样板代码侵入；
2. **状态与时间窗口割裂**：
   虽然 [`CompetitionStatus`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/CompetitionStatus.java) 枚举已包含 `REGISTRATION(5, "REGISTRATION")`，但核心实体 [`Competition.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/Competition.java) 的 `resolveStatusByTime()` 从未根据报名时间窗口计算过 `REGISTRATION` 状态，导致生命周期状态无法真实映射到业务模型；
3. **切面与注解职责单一且割裂**：
   原切面仅服务于运行态校验（`RequiredCompetitionRunning`），无法满足战队报名在“报名中”（`REGISTRATION`）以及未来更多状态下的声明式校验需求；且 `@Transactional` 事务内手工校验容易造成数据库连接资源的无效占用。

---

## 2. 核心架构改动点

### 2.1 实体层：动态计算“报名中” (`REGISTRATION`)
在 [`Competition.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/Competition.java) 的 `resolveStatusByTime()` 中原生注入时间窗口解析逻辑：
- 若 `now > endTime`：返回 `ENDED`；
- 若 `now >= startTime`：返回 `RUNNING`；
- 若比赛未开赛且处于发布态（`now < startTime`）：
  - 检查当前时间是否位于报名起止窗口内（`!hasRegStart || now >= regStartTime` 且 `!hasRegEnd || now <= regEndTime`）；
  - 满足则动态计算为 `REGISTRATION`（报名中）；
  - 否则保留为 `PUBLISHED`（已发布未开赛/报名已截止）。

### 2.2 注解层：新建通用注解并下线历史单一注解
1. **新建 [`RequiredCompetitionStatus.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionStatus.java)**：
   - 属性 `allowed()`：允许的竞赛状态集合（默认 `{CompetitionStatus.RUNNING}`）；
   - 属性 `types()`：可选的赛制模式集合（如 CTF/AWD/ISW 等）；
   - 属性 `key()`：可选的 SpEL 表达式提取器。
2. **彻底删除 [`RequiredCompetitionRunning.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionRunning.java)**：
   - 全面由 `@RequiredCompetitionStatus` 替代，保持架构统一与整洁。

### 2.3 切面层：重构为 `CompetitionStatusValidAspect`
1. 将原 `CompetitionValidationAspect` 重构并重命名为 [`CompetitionStatusValidAspect.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/aspect/CompetitionStatusValidAspect.java)；
2. 切面统一环绕拦截 `@RequiredCompetitionStatus`，执行流程：
   - 提取 `competitionId`（支持 SpEL、参数名绑定、首个 Long 参数匹配及 DTO getter 提取）；
   - 加载实体并调用 `comp.refreshStatus()` 动态刷新实时状态；
   - 严格匹配 `allowed` 状态集；
   - 针对 `REGISTRATION` 与 `RUNNING` 场景提供精准且友好的中文业务异常提示（例如“竞赛尚未发布，暂不开放报名”、“比赛已开始，报名通道已关闭”、“报名尚未开启”、“报名已截止”等），保障与历史体验 100% 兼容；
   - 匹配可选的 `types` 赛制模式约束。

### 2.4 业务服务层重构
1. **战队服务 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)**：
   - 在 `signupTeam`、`updateTeam`、`deleteMyTeam`、`acceptInvitation` 4 个方法上统一标注 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.REGISTRATION})`；
   - 移除上述方法内部所有 `validateRegistrationPeriod(competition);` 样板调用；
   - 彻底删除私有辅助方法 `validateRegistrationPeriod`。
2. **平替运行态服务**：
   - [`CompetitionScoreboardServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionScoreboardServiceImpl.java)：改用 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING})`；
   - [`CtfCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/ctf/impl/CtfCompetitionServiceImpl.java)：改用 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.CTF})`；
   - [`IswCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/isw/impl/IswCompetitionServiceImpl.java)：改用 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.ISW})`；
   - [`AwdCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/awd/impl/AwdCompetitionServiceImpl.java)：改用 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.AWD})`；
   - [`AwdPlusCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/awdp/impl/AwdPlusCompetitionServiceImpl.java)：改用 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.AWD_PLUS})`；
   - [`TheoryCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/theory/impl/TheoryCompetitionServiceImpl.java)：改用 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.THEORY})`。
3. **管理端排程校验优化 [`AdminCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminCompetitionServiceImpl.java)**：
   - `updateCompetition` 中的 `isNotStarted` 判断扩展包含 `CompetitionStatus.REGISTRATION`，避免管理端在报名阶段微调时间时被误拦截。

---

## 3. 安全与性能效益说明

1. **Fail-Fast 防御前置**：
   AOP 切面在进入 `@Transactional` 之前优先完成竞赛状态与时段核验。若状态不合法直接抛出 `BusinessException` 响应，**避免了底层数据库连接的开启、事务上下文维护及锁资源浪费**，大幅提升高并发抢注报名阶段的吞吐性能。
2. **领域职责内聚**：
   报名是否开放由领域模型 `Competition` 自身根据当前时间统一判定，Service 层不再散落重复的状态与时序比对，彻底消除了不同接口可能出现的判定偏差与安全漏洞。

---

## 4. 验证结果

1. **单元测试**：
   - 新增 [`CompetitionStatusValidAspectTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/aspect/CompetitionStatusValidAspectTest.java)，8 个测试用例覆盖草稿拦截、比赛开始/结束拦截、报名开启前拦截、报名截止后拦截、时间窗口放行、运行态放行及赛制校验；
   - 运行 `mvn test -pl volur-competition`：14 个测试用例全部通过（100% SUCCESS）。
2. **全平台模块编译与安装**：
   - 运行 `mvn clean install -DskipTests`：全平台 12 个模块（`volur-framework`, `volur-logging`, `volur-system`, `volur-user`, `volur-verification`, `volur-notification`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-competition`, `volur-server`）全部编译与打包成功。
