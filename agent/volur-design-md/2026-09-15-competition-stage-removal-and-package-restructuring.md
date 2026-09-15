# 竞赛阶段 (Stage) 冗余结构全面清理与包结构重构审计报告

---

## 1. 变更背景与业务分析

在 Volur Lite 历史演进中，早期设计曾经考虑过在一场竞赛内串联多个阶段（例如第一赛段为理论赛、第二赛段为 CTF 解题赛、第三赛段为 AWD 攻防赛）的"一赛多制"复合赛程。然而：
1. **赛制模式早已定型取消多赛段**：在 2026-09-11 的一赛一制架构重构中，平台已经全面确立了"一赛一制（Single Competition Mode）"的单一竞技模式（每场比赛由 `Competition.type` 显式指定：`CTF`、`THEORY`、`AWD`、`AWD_PLUS`、`ISW`），彻底移除了底层数据库关联的 `CompetitionStageConfig` 实体与外键 `stage_id`。
2. **业务中使用 Stage 的实际核验**：
   - 经对 `volur-competition` 业务逻辑的全量检索与数据流分析，**业务层没有任何地方真正依赖赛段流转或 `stageId`**；
   - 此前代码中仅残留历史痕迹，例如 `CtfCompetitionController.listChallenges` 接口中接收但直接被忽略的 `@RequestParam(required = false) Long stageId`，以及调用容器实例时硬编码传递的 `stageId = 0L`；
   - 此外，母题库 `volur-challenge` 本身作为纯粹的公共题库资产池，与竞赛阶段毫无从属关系，却历史性地在包命名中加入了 `.stage.`（如 `controller.v1.admin.stage`、`domain.entity.stage` 等），造成概念混淆与目录层级冗余。

为践行代码库的极致清爽与单一真实数据源原则，本次重构对 `volur-competition`、`volur-challenge`、`volur-container` 与前端 `volur-lite-view` 中的所有 `stage` 冗余包层级、命名与路径进行了系统性清理。

---

## 2. 核心改动点与重构明细

### 2.1 `volur-competition` 模块重构

1. **包路径与分层重构（彻底抹除 `.stage.` 层级）**：
   - 控制器层：
     - `controller/v1/stage/CtfStageController.java` $\rightarrow$ [`controller/v1/ctf/CtfCompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/ctf/CtfCompetitionController.java)
     - `controller/v1/stage/AwdStageController.java` $\rightarrow$ [`controller/v1/awd/AwdCompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/awd/AwdCompetitionController.java)
     - `controller/v1/stage/AwdPlusStageController.java` $\rightarrow$ [`controller/v1/awdp/AwdPlusCompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/awdp/AwdPlusCompetitionController.java)
     - `controller/v1/stage/IswStageController.java` $\rightarrow$ [`controller/v1/isw/IswCompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/isw/IswCompetitionController.java)
     - `controller/v1/stage/TheoryStageController.java` $\rightarrow$ [`controller/v1/theory/TheoryCompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/theory/TheoryCompetitionController.java)
     - `controller/v1/admin/stage/AdminCtfMountController.java` $\rightarrow$ [`controller/v1/admin/ctf/AdminCtfMountController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/ctf/AdminCtfMountController.java)
   - 领域模型与仓储层：
     - `domain/dto/stage/*` $\rightarrow$ `domain/dto/{ctf, awd, awdp, isw, theory}/*`
     - `domain/entity/stage/*` $\rightarrow$ `domain/entity/{ctf, awd, awdp, isw, theory}/*`
     - `domain/vo/stage/*` $\rightarrow$ `domain/vo/{ctf, awd, awdp, isw, theory}/*`
     - `repository/stage/*` $\rightarrow$ `repository/{ctf, awd, awdp, isw, theory}/*`
   - 业务服务层：
     - `service/stage/{ctf,awd,awdp,isw,theory}/*` $\rightarrow$ `service/{ctf,awd,awdp,isw,theory}/*`（接口与实现重命名为 `*CompetitionService` / `*CompetitionServiceImpl`）
     - `service/admin/stage/ctf/*` $\rightarrow$ `service/admin/ctf/*`

2. **HTTP 路由与入参精简**：
   - 移除 RESTful 路由中的 `/stage/` 中缀：
     - `/api/v1/competitions/{competitionId}/stage/ctf` $\rightarrow$ `/api/v1/competitions/{competitionId}/ctf`
     - `/api/v1/competitions/{competitionId}/stage/awd` $\rightarrow$ `/api/v1/competitions/{competitionId}/awd`
     - `/api/v1/competitions/{competitionId}/stage/awdp` $\rightarrow$ `/api/v1/competitions/{competitionId}/awdp`
     - `/api/v1/competitions/{competitionId}/stage/isw` $\rightarrow$ `/api/v1/competitions/{competitionId}/isw`
     - `/api/v1/competitions/{competitionId}/stage/theory` $\rightarrow$ `/api/v1/competitions/{competitionId}/theory`
     - `/api/v1/admin/competitions/{competitionId}/stage/ctf/mount` $\rightarrow$ `/api/v1/admin/competitions/{competitionId}/ctf/mount`
   - 移除 `CtfCompetitionController.listChallenges` 中无意义的 `@RequestParam(required = false) Long stageId` 入参，服务层方法签名精简为 `listChallenges(Long competitionId, Long userId)`。

