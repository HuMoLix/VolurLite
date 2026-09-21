# 参赛队伍管理端编辑、批量审核/删除功能落地与详情组件抽离记录

## 1. 变更背景与需求分析

### 1.1 业务诉求
1. **组件架构解耦与复用分析**：
   - 深入分析 [`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 中查看队伍详情的模态框是否直接复用了选手端的 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue)。
   - **分析结论**：`TeamFormModal.vue` 是选手/队长视角的队伍创建与信息修改组件（包含多步骤表单、邀请码生成等）；而 `TeamAuditTab.vue` 中原本是一段多达 350+ 行的只读审核模态框和近 400 行专属样式，内嵌在主视图中导致文件臃肿（原文件超过 1700 行），未作独立抽象。
   - **方案决策**：按照规范将其完整抽离为管理端专用的独立高复用组件，放置在 `@views/competition/components/admin/AdminTeamDetailModal.vue`（对应实际路径 `src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue`）。
2. **管理员修改队伍全量信息功能（含成员与指定队长）**：
   - 管理员在审核或赛事筹备过程中，需要具备修改违规或错误队伍名称、所属单位、简介以及参赛队员信息（姓名、性别、学号/工号、手机、邮箱、身份证、学院/部门、班级/职位）、指导老师/领队信息、甚至重新指定队长的能力。
   - 后端需提供安全的更新接口，包含队伍名称在同一比赛范围内的唯一性冲突校验，以及成员数据、新队长的安全流转持久化。
   - 前端交互规范化：彻底移除 `TeamAuditTab.vue` 列表中的平铺「编辑」按钮，统一在详情模态框中参考 `TeamFormModal.vue` 提供「查看模式」与「编辑模式」一键无感切换，降低视图分散度。
3. **队伍批量操作功能**：
   - 面对大规模赛事成百上千队伍的报名，管理员需要能够多选批量通过资质审核、批量驳回（附带统一驳回说明）以及批量安全删除。
   - 后端需提供事务内高内聚的批量审核与批量删除接口，并继承已有的作答提交防破坏保护机制与级联清理。
4. **细节微调与缺陷修复**：
   - 选项卡标题规范：将竞赛编辑页的「队伍审核」选项卡统一调整为「队伍管理」（中英文国际化语言包同步对齐为 `队伍管理` / `Team Management`）；
   - 单位类型显示修复：修复 `AdminTeamDetailModal` 中 `SCHOOL` 被错误映射为“高校/科研机构”的缺陷，100% 严格还原选手报名时填写的“学校”；
   - 文本基线对齐修复：修复 `AdminTeamDetailModal` 中「队伍简介:」标签与后面文本在垂直基线不对齐的问题，改用 `align-items: baseline; line-height: 1.5;` 保证基线贴合。

---

## 2. 后端核心架构改造（Platform: volur-competition）

### 2.1 领域模型（DTO）标准化建设
根据 [`Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/Rules.md) 领域模型规范，统一存放在 `domain.dto` 包下：
1. **[`AdminTeamMemberDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/AdminTeamMemberDTO.java)**：
   - 成员数据载荷：`id`, `realName`, `gender`, `role`（CAPTAIN/MEMBER/LEADER）, `phone`, `email`, `idCard`, `employeeNumber`, `department`, `position`。
2. **[`AdminTeamUpdateDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/AdminTeamUpdateDTO.java)**：
   - 字段包括 `teamName`（必填，2~32 字符）、`description`（最多 512 字符）、`organization`（最多 128 字符）、`organizationType`，以及 `@Valid List<AdminTeamMemberDTO> members` 与 `@Valid List<AdminTeamMemberDTO> leaders`；
   - 统一添加 `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`，严禁 `@Builder`。
3. **[`BatchTeamAuditDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/BatchTeamAuditDTO.java)**：
   - 字段包括 `List<Long> teamIds`（`@NotEmpty`）、`TeamStatus status`（`@NotNull`）、`String auditReason`（驳回时必填/选填）；
4. **[`BatchTeamDeleteDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/BatchTeamDeleteDTO.java)**：
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
   - 校验队伍存在性，检查比赛内队伍重名（排除自身）；
   - 支持更新名称、简介、所属机构与机构类型；
   - **全量队员更新与队长重分配**：
     - 当 `members` 传入时，逐项对比 `existingMembers`；
     - 识别指定的队长角色 `MemberRole.CAPTAIN`，若有队长变更，自动将新队长绑至 `team.setCaptain(...)`，并确保全队有且仅有 1 位队长；
     - 自动同步更新队员各项实名登记字段（身份证、学工号、手机、院系班级等）；
     - 自动同步更新指导老师/领队信息（`leaders`）；
     - 自动清理已被删除的队员关系。
