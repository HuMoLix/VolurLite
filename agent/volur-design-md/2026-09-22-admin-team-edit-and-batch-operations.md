# 参赛队伍管理端编辑、批量审核/删除功能落地与详情组件抽离记录

## 1. 变更背景与需求分析

### 1.1 业务诉求
1. **组件架构解耦与复用分析**：
   - 深入分析 [`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 中查看队伍详情的模态框是否直接复用了选手端的 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue)。
   - **分析结论**：`TeamFormModal.vue` 是选手/队长视角的队伍创建与信息修改组件（包含多步骤表单、邀请码生成等）；而 `TeamAuditTab.vue` 中原本是一段多达 350+ 行的只读审核模态框和近 400 行专属样式，内嵌在主视图中导致文件臃肿（原文件超过 1700 行），未作独立抽象。
   - **方案决策**：按照规范将其完整抽离为管理端专用的独立高复用组件，放置在 `@views/competition/components/admin/AdminTeamDetailModal.vue`（对应实际路径 `src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue`）。
2. **管理员修改队伍信息功能**：
   - 管理员在审核或赛事筹备过程中，需要具备修改违规或错误队伍名称、所属单位、简介等基本信息的能力。
   - 后端需提供安全的更新接口，包含队伍名称在同一比赛范围内的唯一性冲突校验。
   - 前端同时支持：在表格操作列快速点击「编辑」弹窗修改；在队伍详情模态框内直接悬浮点击铅笔图标进行行内快捷改名。
3. **队伍批量操作功能**：
   - 面对大规模赛事成百上千队伍的报名，管理员需要能够多选批量通过资质审核、批量驳回（附带统一驳回说明）以及批量安全删除。
   - 后端需提供事务内高内聚的批量审核与批量删除接口，并继承已有的作答提交防破坏保护机制与级联清理。

---

## 2. 后端核心架构改造（Platform: volur-competition）

### 2.1 领域模型（DTO）标准化建设
根据 [`Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/Rules.md) 领域模型规范，统一存放在 `domain.dto` 包下：
1. **[`AdminTeamUpdateDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/AdminTeamUpdateDTO.java)**：
   - 字段包括 `teamName`（必填，2~32 字符）、`description`（最多 200 字符）、`organization`（最多 64 字符）、`organizationType`；
   - 统一添加 `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`，严禁 `@Builder`。
2. **[`BatchTeamAuditDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/BatchTeamAuditDTO.java)**：
   - 字段包括 `List<Long> teamIds`（`@NotEmpty`）、`TeamStatus status`（`@NotNull`）、`String auditReason`（驳回时必填/选填）；
3. **[`BatchTeamDeleteDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/BatchTeamDeleteDTO.java)**：
   - 字段包括 `List<Long> teamIds`（`@NotEmpty`）。

### 2.2 数据访问层扩展（Repository）
- **[`CompetitionTeamRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionTeamRepository.java)**：
  - 新增同比赛下排除自身的重名校验：
    ```java
    boolean existsByCompetitionIdAndTeamNameAndIdNot(Long competitionId, String teamName, Long id);
    ```

### 2.3 业务服务层扩展与安全保障（Service）
在 [`AdminTeamService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/AdminTeamService.java) 与 [`AdminTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java) 中实现：
1. **`updateTeam(Long teamId, AdminTeamUpdateDTO request)`**：
   - 校验队伍存在性；
   - 若修改了队伍名称，调用 `existsByCompetitionIdAndTeamNameAndIdNot` 进行比赛内重名拦截；
   - 灵活支持更新名称、简介、所属机构与机构类型，并在更新后记录审计日志。
2. **`batchAuditTeams(Long competitionId, BatchTeamAuditDTO request)`**：
   - 在单个 `@Transactional` 事务内循环调用 `auditTeam`，复用单队审核逻辑、状态机约束以及全员站内信异步事件通知（`SendNotificationEvent`）。
