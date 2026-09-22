# 管理端战队角色选择交互重构、唯一队长强校验、统计卡片计数漂移修复及状态筛选标签精简

## 1. 变更背景与需求说明

在管理端比赛后台的“队伍管理”模块（`TeamAuditTab.vue`）与队伍详情/编辑模态框（`AdminTeamDetailModal.vue`）中，用户在实际使用和测试过程中发现了若干体验与逻辑缺陷：

1. **队员角色与队长切换交互缺陷**：
   - 编辑队伍成员时，角色列原先为只读 Tag 标识，后方操作列提供“设为队长”按钮，交互繁琐且不符合表单编辑的直觉。
   - 需求变更为：将角色列直接切换为下拉选择框（`n-select`，提供“队长”与“队员”选项），并在保存时前后端强制校验全队**是否且仅有唯一一名队长**。
   - “编辑队伍”按钮由于此前使用了 `secondary type="primary"`，在暗色模式下背景呈现为带透明度的暗绿色，不符合平台视觉主题规范（平台暗色模式主要按钮应为高对比反色纯白 `#ffffff`）。

2. **顶部统计卡片计数受筛选与分页污染问题**：
   - 原前端在 `fetchTeams()` 内部直接使用当前表格的 `pagination.total` 赋值给 `statData.total`，并通过当前页分页数组（仅 10 条数据）做 `list.filter` 计算待审核、已通过和已驳回数量。
   - 导致严重 Bug：当管理员在状态下拉框中选择“待审核”时，“报名总队伍”直接变成了待审核队伍的数量；翻到第二页时，“待审核队伍”数量又变成了第二页局部的待审核数量，严重失真。

3. **状态筛选下拉框技术枚举后缀冗余**：
   - 状态筛选下拉选项中文案带有 `(AUDIT_DRAFT)`、`(AUDIT_PASSED)` 等技术枚举后缀，界面不够纯粹专业，需精简为纯正中文。

---

## 2. 核心改动点

### 2.1 后端改动 (Java & Spring Boot)

1. **新增统计概览 VO (`AdminTeamStatsVO`)**：
   - 文件：`com.humolix.volur.competition.domain.vo.AdminTeamStatsVO`
   - 包含 `total`（总队伍数）、`pending`（待审核数）、`approved`（已通过数）、`rejected`（已驳回数）四个 Long 字段。
   - 严格遵循 VO 规范，添加 `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`。

2. **新增管理端队伍统计接口与服务实现**：
   - 控制器：`AdminCompetitionTeamController.getTeamStats(@PathVariable Long competitionId)`，映射 `GET /api/v1/admin/competitions/{competitionId}/teams/stats`。
   - 服务层：`AdminTeamServiceImpl.getTeamStats(competitionId)` 利用 `CompetitionTeamRepository` 内置的 `countByCompetitionId` 与 `countByCompetitionIdAndStatus` 快速聚合统计，执行毫秒级数据库查询。

3. **编辑保存时唯一队长强校验 (`AdminTeamServiceImpl.updateTeam`)**：
   - 对传入的 `request.getMembers()` 进行 Stream 流统计 `m.getRole() == MemberRole.CAPTAIN`。
   - 若 `captainCount != 1`，立即抛出 `BusinessException(HttpStatus.BAD_REQUEST, "参赛队伍必须且只能包含 1 名队长，当前指定了 X 名队长")`，彻底杜绝无队长或多队长提交。
   - 正确更新队长角色与 `team.setCaptain()` 归属。

4. **补齐单元测试用例 (`AdminTeamServiceImplTest`)**：
   - 新增 `testUpdateTeam_whenCaptainCountNotOne_throwsBusinessException`，覆盖 0 队长与多队长场景下的拦截验证。
   - 新增 `testGetTeamStats_success`，验证统计服务聚合数据的正确性。全部 12 个单测 100% 成功通过。

---

### 2.2 前端改动 (Vue 3 / TypeScript / Naive UI)

1. **类型定义与 API 对齐 (`types/competition` & `api/competition`)**：
   - 在 `src/types/competition/index.ts` 中导出 `AdminTeamStatsVO`。
   - 在 `src/api/competition/index.ts` 中导出 `getAdminTeamStatsApi(competitionId)`，路由严格 1:1 对齐后端 `GET /admin/competitions/${competitionId}/teams/stats`。

2. **队长选择交互与按钮样式重构 (`AdminTeamDetailModal.vue`)**：
   - 成员编辑表格中将角色列改为 `<n-select v-model:value="row.role" size="small" :options="memberRoleOptions" style="width: 85px;" />`。
   - 操作列移除“设为队长”按钮，列宽精简至 70px，仅保留行删除按钮，并移除冗余的 `setAsCaptain` 函数。
   - 在保存逻辑 `handleSaveAll()` 中前置校验 `captainCount !== 1`，给出精准友好的 Toast 提示并阻断提交。
   - “编辑队伍”按钮移除 `secondary type="primary"`，加上 `class="theme-primary-btn"`，确保在暗色模式下呈现为纯白背景 `#ffffff`，在亮色模式下为纯黑 `#000000`，严格遵从平台黑白天平规范。

3. **统计计数与筛选解耦 (`TeamAuditTab.vue`)**：
   - 下拉选项精简：`statusFilterOptions` 移除 `(AUDIT_DRAFT)` 等英文括号枚举，保持“全部状态”、“待审核”、“已通过”、“已驳回”纯净中文。
   - 计数逻辑解耦：抽出独立的 `fetchStats()` 函数调用 `getAdminTeamStatsApi`；在 `fetchTeams()` 中彻底移除覆盖 `statData` 的代码。
   - 在首次加载（`onMounted`）、手动刷新（`handleManualRefresh`）、模态框修改/审核成功（`handleDetailModalSuccess`）以及单项/批量通过、驳回、删除动作后，协同触发 `fetchStats()`，保证无论表格如何翻页或过滤，顶部统计数据始终为全局真实统计。

---

## 3. 影响范围与验证

- **影响文件**：
  - 后端：
    - `AdminTeamStatsVO.java` (新增)
    - `AdminCompetitionTeamController.java`
    - `AdminTeamService.java`
    - `AdminTeamServiceImpl.java`
    - `AdminTeamServiceImplTest.java`
  - 前端：
    - `src/types/competition/index.ts`
    - `src/api/competition/index.ts`
    - `src/views/competition/components/admin/AdminTeamDetailModal.vue`
    - `src/views/admin/competition/tabs/TeamAuditTab.vue`
- **验证结果**：
  - 后端执行 `mvn test -pl volur-competition`：36 个单测全部 PASS。
  - 前端执行 `npm run build`（含 vue-tsc 类型检查与 Vite 生产打包）：通过无任何报错。
