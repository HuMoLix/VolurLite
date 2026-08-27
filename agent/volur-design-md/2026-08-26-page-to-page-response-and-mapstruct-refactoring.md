# 分页模型标准化 (Page -> PageResponse) 与 MapStruct 架构重构审计文档

## 1. 变更背景与动因

在 Volur Lite 架构规范演进中，针对以下两项核心规范进行了全量代码库扫描与统一重构：
1. **分页模型与响应标准化 (`Rules.md` 1.8)**：严禁在 Controller 接口或 Service 层对外直接暴露 Spring Data 原生 `Page<T>` 或 `ApiResponse<Page<T>>`。强制统一使用 `com.humolix.volur.framework.common.api.PageResponse<T>` 轻量级不可变 Record 模型，避免向前端泄漏 Spring Data 底层无用元数据，并保持响应格式高度一致。
2. **对象映射与模型转换规范 (`Rules.md` 1.6)**：严禁在 Service / Controller 中手写 `toVO` / `toEntity` / `toDTO` / `buildVO` 等样板代码。必须统一采用 MapStruct 声明 Mapper 接口并由 Spring 容器依赖注入。

---

## 2. 变更详情与改动点

### 2.1 自动化扫描与修复脚本
- 编写脚本 [`agent/volur-design-scripts/replace_page_with_page_response.py`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/replace_page_with_page_response.py) 与 [`agent/volur-design-scripts/scan_manual_mappers.py`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/scan_manual_mappers.py)，严格遵循 `Script-rules.md` 零冗余、极简输出约束。

### 2.2 Controller & Service 分页模型统一替换 (`ApiResponse<PageResponse<T>>`)
对以下 10 个 Controller 及其对应的 Service / ServiceImpl 进行了全链路重构，将返回值统一为 `PageResponse<T>`：
1. **排行榜模块 (`volur-scoreboard`)**：
   - [`RankController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/controller/v1/RankController.java)
   - [`RankService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/service/RankService.java)
   - [`RankServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/service/impl/RankServiceImpl.java)
2. **用户管理模块 (`volur-user`)**：
   - [`AdminUserController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/admin/AdminUserController.java)
   - [`AdminUserService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/AdminUserService.java)
   - [`AdminUserServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java)
3. **通知消息模块 (`volur-notification`)**：
   - [`NotificationController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/NotificationController.java)
   - [`NotificationService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/service/NotificationService.java)
   - [`NotificationServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/service/impl/NotificationServiceImpl.java)
4. **竞赛管理模块 (`volur-competition`)**：
   - [`AdminCompetitionController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionController.java) / [`AdminCompetitionService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/AdminCompetitionService.java) / [`AdminCompetitionServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminCompetitionServiceImpl.java)
   - [`AdminCompetitionTeamController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionTeamController.java) / [`AdminTeamService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/AdminTeamService.java) / [`AdminTeamServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java)
5. **题目管理模块 (`volur-challenge`)**：
   - AWD: [`AdminAwdChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/awd/AdminAwdChallengeController.java) / [`AdminAwdChallengeService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/awd/AdminAwdChallengeService.java) / [`AdminAwdChallengeServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/awd/impl/AdminAwdChallengeServiceImpl.java)
   - AWD+: [`AdminAwdPlusChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/awdp/AdminAwdPlusChallengeController.java) / [`AdminAwdPlusChallengeService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/awdp/AdminAwdPlusChallengeService.java) / [`AdminAwdPlusChallengeServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/awdp/impl/AdminAwdPlusChallengeServiceImpl.java)
   - CTF: [`AdminCtfChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/ctf/AdminCtfChallengeController.java) / [`AdminCtfChallengeService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/ctf/AdminCtfChallengeService.java) / [`AdminCtfChallengeServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/ctf/impl/AdminCtfChallengeServiceImpl.java)
   - ISW: [`AdminIswChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/isw/AdminIswChallengeController.java) / [`AdminIswChallengeService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/isw/AdminIswChallengeService.java) / [`AdminIswChallengeServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/isw/impl/AdminIswChallengeServiceImpl.java)
   - Theory: [`AdminTheoryChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/stage/theory/AdminTheoryChallengeController.java) / [`AdminTheoryChallengeService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/theory/AdminTheoryChallengeService.java) / [`AdminTheoryChallengeServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/stage/theory/impl/AdminTheoryChallengeServiceImpl.java)

### 2.3 MapStruct 映射器补全与模型包收敛
1. **排行榜模块规范收敛 (`volur-scoreboard`)**：
   - 将原平铺的 `vo/` 迁移至规范的 [`com.humolix.volur.scoreboard.domain.vo`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/domain/vo) 包下（`RankResponseVO`, `UserRankResponseVO`）。
   - 新增 MapStruct 映射器接口 [`RankMapper`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/mapper/RankMapper.java)。
   - 在 [`RankServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/service/impl/RankServiceImpl.java) 中彻底移除手写私有方法 `toUserRankVO(User user)`，通过构造注入 `RankMapper` 执行对象转换。
2. **通知模块广播映射优化 (`volur-notification`)**：
   - 在 [`NotificationMapper`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/mapper/NotificationMapper.java) 中新增 `toBroadcastVO(BroadcastMessageDTO dto)` 映射方法。
   - 在 [`RealtimePushServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/service/impl/RealtimePushServiceImpl.java) 中移除手写 `buildBroadcastVO` 方法，改为注入 `NotificationMapper`。

---

## 3. 验证与影响范围

1. **合规性验证**：
   - 运行自动化检查脚本，所有 Controller 及 Service 层中原生 `Page<T>` 暴露违规项全部清零（`TOTAL_FOUND: 0` / `ALL_CONTROLLERS_COMPLIANT`）。
   - 运行手写映射检查脚本，所有违规私有映射方法均已清理。
2. **全工程构建验证**：
   - 执行 `mvn clean test-compile -DskipTests`，全平台 12 个 Maven 模块（包括 MapStruct 编译期 APT 代码生成）全部顺利通过（`BUILD SUCCESS`）。