2. **`batchAuditTeams(Long competitionId, BatchTeamAuditDTO request)`**：
   - 在单个 `@Transactional` 事务内循环调用 `auditTeam`，复用单队审核逻辑、状态机约束以及全员站内信异步事件通知（`SendNotificationEvent`）。
3. **`batchDeleteTeams(Long competitionId, List<Long> teamIds)`**：
   - 批量级联安全删除，逐一校验每支队伍是否存在作答提交历史（CTF / 理论题 / AWD+ 提交保护），并在确认无题解数据后安全清理邀请记录、聊天记录与成员关系。

### 2.4 控制器层 REST 接口声明（Controller）
在 [`AdminCompetitionTeamController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionTeamController.java) 中发布接口：
- `PUT /api/v1/admin/competitions/{competitionId}/teams/{teamId}`：管理员修改队伍及成员信息；
- `POST /api/v1/admin/competitions/{competitionId}/teams/batch-audit`：管理员批量审核队伍；
- `POST /api/v1/admin/competitions/{competitionId}/teams/batch-delete`：管理员批量删除队伍。

---

## 3. 前端工程重构与实现（View: Vue 3 + Naive UI + volurEx）

### 3.1 独立抽离 Admin 详情模态框组件（双模式）
[`src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue)：
1. **查看模式（View Mode）**：
   - 基础信息展示卡片：头像、战队名称、所属单位、单位类型、队长、报名时间；
   - 队伍简介：修复 `.team-desc-line` 垂直基线对齐，字体和排版协调规范；
   - 参赛成员与指导老师只读表格；
   - 底部按钮矩阵：关闭、驳回报名、通过资质审核、以及「编辑队伍」切换按钮。
2. **编辑模式（Edit Mode）**：
   - 队伍基本信息卡片：队伍名称、所属单位、单位类型（学校、企业单位等）、队伍简介；
   - **可编辑名册表格**：
     - 支持实时修改成员姓名、性别、学号/工号（随单位类型动态自适应）、手机、邮箱、身份证、学院/部门、班级/职位；
     - **支持重新指定队长**：非队长成员操作列提供「设为队长」动作，点击后立即无感转移队长标识；
     - 支持添加队员（`+ 添加队员`，受限于人数上限）与删除非队长成员；
     - 指导老师/领队编辑与增删行；
   - 底部操作栏：取消编辑、保存修改。

### 3.2 `TeamAuditTab.vue` 主选项卡视图重构
在 [`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 中：
1. **删除列表中的平铺「编辑」按钮**：避免操作列冗余拥挤，所有编辑统一收敛在详情模态框中；
2. **多选列与批量操作栏（Bulk Actions Bar）**：
   - 首列增加选择框 `{ type: 'selection', fixed: 'left', width: 48 }`；
   - 勾选队伍后在顶部唤出批量操作栏，支持批量通过、批量驳回（统一原因）、批量删除（强二次确认）；
3. **选项卡更名**：
   - 在国际化文件 `competitions.json` 中统一将 `tab_teams` 更名为「队伍管理」（en: `Team Management`）。

---

## 4. 验证与测试结果

1. **后端单元测试**：
   - 在 [`AdminTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/AdminTeamServiceImplTest.java) 中补充全套测试用例：
     - `testUpdateTeam_success`
     - `testUpdateTeam_withMembersAndCaptainChange`（覆盖成员信息修改与队长重新指定）
     - `testUpdateTeam_duplicateName_throwsBusinessException`
     - `testBatchAuditTeams`
     - `testBatchDeleteTeams`
   - 执行 `mvn test -pl volur-competition`：**34 个测试用例全部通过（0 failures, 0 errors, 0 skipped）**。
2. **前端编译与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：**0 错误 0 警告通过编译与打包（✓ built in 6.03s）**。

---

## 5. 影响范围

- **后端**：`volur-competition` 模块新增成员/领队编辑及批量操作逻辑，全面保障数据完整性；
- **前端**：`TeamAuditTab.vue`、`AdminTeamDetailModal.vue` 及多语言国际化配置。
