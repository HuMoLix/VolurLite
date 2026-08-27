# Volur Lite 竞赛队伍控制器 RESTful 路由规范化与领域职责重构设计与施工档案

- **执行日期**：2026-08-25
- **施工模块**：`volur-competition`
- **文档类型**：RESTful API 契约与领域职责边界重构归档

---

## 1. 重构背景与动因

在既有的 `volur-competition` 模块中，[`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) 存在以下三个典型的架构设计与 RESTful 规范缺陷：

1. **类级别路由粒度过粗，导致子资源 URI 语义严重断层**：
   - 原类级别注解为 `@RequestMapping("/api/v1/competitions")`，各个方法重复拼接 `/{competitionId}/...`。
   - 邀请相关接口原写为 `@PostMapping("/{competitionId}/{teamId}/invite/accept")`，实际映射路径为 `/api/v1/competitions/{competitionId}/{teamId}/invite/accept`。在 RESTful 语义中，两个 ID 连续出现，中间**完全丢失了 `teams` 资源层级标识**，破坏了资源的从属表达。
   - 同模块的 [`AdminCompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionTeamController.java)、[`CompetitionScoreboardController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionScoreboardController.java) 以及各类 StageController 均已采用带父级 ID 的类级别嵌套路由（如 `/api/v1/competitions/{competitionId}/...`），原前台队伍控制器与其风格严重脱节。
2. **资源领域与单一职责原则 (SRP) 违背**：
   - 竞赛介绍页面接口 `getCompetitionAbout`（返回 [`CompetitionAboutVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionAboutVO.java)）承载的是竞赛主实体属性（名称、规则、报名时间窗口等），与队伍领域模型无关，却被错放在 [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) 及 [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) 中。
3. **单复数混用与动宾 URI 不规范**：
   - `/team/signup`、`/team/me`、`/{competitionId}/teams` 在同一控制器中单复数（`team` 与 `teams`）混杂。

---

## 2. 重构设计方案与对比

### 2.1 接口路由与归属变更对照表

| 业务操作 | 原 URI (Base: `/api/v1/competitions`) | 重构后 URI (Base: `/api/v1/competitions/{competitionId}/teams`) | 归属 Controller | 归属 Service | 规范收益 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **获取竞赛介绍/关于** | `GET /{competitionId}/about` | `GET /api/v1/competitions/{id}/about` (`@GetMapping("/{id}/about")`) | [`CompetitionController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java) | [`CompetitionService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionService.java) | 职责回归竞赛主资源 |
| **获取参赛队伍列表** | `GET /{competitionId}/teams` | `GET .../teams` (`@GetMapping`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | 标准 RESTful 集合查询 |
| **创建队伍/报名参赛** | `POST /{competitionId}/team/signup` | `POST .../teams/signup` (`@PostMapping("/signup")`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | 统一复数命名空间 |
| **获取我的队伍信息** | `GET /{competitionId}/team/me` | `GET .../teams/me` (`@GetMapping("/me")`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | 消除 `team` / `teams` 混用 |
| **修改我的队伍信息** | `PUT /{competitionId}/team` | `PUT .../teams/me` (`@PutMapping("/me")`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | 语义清晰聚焦 |
| **接受队伍邀请** | `POST /{competitionId}/{teamId}/invite/accept` | `POST .../teams/{teamId}/invite/accept` (`@PostMapping("/{teamId}/invite/accept")`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | **补齐丢失的 `teams/` 层级** |
| **拒绝队伍邀请** | `POST /{competitionId}/{teamId}/invite/reject` | `POST .../teams/{teamId}/invite/reject` (`@PostMapping("/{teamId}/invite/reject")`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | **补齐丢失的 `teams/` 层级** |
| **取消成员邀请** | `DELETE /{competitionId}/{teamId}/invite/cancel` | `DELETE .../teams/{teamId}/invite/cancel` (`@DeleteMapping("/{teamId}/invite/cancel")`) | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | [`CompetitionTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) | **补齐丢失的 `teams/` 层级** |

---

## 3. 具体修改清单

1. **[`CompetitionController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java)**：
   - 增加 `@GetMapping("/{id}/about")` 接口，用于获取竞赛关于信息。
2. **[`CompetitionService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionService.java) & [`CompetitionServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java)**：
   - 声明并实现 `CompetitionAboutVO getCompetitionAbout(Long id)` 方法，将逻辑统一在 `Competition` 主领域服务中维护。
3. **[`CompetitionTeamController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java)**：
   - 类级别修改为 `@RequestMapping("/api/v1/competitions/{competitionId}/teams")`；
   - 移除 `getCompetitionAbout` 接口及无用引用；
   - 方法路径扁平化重构（`listTeams` -> `@GetMapping`，`signupTeam` -> `@PostMapping("/signup")`，`getMyTeam` -> `@GetMapping("/me")`，`updateTeam` -> `@PutMapping("/me")`，邀请相关方法 -> `@PostMapping("/{teamId}/invite/accept")` 等）。
4. **[`CompetitionTeamService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java) & [`CompetitionTeamServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)**：
   - 移除 `getCompetitionAbout` 方法声明与实现，使服务层高度聚焦队伍、成员与邀请状态机。

---

## 4. 构建与验证结果

执行 Maven 全量编译测试：
```bash
mvn clean test-compile
```
输出结果：
- 12 个模块全部编译通过：**BUILD SUCCESS**
- 零编译错误，零依赖缺失。