3. **`batchDeleteTeams(Long competitionId, List<Long> teamIds)`**：
   - 批量级联安全删除，逐一校验每支队伍是否存在作答提交历史（CTF / 理论题 / AWD+ 提交保护），并在确认无题解数据后安全清理邀请记录、聊天记录与成员关系。

### 2.4 控制器层 REST 接口新增（Controller）
在 [`AdminCompetitionTeamController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionTeamController.java) 中发布符合版本化规范的接口：
- `PUT /api/v1/admin/competitions/{competitionId}/teams/{teamId}`：管理员修改队伍信息；
- `POST /api/v1/admin/competitions/{competitionId}/teams/batch-audit`：管理员批量审核队伍；
- `POST /api/v1/admin/competitions/{competitionId}/teams/batch-delete`：管理员批量删除队伍。

---

## 3. 前端工程重构与实现（View: Vue 3 + Naive UI + volurEx）

### 3.1 独立抽离 Admin 详情模态框组件
新建 [`src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue)：
1. **视觉与交互规范遵循**：
   - 严格遵循 `GEMINI.md` 与 `view-Rules.md`：主要操作按钮保持 `#000000` / 白字，描述文本为 `#444444`，深色模式适配 Cyber Slate 沉浸阶梯；
   - 包含参赛成员列表、指导老师名册、驳回历史警示条等；
   - 内置管理员快捷修改队伍名称功能（悬浮铅笔图标，行内无缝切换输入框与保存/取消按钮）。
2. **单向数据流与标准 Emits**：
   - 接收 `:comp-id`、`:team-id`、`v-model:show` 等属性；
   - 支持 `approved`, `reject`, `updated`, `audit-success`, `update-success` 标准事件。

### 3.2 `TeamAuditTab.vue` 主视图瘦身与功能强化
在 [`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 中：
1. **代码精简**：移除此前侵入视图的 350+ 行详情 HTML 和近 500 行嵌套 CSS，整体代码量由 1711 行骤降至 690 行，结构清晰且极大提升可维护性；
2. **多选列与批量操作栏（Bulk Actions Bar）**：
   - 在表格首列加入 `{ type: 'selection', fixed: 'left', width: 48 }`；
   - 通过 `VolurTableCard` 的 `#table-header` 插槽优雅挂载批量操作栏；
   - 当 `checkedRowKeys.length > 0` 时展示已选计数、批量通过、批量驳回（弹出统一输入原因模态框）及批量删除（带强二次确认弹窗）；
3. **快捷行内操作**：
   - 操作列提供「通过」、「驳回」、「编辑」、「详情」、「删除」等完整动作矩阵；
   - 点击「编辑」唤起管理员专属的队伍全量基本信息修改表单。

### 3.3 TypeScript 类型与 API 函数 1:1 对齐
- 在 [`src/volur-lite-view/src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts) 中定义：
  - `AdminTeamUpdateDTO`、`BatchTeamAuditDTO`、`BatchTeamDeleteDTO`；
- 在 [`src/volur-lite-view/src/api/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/competition/index.ts) 中导出：
  - `updateAdminTeamApi`、`batchAuditAdminTeamsApi`、`batchDeleteAdminTeamsApi`。

---

## 4. 验证与测试结果

1. **后端单元测试**：
   - 在 [`AdminTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/AdminTeamServiceImplTest.java) 中补齐并验证：
     - `testUpdateTeam_success`
     - `testUpdateTeam_duplicateName_throwsBusinessException`
     - `testBatchAuditTeams`
     - `testBatchDeleteTeams`
   - 执行 `mvn test -pl volur-competition`：**33 个测试用例全部通过（0 failures, 0 errors, 0 skipped）**。
2. **前端编译与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：**无任何类型错误或警告，打包成功（✓ built in 6.34s）**。

---

## 5. 影响范围

- **后端**：`volur-competition` 模块新增队伍修改与批量处理相关接口，原有单队审核与删除逻辑不受影响且被批量接口完全复用。
- **前端**：`TeamAuditTab.vue` 视图与 `@views/competition/components/admin/AdminTeamDetailModal.vue`，彻底消除大文件样板代码堆积。
