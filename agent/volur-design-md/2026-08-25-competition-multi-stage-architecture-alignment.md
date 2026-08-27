# 竞赛多阶段（支持同类型多赛段）架构对齐与编译问题修复

## 1. 变更背景与需求

1. **编译构建问题修复**：修复了由于 `volur-competition` 模块中 `CtfStageServiceImpl` 引用缺失以及 `CompetitionValidationAspect` 中 `List` 导入缺失导致的编译错误。
2. **同类型多阶段架构支持**：在实际 CTF 与网络安全演练竞赛中，同一场竞赛可能包含多个相同类型的赛段（例如：Stage 1 为「CTF 初赛 / 资格赛」，Stage 3 为「CTF 总决赛」；或存在多个分段 AWD 攻防阶段）。此前部分查询方法（如 `findByCompetitionIdAndStage` 返回单体 `Optional`）在存在多个同类型赛段时会引发查询非唯一异常，且赛题未显式关联到具体的 `stage_id`。

---

## 2. 核心架构与模型改动

### 2.1 赛段配置持久层增强 (`CompetitionStageConfigRepository`)
- 新增 `List<CompetitionStageConfig> findByCompetitionIdAndStageOrderByStageOrderAsc(Long competitionId, CompetitionStage stage)`；
- 新增 `Optional<CompetitionStageConfig> findFirstByCompetitionIdAndStageOrderByStageOrderAsc(Long competitionId, CompetitionStage stage)`；
- 改造 `CompetitionValidationAspect`：当校验指定阶段类型时，扫描该竞赛下该类型的所有赛段，只要当前处于任意一个正在开放运行的同类型赛段内即放行通过，不再受单阶段限制。

### 2.2 挂载实体显式关联赛段 (`stage_id`)
在以下竞赛赛题挂载实体中显式引入 `@ManyToOne stage`（关联 `CompetitionStageConfig`）：
1. [`CtfCompetitionChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/stage/ctf/CtfCompetitionChallenge.java)
2. [`AwdCompetitionChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/stage/awd/AwdCompetitionChallenge.java)
3. [`AwdPlusCompetitionChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/stage/awdp/AwdPlusCompetitionChallenge.java)
4. [`IswCompetitionChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/stage/isw/IswCompetitionChallenge.java)
5. [`TheoryCompetitionPaper`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/stage/theory/TheoryCompetitionPaper.java)

### 2.3 业务层与接口层协同
1. **赛题挂载 (`AdminCtfMountServiceImpl`)**：
   - 支持在挂载请求 `CtfChallengeMountRequestDTO` 中指定目标 `stageId`；
   - 校验 `existsByCompetitionIdAndStageIdAndChallengeId`，允许同一道题目在不同赛段（如初赛与决赛）独立挂载并配置不同的分值与衰减策略。
2. **选手端赛题获取与实例启动 (`CtfStageServiceImpl`)**：
   - 接口支持传递 `stageId` 筛选特定赛段赛题；
   - 启动动态容器时自动获取对应赛题所属的 `stageId` 并传递给 `CtfInstanceService` 生成专属 Flag。
3. **视图模型与映射器 (`CtfCompetitionChallengeVO` / `CompetitionMapper`)**：
   - 扩展 `stageId` 与 `stageName` 字段，方便前端多 Tab 或阶段看板按赛段聚合展示。

---

## 3. 验证结果

1. 执行 `mvn clean compile -DskipTests`：全平台 12 个模块全部构建成功（12/12 SUCCESS）；
2. 执行 `mvn test`：所有单元测试执行完毕，0 失败 0 错误（BUILD SUCCESS）。
