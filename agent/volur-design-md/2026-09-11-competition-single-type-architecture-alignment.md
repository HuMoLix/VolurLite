# 竞赛一赛一制架构重构与前后端全栈对齐审计说明

## 1. 变更背景与需求说明
在过去的架构中，单个竞赛通过 `stageConfigs`（赛段配置）支持一场赛事内混合多个比赛模式（如同一场比赛中同时包含理论赛段、CTF 赛段、AWD 赛段等），并在题目挂载和计分板中引入了 `stage_id` 的二层嵌套模型。
这种设计带来了如下问题：
1. **概念冗余与模型复杂**：对于绝大多数网络安全竞赛，一场赛事即严格代表一种竞技模式（要么是纯 CTF 解题赛，要么是纯理论赛，要么是纯 AWD 攻防对抗）。赛段（Stage）与竞赛（Competition）的二层嵌套导致数据库关系混乱、查询联表冗余、管理端配置负担沉重。
2. **前后端接口及语义割裂**：前端 API 路径、入参实体和后端 Controller 未能严格 1:1 对齐（如前端调用不存在的 `/competitions/{id}/teams/my`，而实际后端为 `/competitions/{id}/teams/me`；创建战队使用了非标准的 `/teams` 路径而非 `/teams/signup`；天梯榜未映射根路径等）。
3. **管理端缺失**：前端管理端的竞赛管理仅为占位组件，缺少一赛一制赛事的创建、编辑、删选、封榜控制。

基于此，本次任务全面取消"一赛多制/多赛段"机制，全面采用**一赛一制（Single Competition Mode）**，并对后端 `volur-competition`、`volur-challenge` 及前端 `volur-lite-view` 进行系统化架构重构与 1:1 严格对齐。

---

## 2. 核心架构改动点

### 2.1 后端领域模型重构 (Backend Domain Model Refactoring)
1. **新增 `CompetitionType` 枚举** (`com.humolix.volur.competition.domain.enums.CompetitionType`)：
   - 包含：`CTF`(1)、`THEORY`(2)、`AWD`(3)、`AWD_PLUS`(4)、`ISW`(5)。
   - 实现 `EnumEntity`，提供 `@JsonValue` 描述。
2. **重构 `Competition` 实体与 DTO/VO**：
   - 在 `Competition` 实体中添加 `type` 字段（默认 `CompetitionType.CTF`），并移除 `List<CompetitionStageConfig> stageConfigs` 关系。
   - `CompetitionCreateRequestDTO`：增加 `@NotNull CompetitionType type`，彻底移除 `stageConfigs`。
   - `CompetitionUpdateRequestDTO`：增加 `CompetitionType type`，移除 `stageConfigs`。
   - `CompetitionSummaryVO`、`CompetitionDetailVO`、`AdminCompetitionListVO`、`AdminCompetitionDetailVO`、`CompetitionAboutVO`：全面增补 `type: CompetitionType`，移除 `stages` 列表。
3. **题目/靶卷挂载实体与表结构瘦身**：
   - `CtfCompetitionChallenge`：移除 `stage_id` 外键与关联，赛题直接绑定 `competition_id` 与 `challenge_id`。
   - `AwdCompetitionChallenge`、`AwdPlusCompetitionChallenge`、`IswCompetitionChallenge`、`TheoryCompetitionPaper`：全面清理废弃的 `stage_id` 映射。
4. **清理多阶段遗留类文件**：
   - 物理删除 `CompetitionStageConfig.java`、`CompetitionStageConfigDTO.java`、`CompetitionStageConfigRepository.java`、`CompetitionStageVO.java`、`CompetitionStage.java`。

### 2.2 注解切面与业务逻辑重构 (Annotations, Aspect & Services)
1. **`@RequiredCompetitionStageRunning` 注解重构**：
   - 修改属性由 `CompetitionStage` 改为 `CompetitionType type()` 与 `CompetitionType value()`。
   - `CompetitionValidationAspect` 切面移除对 `stageConfigRepository` 的依赖，直接校验 `competition.getType() == requiredType` 以及赛事运行时间。
2. **各赛制 Service 实现**：
   - `CtfStageServiceImpl`：标注 `@RequiredCompetitionStageRunning(type = CompetitionType.CTF)`；赛题查询直接基于 `findByCompetitionIdAndIsVisibleTrueOrderBySortOrderAsc`，容器实例启动传递 `0L` 兼容标识。
   - `AwdStageServiceImpl`：标注 `@RequiredCompetitionStageRunning(type = CompetitionType.AWD)`。
   - `AwdPlusStageServiceImpl`：标注 `@RequiredCompetitionStageRunning(type = CompetitionType.AWD_PLUS)`。
   - `IswStageServiceImpl`：标注 `@RequiredCompetitionStageRunning(type = CompetitionType.ISW)`。
   - `TheoryStageServiceImpl`：标注 `@RequiredCompetitionStageRunning(type = CompetitionType.THEORY)`。