3. **注解与切面清理**：
   - 扩展 [`RequiredCompetitionRunning`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/annotation/RequiredCompetitionRunning.java) 注解，增加 `CompetitionType[] value() default {};` 属性，支持直接声明指定赛制模式校验（如 `@RequiredCompetitionRunning(CompetitionType.CTF)`）；
   - 更新 [`CompetitionValidationAspect`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/aspect/CompetitionValidationAspect.java)，统一支持无参（校验赛事运行态）与有参（校验特定赛制运行态）；
   - 彻底删除物理文件 `RequiredCompetitionStageRunning.java`。

4. **实体与枚举语义纠偏**：
   - 将原 `StageStatus` 物理重命名为 [`AwdRoundStatus.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/AwdRoundStatus.java)（`PENDING`, `RUNNING`, `PAUSED`, `ENDED`），准确表达 AWD 攻防轮次的状态，彻底解耦 Stage 概念；
   - 将 `CtfStageScoreboardVO` 重命名为 [`CtfScoreboardVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/scoreboard/CtfScoreboardVO.java)；
   - 将 `StageScoreDetailVO` 重构为 [`CompetitionScoreDetailVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/scoreboard/CompetitionScoreDetailVO.java)，通过 Jackson `@JsonProperty` 兼容既有前端字段序列化；
   - 更新 `volur-competition/pom.xml` 描述为 Single-Mode Competition。

---

### 2.2 `volur-challenge` 模块重构

1. **母题库分包净化**：
   - 管理员母题控制器：`com.humolix.volur.challenge.controller.v1.admin.stage.*` $\rightarrow$ `com.humolix.volur.challenge.controller.v1.admin.{ctf, awd, awdp, isw, theory}.*`；
   - DTO / VO / Entity / Repository / Service 均全面移除 `.stage.` 目录层级，平铺至各母题类别包下。
2. **枚举重构**：
   - 原 `ChallengeStageType` 实体物理重构为 [`ChallengeMode.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/enums/ChallengeMode.java)（`CTF`, `THEORY`, `AWD`, `AWD_PLUS`, `ISW`）；
   - [`Challenge.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/entity/Challenge.java) 及子类接口将 `getStageType()` 规范纠偏为 `ChallengeMode getMode()`。

---

### 2.3 `volur-container` 模块对齐

- 同步更新了 `CtfChallengeInstance`、`CtfInstanceServiceImpl`、`K3sRuntimeStrategy` 与 `DockerSwarmRuntimeStrategy` 中对 `volur-challenge` 母题实体与仓储的新包路径引用。

---

### 2.4 `volur-lite-view` 前端 100% 严格对齐

遵循 `view-Rules.md` 铁律：
1. **API 路径 1:1 对齐**：
   - [`src/api/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/competition/index.ts) 全面移除 CTF 答题端点与管理端挂载端点中的 `/stage/` 前缀；
   - 保持与后端 Controller 类注解 `@RequestMapping` 及方法注解 100% 一致。
2. **类型定义对齐**：
   - [`src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts) 对齐 `CtfScoreboardVO`，并保留 `CtfStageScoreboardVO` 类型别名以保持平滑过渡；
   - 导出 `getCtfScoreboardApi` 与兼容别名 `getCtfStageScoreboardApi`。

---

## 3. 验证与测试结果

### 3.1 后端平台构建与测试
1. **全模块测试编译**：
   - 执行 `mvn test-compile`：全平台 12 个子模块全部构建成功（`12/12 SUCCESS`）。
2. **单元测试验证**：
   - 执行 `mvn test -pl volur-competition`：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` 全部通过；
   - 执行 `mvn test -pl volur-container`：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` 全部通过。

### 3.2 前端工程验证
1. **TypeScript 类型校验与 Vite 打包**：
   - 在 `src/volur-lite-view` 下执行 `npm run build`（包含 `vue-tsc --noEmit && vite build`）；
   - 结果：**0 类型推断错误，0 语法警告，打包成功（built in 4.54s）**。

---

## 4. 影响范围与审计结论

- **影响范围**：`volur-competition`、`volur-challenge`、`volur-container`、`volur-lite-view`。
- **审计结论**：经过对整个代码库的深入梳理与彻底重构，成功消除了历史遗留的全部 Stage 伪概念与深层嵌套目录；前后端通信协议和领域模型达到高度精简与统一，彻底解决了技术债。
