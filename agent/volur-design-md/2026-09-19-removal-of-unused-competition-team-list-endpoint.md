# 废弃并移除未使用的前台参赛队伍列表接口

## 1. 变更背景与原因分析

### 1.1 背景说明
在 [`CompetitionTeamController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) 中，历史版本保留了前台参赛队伍列表接口 `GET /api/v1/competitions/{competitionId}/teams` 及其底层业务实现 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 中的 `listTeams(competitionId, userId)` 方法。

### 1.2 移除原因与设计审计
1. **前端无任何视图调用**：
   - 经全面审计，前端页面与组件中调用队伍相关信息均走专属接口：
     * 选手自身队伍信息走 `GET /api/v1/competitions/{id}/teams/me`；
     * 管理后台队伍管理与资质审核走 `GET /api/v1/admin/competitions/{id}/teams`；
     * 竞技场排行榜天梯走 `GET /api/v1/competitions/{id}/scoreboard`；
   - 前端此前仅在 `src/api/competition/index.ts` 中声明了 `listCompetitionTeamsApi`，所有 UI 组件均未进行调用，属于冗余且闲置的死接口。
2. **消除了不必要的权限校验与隐式依赖复杂度**：
   - 该接口在底层服务中存在 `checkIsAdmin()` 逻辑，从 `SecurityContextHolder` 隐式获取上下文，并进行复杂度较高的三级动态脱敏处理；
   - 移除该接口后，前后端职责划分更加清晰，杜绝了前台公开端与管理端队伍查询职责重叠的隐患。

---

## 2. 核心改动点

### 2.1 后端改动
1. **控制器层 (`CompetitionTeamController.java`)**：
   - 移除 `listTeams` 端点声明（`GET /api/v1/competitions/{competitionId}/teams`）；
   - 移除未使用的 `CompetitionTeamVO` 引用。
2. **服务接口层 (`CompetitionTeamService.java`)**：
   - 移除 `listTeams(Long competitionId, Long currentUserId)` 及其默认重载 `listTeams(Long competitionId)`。
3. **服务实现层 (`CompetitionTeamServiceImpl.java`)**：
   - 移除 `listTeams` 两个重载方法的实现；
   - 移除专用于该接口的私有辅助方法：`checkIsAdmin()`、`desensitizeKeyInfo()`、`hidePrivateInfo()`；
   - 清理未使用的 Spring Security 相关类（`Authentication`, `SecurityContextHolder`）及 `CompetitionTeamVO` 导包。
4. **单元测试升级与调整**：
   - [`CompetitionTeamControllerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/controller/CompetitionTeamControllerTest.java)：将原本针对已移除 `listTeams` 的认证放行与 401 拦截测试迁移为针对当前仍在使用的 `GET /api/v1/competitions/{compId}/teams/me` 接口，完整保留对框架层 `@CurrentUserId` 的解析与拦截行为验证；
   - [`CompetitionTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/CompetitionTeamServiceImplTest.java)：清理掉针对 `listTeams` 的 Mock 打桩及 3 个脱敏断言用例，避免 Mockito 的 Strict Stubbing 产生无用打桩报错。

### 2.2 前端改动
1. **API 定义层 (`src/volur-lite-view/src/api/competition/index.ts`)**：
   - 移除导出的 `listCompetitionTeamsApi` 请求方法；
   - 移除未使用的 `CompetitionTeamVO` 类型导入。

---

## 3. 验证与影响范围

1. **后端单元测试与编译**：
   - 在 `src/volur-lite-platform` 目录下运行 `mvn test -pl volur-competition`：
   - 结果：**24 个测试用例全部通过（`BUILD SUCCESS`，Failures: 0, Errors: 0）**。
2. **前端类型校验与构建打包**：
   - 在 `src/volur-lite-view` 目录下执行 `npm run build`（包含 `vue-tsc` 类型检查与 Vite 打包）：
   - 结果：**构建完全成功，无任何 TypeScript 报错与未解析依赖（`built in 5.83s`）**。
3. **影响范围评估**：
   - 移除仅涉及未被调用的冗余接口，对现有选手端组队、报名、管理端队伍审核等核心业务流程无任何副作用与回归风险。
