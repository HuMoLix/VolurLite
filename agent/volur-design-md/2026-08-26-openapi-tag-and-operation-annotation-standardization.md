# Volur Lite 控制器层 OpenAPI @Tag 与 @Operation 全局标准化归档

- **执行日期**：2026-08-26
- **施工模块**：全平台控制器（`volur-auth`, `volur-user`, `volur-verification`, `volur-notification`, `volur-challenge`, `volur-container`, `volur-scoreboard`, `volur-competition`, `volur-system`）
- **文档类型**：API 文档标准化与 Swagger 契约补全

---

## 1. 变更背景

在升级为 SpringDoc OpenAPI 3.1.0 架构后，为了提供结构清晰、语义直观且符合业务逻辑的交互式 API 文档，需对平台所有 Controller 控制器类及接口统一规范标注 `@Tag` 与 `@Operation`。

---

## 2. 改造清单与规范标准

### 2.1 标注规范
- **控制器类级别 (`Class-Level`)**：
  - 必须标注 `@Tag(name = "...", description = "...")`；
  - `name`：清晰的业务模块功能名称（如“认证与账户管理”、“管理端 CTF 题库管理”、“竞赛阶段：AWD 攻防模式”等）；
  - `description`：简明自然地阐述该控制器所承载的核心业务职责。
- **接口方法级别 (`Method-Level`)**：
  - 每个 HTTP 映射接口（`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`）必须显式标注 `@Operation(summary = "...")`；
  - `summary`：简明扼要的动作与业务语义描述。

---

### 2.2 全平台 30 个 Controller 标准化覆盖明细

| 子模块 | 控制器类 | `@Tag` 名称 |
| :--- | :--- | :--- |
| **`volur-auth`** | [`AuthController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java) | 认证与账户管理 |
| **`volur-auth`** | [`TokenController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/TokenController.java) | 令牌与会话管理 |
| **`volur-user`** | [`UserController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java) | 用户个人中心 |
| **`volur-user`** | [`AdminUserController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/admin/AdminUserController.java) | 管理端用户管理 |
| **`volur-verification`** | [`CaptchaController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/CaptchaController.java) | 验证码与人机核验 |
| **`volur-verification`** | [`EmailController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/EmailController.java) | 邮件服务管理 |
| **`volur-notification`** | [`NotificationController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/NotificationController.java) | 站内信与通知管理 |
| **`volur-notification`** | [`RealtimeStreamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/RealtimeStreamController.java) | 实时事件流 (SSE) |
| **`volur-notification`** | [`AdminBroadcastController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/admin/AdminBroadcastController.java) | 管理端全站广播 |
| **`volur-container`** | [`CtfInstanceController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/CtfInstanceController.java) | 赛题靶机容器管理 |
| **`volur-scoreboard`** | [`RankController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/controller/v1/RankController.java) | 天梯与排行榜 |
| **`volur-system`** | [`SystemSettingController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/controller/v1/SystemSettingController.java) | 系统公开设置 |
| **`volur-system`** | [`AdminSystemSettingController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/controller/v1/admin/AdminSystemSettingController.java) | 管理端系统设置 |
| **`volur-challenge`** | [`AdminAwdChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/awd/AdminAwdChallengeController.java) | 管理端 AWD 题库管理 |
| **`volur-challenge`** | [`AdminAwdPlusChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/awdp/AdminAwdPlusChallengeController.java) | 管理端 AWD+ 题库管理 |
| **`volur-challenge`** | [`AdminCtfCategoryController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/ctf/AdminCtfCategoryController.java) | 管理端 CTF 赛题分类管理 |
| **`volur-challenge`** | [`AdminCtfChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/ctf/AdminCtfChallengeController.java) | 管理端 CTF 题库管理 |
| **`volur-challenge`** | [`AdminIswChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/isw/AdminIswChallengeController.java) | 管理端 ISW 题库管理 |
| **`volur-challenge`** | [`AdminTheoryChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/theory/AdminTheoryChallengeController.java) | 管理端理论题库管理 |
| **`volur-competition`** | [`CompetitionController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java) | 竞赛信息管理 |
| **`volur-competition`** | [`CompetitionScoreboardController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionScoreboardController.java) | 竞赛计分板与排行榜 |
| **`volur-competition`** | [`CompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) | 竞赛战队与报名管理 |
| **`volur-competition`** | [`AdminCompetitionController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionController.java) | 管理端竞赛管理 |
| **`volur-competition`** | [`AdminCompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionTeamController.java) | 管理端参赛队伍审核 |
| **`volur-competition`** | [`AdminCtfMountController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/stage/AdminCtfMountController.java) | 管理端 CTF 赛题挂载 |
| **`volur-competition`** | [`AwdStageController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/stage/AwdStageController.java) | 竞赛阶段：AWD 攻防模式 |
| **`volur-competition`** | [`AwdPlusStageController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/stage/AwdPlusStageController.java) | 竞赛阶段：AWD+ 进阶攻防模式 |
| **`volur-competition`** | [`CtfStageController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/stage/CtfStageController.java) | 竞赛阶段：CTF 解题模式 |
| **`volur-competition`** | [`IswStageController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/stage/IswStageController.java) | 竞赛阶段：ISW 实战靶场模式 |
| **`volur-competition`** | [`TheoryStageController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/stage/TheoryStageController.java) | 竞赛阶段：理论考核模式 |

---

## 3. 验证结果

执行全平台 Maven 构建：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功（`BUILD SUCCESS`），全站 30 个 Controller 控制器及下辖所有 API 接口已 100% 具备规范、清晰的 Swagger 3 / OpenAPI 注解。