3. **挂载管理 `AdminCtfMountServiceImpl`**：
   - 移除 `stageConfigRepository`，挂载前强制校验 `competition.getType() == CompetitionType.CTF`，赛题直接挂载于本场竞赛。
4. **天梯排行榜 `CompetitionScoreboardController` & `CompetitionScoreboardServiceImpl`**：
   - 控制器支持 `@GetMapping({"", "/composite"})`，同时保留 `/ctf` 专项分榜。
   - 计分板根据当前赛事的单一 `CompetitionType` 精确汇总战队得分（CTF 题目解题、理论考试答题、AWD 攻防得分）。

---

### 2.3 前端领域对齐与视图重构 (Frontend Alignment & Views)
1. **类型定义对齐 (`src/types/competition/index.ts`)**：
   - 定义 `CompetitionType = 'CTF' | 'THEORY' | 'AWD' | 'AWD_PLUS' | 'ISW'`。
   - 对齐 `CompetitionStatus`、`TeamStatus`、`MemberRole`。
   - 补齐 `CompetitionConfigDTO/VO`、`CompetitionFieldSchemaDTO/VO`、`CompetitionSummaryVO`、`CompetitionDetailVO`、`AdminCompetitionListVO`、`AdminCompetitionDetailVO`、`CtfCompetitionChallengeVO`、`CtfCompetitionChallengeDetailVO`、`CompositeScoreboardVO`、`CtfStageScoreboardVO`。
2. **API 请求层 100% 1:1 对齐 (`src/api/competition/index.ts`)**：
   - 公开端：`getCompetitionListApi`、`getCompetitionByIdApi`、`getCompetitionBySlugApi`、`getCompetitionAboutApi`。
   - 天梯榜：`getCompetitionScoreboardApi`、`getCtfStageScoreboardApi`。
   - 战队端：`listCompetitionTeamsApi`、`signupTeamApi` (`/teams/signup`)、`getMyTeamApi` (`/teams/me`)、`updateMyTeamApi` (`/teams/me`)、`acceptInvitationApi`、`rejectInvitationApi`、`cancelInvitationApi`。
   - CTF 阶段：`listCtfChallengesApi`、`getCtfChallengeDetailApi`、`submitCtfFlagApi`、`startCtfInstanceApi`、`stopCtfInstanceApi`、`extendCtfInstanceApi`。
   - 管理端：`getAdminCompetitionListApi`、`getAdminCompetitionByIdApi`、`createCompetitionApi`、`updateCompetitionApi`、`deleteCompetitionApi`、`setFreezeScoreboardApi`、`listAdminTeamsApi`、`auditAdminTeamApi`、`mountCtfChallengeApi`、`batchMountCtfChallengesApi`、`unmountCtfChallengeApi`。
3. **公开端竞赛大厅 (`src/views/competition/CompetitionListView.vue`)**：
   - 增加按赛制模式过滤的 Tabs（全部赛制、CTF 解题赛、理论知识赛、AWD 攻防对抗、AWD+ 综合攻防、ISW 靶场实战）。
   - 比赛卡片增加醒目赛制类型徽章（Tag）与状态徽章。
   - 遵循视觉设计准则：主要操作按钮采用亮色 `#000000` / `#ffffff`，副文本与描述文字使用 `#444444`。
4. **管理端竞赛管理 (`src/views/admin/competition/AdminCompetitionView.vue`)**：
   - 实现完整的赛事管理表格视图（ID、标题、Slug、赛制模式、状态、起止时间窗口、队伍/人数、操作列）。
   - 实现"创建竞赛 (一赛一制)"与"编辑竞赛"弹窗模态框，内置基础信息与赛制规则配置表单。
   - 实现一键快速封榜/解封榜单、删除赛事二次确认弹窗。
5. **国际化多语言补齐 (`locales/zh_CN` & `locales/en_US`)**：
   - 补全 `competition.json` 与 `admin/competitions.json` 赛制类型、状态、表单和操作相关的所有中英文翻译文本。

---

## 3. 验证与构建结果
1. **后端验证 (`volur-lite-platform`)**：
   - `mvn compile -pl volur-competition`：0 错误编译通过。
   - `mvn test-compile`：所有 12 个模块编译成功。
   - `mvn test`：包含 `DynamicFlagGeneratorTest`、`CompetitionControllerTest` 等所有自动化单元/集成测试执行完毕，测试结果 **Tests run: 7, Failures: 0, Errors: 0, Skipped: 0**，Reactor Summary 全为 SUCCESS。
2. **前端验证 (`volur-lite-view`)**：
   - 执行 `vue-tsc --noEmit && vite build`，TypeScript 类型推断与 Vite 打包通过，0 类型错误与语法告警，打包成功。

---

## 4. 影响范围与审计结论
- **影响范围**：`volur-competition` 模块、`volur-challenge` 挂载引用、`volur-lite-view` 竞赛与管理端页面。
- **兼容性说明**：已废弃所有历史多赛段（`stageConfigs`）持久化与传输模型；前后端数据模型统一为单赛制模式，前后端通信路径与数据结构实现 100% 严密对齐，架构高度清爽健壮。
