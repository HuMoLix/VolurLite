# 施工与审计记录：竞赛与赛段运行状态 AOP 注解化重构与架构升级

## 1. 基本信息

- **任务名称**：竞赛/赛段运行态校验注解化封装与 AOP 切面实现（`@RequiredCompetitionRunning` & `@RequiredCompetitionStageRunning`）
- **执行时间**：2026-08-25
- **影响模块与文件**：
  - `volur-competition`（新增注解、切面，重构 5 个赛段服务及计分板服务）
  - [`RequiredCompetitionRunning.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionRunning.java)
  - [`RequiredCompetitionStageRunning.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionStageRunning.java)
  - [`CompetitionValidationAspect.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/aspect/CompetitionValidationAspect.java)
  - [`CtfStageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/ctf/impl/CtfStageServiceImpl.java)
  - [`TheoryStageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/theory/impl/TheoryStageServiceImpl.java)
  - [`AwdStageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/awd/impl/AwdStageServiceImpl.java)
  - [`AwdPlusStageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/awdp/impl/AwdPlusStageServiceImpl.java)
  - [`IswStageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/isw/impl/IswStageServiceImpl.java)
  - [`CompetitionScoreboardServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionScoreboardServiceImpl.java)
  - [`volur-competition/pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/pom.xml)

---

## 2. 变更背景与架构动因

在重构前，所有赛段实现类（CTF、Theory、AWD、AWD+、ISW）中均在每个方法入口显式手动调用 `validateCompetitionRunning(competitionId)` 命令式方法，并在各服务类内部重复编写私有的校验逻辑：
1. **代码冗余度高**：每个 Service 方法第一行均包含手动校验，且各 Service 均维护一份相同的私有 `validateCompetitionRunning` 样板方法；
2. **缺乏赛段细粒度控制**：多赛段竞赛中，除了整场竞赛的起止时间与状态外，各个独立赛段（如 CTF、Theory 等）拥有独立的 `startTime`、`endTime` 及 `status`（PENDING / RUNNING / ENDED），手动校验无法统一且优雅地校验赛段维度的运行状态；
3. **依赖冗余**：部分仅需题目和解题仓储的 Service 被迫注入 `CompetitionRepository`，仅为了完成运行态校验。

---

## 3. 具体修改与设计实现

### 3.1 自定义声明式注解设计

1. **[`@RequiredCompetitionRunning`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionRunning.java)**：
   - 目标：用于整场竞赛级别的状态校验（如 Scoreboard 榜单查询、团队管理等）；
   - 支持 SpEL 表达式指定 `key`，未配置时自动按形参名 `competitionId`、首个 `Long` 参数或实体 `getCompetitionId()` 解析。

2. **[`@RequiredCompetitionStageRunning`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionStageRunning.java)**：
   - 目标：用于具体赛段（CTF、THEORY、AWD、AWD_PLUS、ISW）的操作前置校验；
   - 校验流程：
     1. 首先校验整场竞赛存在、非 DRAFT/ARCHIVED 且处于开始与结束时间区间之内；
     2. 探测目标赛段的 `CompetitionStageConfig` 配置，校验赛段状态非 PENDING、非 ENDED，且处于赛段起止时间区间之内。

### 3.2 AOP 切面实现（`CompetitionValidationAspect`）

- 使用 `@Around` 环绕通知拦截被注解标注的方法与类（方法级注解优先于类级）；
- 提供多级参数提取策略（SpEL ➔ 形参名 `competitionId` ➔ 首个 `Long` 实参 ➔ 对象属性反射探测）；
- 当状态不满足时抛出明确语义的 `BusinessException`（`NOT_FOUND`、`FORBIDDEN` 等）。

### 3.3 领域服务层全面重构与按需应用

1. **[`CtfStageServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/ctf/impl/CtfStageServiceImpl.java)**：
   - 标注 `@RequiredCompetitionStageRunning(stage = CompetitionStage.CTF)`；
   - 移除所有方法体内部手写的 `validateCompetitionRunning` 调用；
   - 移除类内部私有 `validateCompetitionRunning` 样板方法及冗余的 `CompetitionRepository` 依赖注入。
2. **[`TheoryStageServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/theory/impl/TheoryStageServiceImpl.java)**：
   - 标注 `@RequiredCompetitionStageRunning(stage = CompetitionStage.THEORY)`，移除样板校验。
3. **[`AwdStageServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/awd/impl/AwdStageServiceImpl.java)**：
   - 标注 `@RequiredCompetitionStageRunning(stage = CompetitionStage.AWD)`，移除样板校验。
4. **[`AwdPlusStageServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/awdp/impl/AwdPlusStageServiceImpl.java)**：
   - 标注 `@RequiredCompetitionStageRunning(stage = CompetitionStage.AWD_PLUS)`，移除样板校验。
5. **[`IswStageServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/isw/impl/IswStageServiceImpl.java)**：
   - 标注 `@RequiredCompetitionStageRunning(stage = CompetitionStage.ISW)`，移除样板校验。
6. **[`CompetitionScoreboardServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionScoreboardServiceImpl.java)**：
   - 标注 `@RequiredCompetitionRunning`，确保榜单数据查询在有效竞赛状态下进行。

---

## 4. 验证与审计结果

- 执行 `mvn clean test-compile` 全量构建，全部 11 个模块编译通过（`BUILD SUCCESS`，耗时 9.0s）；
- 各 Service 代码大幅精简，彻底消除了冗余的方法级样板调用与私有校验方法。
