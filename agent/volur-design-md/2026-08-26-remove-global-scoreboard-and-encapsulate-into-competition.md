# 移除全站排行榜并内敛至竞赛计分板审计文档

## 1. 变更背景与架构决策

在 Volur Lite 定位为轻量化竞赛平台的背景下，系统聚焦于比赛维度的竞技与评估，不再维护全局站内长期天梯排行榜。全站排行榜（`volur-scoreboard`）功能与代码属于冗余结构。
本次变更将全站排行榜模块彻底废弃并移除，排行榜与计分板能力完全收敛至 [`volur-competition`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition) 竞赛核心模块中。

---

## 2. 变更详情与改动点

### 2.1 移除全站排行榜模块 (`volur-scoreboard`)
- 删除了 `src/volur-lite-platform/volur-scoreboard` 目录及以下全部文件：
  - `RankController.java` (`/api/v1/rank`)
  - `RankService.java` / `RankServiceImpl.java`
  - `RankMapper.java`
  - `RankResponseVO.java` / `UserRankResponseVO.java`
  - `pom.xml`

### 2.2 构建与依赖编排清理
1. **聚合 POM (`src/volur-lite-platform/pom.xml`)**：
   - 从 `<modules>` 中移除 `<module>volur-scoreboard</module>`。
   - 从 `<dependencyManagement>` 中移除 `volur-scoreboard` 依赖声明。
2. **服务端启动模块 (`src/volur-lite-platform/volur-server/pom.xml`)**：
   - 移除对 `volur-scoreboard` 的依赖。

### 2.3 竞赛排行榜能力核验与保留
核验确认 [`volur-competition`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition) 模块已具备完备的比赛维度排行榜支持：
- 控制器：[`CompetitionScoreboardController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionScoreboardController.java) (`/api/v1/competitions/{competitionId}/scoreboard`)
  - `/composite`：综合多阶段加权总榜与冻榜支持；
  - `/ctf`：CTF 专项阶段排行榜。
- 服务实现：[`CompetitionScoreboardServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionScoreboardServiceImpl.java)

---

## 3. 构建与验证

- **代码无残留检查**：确认代码库中已无任何引用 `com.humolix.volur.scoreboard` 或 `RankController` 的残留代码。
- **Maven 全量编译验证**：执行 `mvn clean test-compile -DskipTests`，全平台 11 个模块全部构建成功（`BUILD SUCCESS`）。
